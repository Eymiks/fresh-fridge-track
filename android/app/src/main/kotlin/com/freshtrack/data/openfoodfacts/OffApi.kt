package com.freshtrack.data.openfoodfacts

import com.freshtrack.domain.catalog.matchCategory
import com.freshtrack.domain.catalog.matchSubcategory
import com.freshtrack.domain.model.Product
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private const val OFF_BASE = "https://world.openfoodfacts.org/api/v0/product"

@Serializable
private data class OffResponse(val status: Int = 0, val product: JsonObject? = null)

data class OffResult(val product: Product, val availableImages: List<String>)

@Singleton
class OffApi @Inject constructor(private val httpClient: HttpClient) {

    suspend fun fetchByBarcode(barcode: String): Product? =
        fetchByBarcodeRaw(barcode)?.product

    suspend fun fetchByBarcodeRaw(barcode: String): OffResult? = runCatching {
        val response: OffResponse = httpClient.get("$OFF_BASE/$barcode.json").body()
        if (response.status != 1 || response.product == null) return null
        val p = response.product

        fun str(key: String) = p[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

        val name = str("product_name_fr") ?: str("product_name") ?: str("product_name_en") ?: return null
        val brand = str("brands")
        val availableImages = listOfNotNull(
            str("image_front_url"),
            str("image_url"),
            str("image_ingredients_url"),
            str("image_nutrition_url"),
            str("image_packaging_url"),
            str("image_small_url")
        ).distinct().take(5)
        val imageUrl: String? = null  // sera choisi par l'utilisateur via le dialog
        val nutriScore = str("nutriscore_grade") ?: str("nutrition_grade_fr")
        val novaGroup = str("nova_group")?.toIntOrNull()
        val ecoScore = str("ecoscore_grade")
        val allergens = str("allergens")
        val ingredients = str("ingredients_text_fr") ?: str("ingredients_text")
        val offCategory = str("pnns_groups_1") ?: str("categories")
        val quantity = str("quantity")

        val nutriments = p["nutriments"]?.jsonObject
        fun num(key: String) = nutriments?.get(key)?.jsonPrimitive?.doubleOrNull
        val nutrition = buildJsonObject {
            num("energy-kcal_100g")?.let { put("energy_kcal", it) }
            num("proteins_100g")?.let { put("proteins", it) }
            num("carbohydrates_100g")?.let { put("carbohydrates", it) }
            num("fat_100g")?.let { put("fat", it) }
            num("saturated-fat_100g")?.let { put("saturated_fat", it) }
            num("sugars_100g")?.let { put("sugars", it) }
            num("fiber_100g")?.let { put("fiber", it) }
            num("salt_100g")?.let { put("salt", it) }
        }

        val category = matchCategory(offCategory, name)
        val subcategory = matchSubcategory(category, offCategory, name)

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val product = Product(
            name = name, barcode = barcode, brand = brand, imageUrl = imageUrl,
            nutriScore = nutriScore, novaGroup = novaGroup, ecoScore = ecoScore,
            allergens = allergens, ingredients = ingredients,
            quantity = quantity,
            category = category, subcategory = subcategory,
            nutritionData = nutrition.takeIf { it.isNotEmpty() }?.toString(),
            expirationDate = today,
            addedAt = Clock.System.now()
        )
        OffResult(product = product, availableImages = availableImages)
    }.getOrNull()
}

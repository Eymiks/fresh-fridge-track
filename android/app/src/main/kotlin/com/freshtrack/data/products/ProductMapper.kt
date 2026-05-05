package com.freshtrack.data.products

import com.freshtrack.data.db.ProductEntity
import com.freshtrack.data.supabase.ProductInsertRow
import com.freshtrack.data.supabase.ProductRow
import com.freshtrack.data.supabase.ProductUpdateRow
import com.freshtrack.domain.model.Member
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun ProductRow.toEntity(): ProductEntity = ProductEntity(
    id = id, householdId = householdId, name = name, barcode = barcode,
    expirationDate = expirationDate, addedAt = addedAt, imageUrl = imageUrl,
    brand = brand, nutriScore = nutriScore, category = category, subcategory = subcategory,
    status = status, statusChangedAt = statusChangedAt, quantity = quantity,
    novaGroup = novaGroup, ecoScore = ecoScore, allergens = allergens,
    ingredients = ingredients, openedAt = openedAt, daysAfterOpening = daysAfterOpening,
    addedBy = addedBy, notes = notes, frozenUntil = frozenUntil, nutritionData = nutritionData
)

fun ProductEntity.toDomain(members: List<Member> = emptyList()): Product = Product(
    id = id, name = name, barcode = barcode,
    expirationDate = LocalDate.parse(expirationDate),
    addedAt = Instant.parse(addedAt),
    imageUrl = imageUrl, brand = brand, nutriScore = nutriScore,
    category = category, subcategory = subcategory,
    status = ProductStatus.entries.firstOrNull { it.name.lowercase() == status } ?: ProductStatus.ACTIVE,
    statusChangedAt = statusChangedAt?.let { Instant.parse(it) },
    quantity = quantity, novaGroup = novaGroup, ecoScore = ecoScore,
    allergens = allergens, ingredients = ingredients,
    openedAt = openedAt?.let { Instant.parse(it) },
    daysAfterOpening = daysAfterOpening,
    addedBy = addedBy,
    addedByName = members.firstOrNull { it.userId == addedBy }?.displayName,
    notes = notes,
    frozenUntil = frozenUntil?.let { LocalDate.parse(it) },
    nutritionData = nutritionData
)

fun Product.toEntity(householdId: String): ProductEntity = ProductEntity(
    id = id, householdId = householdId, name = name, barcode = barcode,
    expirationDate = expirationDate.toString(),
    addedAt = addedAt.toString(),
    imageUrl = imageUrl, brand = brand, nutriScore = nutriScore,
    category = category, subcategory = subcategory,
    status = status.name.lowercase(),
    statusChangedAt = statusChangedAt?.toString(),
    quantity = quantity, novaGroup = novaGroup, ecoScore = ecoScore,
    allergens = allergens, ingredients = ingredients,
    openedAt = openedAt?.toString(),
    daysAfterOpening = daysAfterOpening,
    addedBy = addedBy, notes = notes,
    frozenUntil = frozenUntil?.toString(),
    nutritionData = nutritionData
)

fun Product.toInsertRow(householdId: String, userId: String): ProductInsertRow = ProductInsertRow(
    householdId = householdId, name = name, barcode = barcode,
    expirationDate = expirationDate.toString(),
    addedAt = addedAt.toString(),
    imageUrl = imageUrl, brand = brand, nutriScore = nutriScore,
    category = category, subcategory = subcategory,
    status = status.name.lowercase(),
    statusChangedAt = statusChangedAt?.toString(),
    quantity = quantity, novaGroup = novaGroup, ecoScore = ecoScore,
    allergens = allergens, ingredients = ingredients,
    openedAt = openedAt?.toString(),
    daysAfterOpening = daysAfterOpening,
    addedBy = userId, notes = notes,
    frozenUntil = frozenUntil?.toString(),
    nutritionData = nutritionData
)

fun Product.toUpdateRow(): ProductUpdateRow = ProductUpdateRow(
    name = name, barcode = barcode,
    expirationDate = expirationDate.toString(),
    imageUrl = imageUrl, brand = brand, nutriScore = nutriScore,
    category = category, subcategory = subcategory,
    status = status.name.lowercase(),
    statusChangedAt = statusChangedAt?.toString(),
    quantity = quantity, novaGroup = novaGroup, ecoScore = ecoScore,
    allergens = allergens, ingredients = ingredients,
    openedAt = openedAt?.toString(),
    daysAfterOpening = daysAfterOpening,
    notes = notes,
    frozenUntil = frozenUntil?.toString(),
    nutritionData = nutritionData
)

fun Product.toUpdateJsonObject(): JsonObject = buildJsonObject {
    fun putNullableString(key: String, value: String?) {
        if (value == null) put(key, JsonNull) else put(key, value)
    }

    fun putNullableInt(key: String, value: Int?) {
        if (value == null) put(key, JsonNull) else put(key, value)
    }

    put("name", name)
    putNullableString("barcode", barcode)
    put("expiration_date", expirationDate.toString())
    putNullableString("image_url", imageUrl)
    putNullableString("brand", brand)
    putNullableString("nutri_score", nutriScore)
    putNullableString("category", category)
    putNullableString("subcategory", subcategory)
    put("status", status.name.lowercase())
    putNullableString("status_changed_at", statusChangedAt?.toString())
    putNullableString("quantity", quantity)
    putNullableInt("nova_group", novaGroup)
    putNullableString("eco_score", ecoScore)
    putNullableString("allergens", allergens)
    putNullableString("ingredients", ingredients)
    putNullableString("opened_at", openedAt?.toString())
    putNullableInt("days_after_opening", daysAfterOpening)
    putNullableString("notes", notes)
    putNullableString("frozen_until", frozenUntil?.toString())
    putNullableString("nutrition_data", nutritionData)
}

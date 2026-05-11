package com.freshtrack.data.openfoodfacts

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class OpenFoodFactsImageSize {
    DISPLAY,
    FULL
}

private val selectedImageTypes = listOf("front", "product", "packaging", "ingredients", "nutrition")
private val preferredLanguages = listOf("fr", "en")
private val displayFields = listOf(
    "image_front_url",
    "image_url",
    "image_packaging_url",
    "image_ingredients_url",
    "image_nutrition_url"
)
private val fallbackFields = listOf(
    "image_front_small_url",
    "image_small_url",
    "image_packaging_small_url",
    "image_ingredients_small_url",
    "image_nutrition_small_url",
    "image_front_thumb_url",
    "image_thumb_url",
    "image_packaging_thumb_url",
    "image_ingredients_thumb_url",
    "image_nutrition_thumb_url"
)
private val offSizedImageRegex = Regex("""\.(?:100|200|400|full)\.(jpe?g|png|webp)(\?.*)?$""", RegexOption.IGNORE_CASE)

fun openFoodFactsImageCandidates(product: JsonObject): List<String> {
    val selected = product["selected_images"]?.jsonObjectOrNull()
    return buildList {
        selected?.let { addAll(selectedUrls(it, "display")) }
        addAll(displayFields.mapNotNull { product.stringOrNull(it) })
        selected?.let {
            addAll(selectedUrls(it, "small"))
            addAll(selectedUrls(it, "thumb"))
        }
        addAll(fallbackFields.mapNotNull { product.stringOrNull(it) })
    }
        .map { normalizeOpenFoodFactsImageUrl(it, OpenFoodFactsImageSize.DISPLAY) }
        .distinct()
        .take(5)
}

fun normalizeOpenFoodFactsImageUrl(
    url: String,
    size: OpenFoodFactsImageSize = OpenFoodFactsImageSize.DISPLAY
): String {
    val cleanUrl = url.trim()
    if (cleanUrl.isBlank() || !isOpenFoodFactsImageUrl(cleanUrl)) return cleanUrl

    val target = when (size) {
        OpenFoodFactsImageSize.DISPLAY -> "400"
        OpenFoodFactsImageSize.FULL -> "full"
    }
    return cleanUrl.replace(offSizedImageRegex) { match ->
        ".$target.${match.groupValues[1]}${match.groupValues.getOrElse(2) { "" }}"
    }
}

private fun selectedUrls(selectedImages: JsonObject, sizeKey: String): List<String> =
    selectedImageTypes.flatMap { type ->
        val bySize = selectedImages[type]?.jsonObjectOrNull()?.get(sizeKey)?.jsonObjectOrNull()
            ?: return@flatMap emptyList()
        val preferred = preferredLanguages.mapNotNull { bySize.stringOrNull(it) }
        val rest = bySize.values.mapNotNull { it.contentOrNull() }
        preferred + rest
    }

private fun JsonObject.stringOrNull(key: String): String? =
    this[key]?.contentOrNull()

private fun JsonElement.contentOrNull(): String? =
    runCatching { jsonPrimitive.content.trim().takeIf { it.isNotBlank() } }.getOrNull()

private fun JsonElement.jsonObjectOrNull(): JsonObject? =
    runCatching { jsonObject }.getOrNull()

private fun isOpenFoodFactsImageUrl(url: String): Boolean =
    url.contains("images.openfoodfacts.org", ignoreCase = true) ||
        url.contains("static.openfoodfacts.org", ignoreCase = true) ||
        url.contains("world.openfoodfacts.org/images", ignoreCase = true)

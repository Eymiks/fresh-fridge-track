package com.freshtrack.domain.catalog

fun matchCategory(openFoodFactsCategory: String? = null, productName: String? = null): String {
    val text = "${openFoodFactsCategory.orEmpty()} ${productName.orEmpty()}".lowercase()
    if (text.isBlank()) return "autre"
    for (cat in PRODUCT_CATEGORIES) {
        if (cat.key == "all" || cat.key == "autre") continue
        if (cat.keywords.any { text.contains(it) }) return cat.key
    }
    return "autre"
}

fun matchSubcategory(
    categoryKey: String? = null,
    openFoodFactsCategory: String? = null,
    productName: String? = null
): String? {
    if (categoryKey == null) return null
    val cat = PRODUCT_CATEGORIES.find { it.key == categoryKey } ?: return null
    if (cat.subcategories.isEmpty()) return null
    val text = "${openFoodFactsCategory.orEmpty()} ${productName.orEmpty()}".lowercase()
    if (text.isBlank()) return null
    for (sub in cat.subcategories) {
        if (sub.keywords.any { text.contains(it) }) return sub.label
    }
    return null
}

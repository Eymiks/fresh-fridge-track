package com.freshtrack.data.openfoodfacts

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class OffImageUrlsTest {

    @Test
    fun prefersSelectedFrontDisplayBeforeDirectFields() {
        val product = Json.parseToJsonElement(
            """
            {
              "selected_images": {
                "front": {
                  "display": {
                    "fr": "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_fr.12.200.jpg"
                  }
                }
              },
              "image_url": "https://images.openfoodfacts.org/images/products/301/762/042/2003/1.400.jpg"
            }
            """.trimIndent()
        ).jsonObject

        assertEquals(
            "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_fr.12.400.jpg",
            openFoodFactsImageCandidates(product).first()
        )
    }

    @Test
    fun avoidsThumbnailWhenBetterImageExists() {
        val product = Json.parseToJsonElement(
            """
            {
              "image_front_thumb_url": "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_fr.12.100.jpg",
              "image_front_url": "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_fr.12.full.jpg"
            }
            """.trimIndent()
        ).jsonObject

        assertEquals(
            listOf("https://images.openfoodfacts.org/images/products/301/762/042/2003/front_fr.12.400.jpg"),
            openFoodFactsImageCandidates(product)
        )
    }

    @Test
    fun fallsBackToThumbnailWhenItIsTheOnlyImage() {
        val product = Json.parseToJsonElement(
            """
            {
              "image_thumb_url": "https://images.openfoodfacts.org/images/products/301/762/042/2003/1.100.jpg"
            }
            """.trimIndent()
        ).jsonObject

        assertEquals(
            listOf("https://images.openfoodfacts.org/images/products/301/762/042/2003/1.400.jpg"),
            openFoodFactsImageCandidates(product)
        )
    }

    @Test
    fun upgradesDisplayAndFullscreenSizesConservatively() {
        val url = "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_fr.12.200.jpg"

        assertEquals(
            "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_fr.12.400.jpg",
            normalizeOpenFoodFactsImageUrl(url, OpenFoodFactsImageSize.DISPLAY)
        )
        assertEquals(
            "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_fr.12.full.jpg",
            normalizeOpenFoodFactsImageUrl(url, OpenFoodFactsImageSize.FULL)
        )
    }
}

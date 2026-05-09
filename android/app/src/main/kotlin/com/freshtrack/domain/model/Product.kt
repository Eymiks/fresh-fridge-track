package com.freshtrack.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

enum class ProductStatus { ACTIVE, OPENED, CONSUMED, THROWN }

enum class ExpirationStatus { FRESH, SOON, EXPIRED }

@Immutable
data class Product(
    val id: String = "",
    val name: String,
    val barcode: String? = null,
    val expirationDate: LocalDate,
    val addedAt: Instant = Clock.System.now(),
    val imageUrl: String? = null,
    val brand: String? = null,
    val nutriScore: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val status: ProductStatus = ProductStatus.ACTIVE,
    val statusChangedAt: Instant? = null,
    val quantity: String? = null,
    val novaGroup: Int? = null,
    val ecoScore: String? = null,
    val allergens: String? = null,
    val ingredients: String? = null,
    val openedAt: Instant? = null,
    val daysAfterOpening: Int? = null,
    val addedBy: String? = null,
    val addedByName: String? = null,
    val notes: String? = null,
    val frozenUntil: LocalDate? = null,
    val nutritionData: String? = null
)

fun Product.getEffectiveExpirationDate(): LocalDate {
    frozenUntil?.let { return it }
    if (status == ProductStatus.OPENED && openedAt != null && daysAfterOpening != null) {
        val tz = TimeZone.currentSystemDefault()
        val openedDate = openedAt.toLocalDateTime(tz).date
        val openedExpiry = openedDate.plus(daysAfterOpening, DateTimeUnit.DAY)
        return if (openedExpiry < expirationDate) openedExpiry else expirationDate
    }
    return expirationDate
}

fun Product.getExpirationStatus(): ExpirationStatus {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val effective = getEffectiveExpirationDate()
    val daysLeft = today.daysUntil(effective)
    return when {
        daysLeft < 0 -> ExpirationStatus.EXPIRED
        daysLeft <= 3 -> ExpirationStatus.SOON
        else -> ExpirationStatus.FRESH
    }
}

fun Product.getDaysUntilExpiration(): Int {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return today.daysUntil(getEffectiveExpirationDate())
}

fun Product.isActive() = status == ProductStatus.ACTIVE || status == ProductStatus.OPENED

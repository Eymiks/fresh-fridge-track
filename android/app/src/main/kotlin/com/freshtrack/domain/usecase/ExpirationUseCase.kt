package com.freshtrack.domain.usecase

import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getEffectiveExpirationDate
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.domain.model.isActive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpirationUseCase @Inject constructor() {

    fun filterActive(products: List<Product>) = products.filter { it.isActive() }

    fun filterExpired(products: List<Product>) = filterActive(products)
        .filter { it.getExpirationStatus() == ExpirationStatus.EXPIRED }

    fun filterSoon(products: List<Product>) = filterActive(products)
        .filter { it.getExpirationStatus() == ExpirationStatus.SOON }

    fun filterFresh(products: List<Product>) = filterActive(products)
        .filter { it.getExpirationStatus() == ExpirationStatus.FRESH }

    fun sortByExpiration(products: List<Product>) = products.sortedBy { it.getEffectiveExpirationDate() }

    fun getUrgent(products: List<Product>, max: Int = 5) =
        sortByExpiration(filterActive(products)).take(max)

    fun filterByCategory(products: List<Product>, category: String) =
        if (category == "all") products else products.filter { it.category == category }

    fun filterByStatus(products: List<Product>, status: ProductStatus) =
        products.filter { it.status == status }

    fun search(products: List<Product>, query: String): List<Product> {
        if (query.isBlank()) return products
        val q = query.lowercase()
        return products.filter {
            it.name.lowercase().contains(q) ||
            it.brand?.lowercase()?.contains(q) == true ||
            it.barcode?.contains(q) == true
        }
    }
}

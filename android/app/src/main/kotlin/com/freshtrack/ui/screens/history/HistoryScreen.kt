package com.freshtrack.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus

@Composable
fun HistoryScreen(
    onProductClick: (String) -> Unit,
    vm: HistoryViewModel = hiltViewModel()
) {
    val products by vm.historyProducts.collectAsState()
    val opened = products.filter { it.status == ProductStatus.OPENED }
    val consumed = products.filter { it.status == ProductStatus.CONSUMED }
    val thrown = products.filter { it.status == ProductStatus.THROWN }

    Surface(Modifier.fillMaxSize()) {
        LazyColumn {
            item {
                Text("Historique", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            }
            if (products.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp)) {
                        Text("Aucun produit dans l'historique", fontWeight = FontWeight.Medium)
                        Text(
                            "Les produits ouverts, consommés ou jetés apparaîtront ici.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                historySection(
                    title = "Ouverts",
                    products = opened,
                    onProductClick = onProductClick,
                    onRestore = vm::restoreProduct
                )
                historySection(
                    title = "Consommés",
                    products = consumed,
                    onProductClick = onProductClick,
                    onRestore = vm::restoreProduct
                )
                historySection(
                    title = "Jetés",
                    products = thrown,
                    onProductClick = onProductClick,
                    onRestore = vm::restoreProduct
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(
    product: Product,
    onClick: () -> Unit,
    onRestore: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(product.name, fontWeight = FontWeight.Medium)
            product.brand?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.width(8.dp))
        val (label, color) = when (product.status) {
            ProductStatus.CONSUMED -> "Consommé" to MaterialTheme.colorScheme.primary
            ProductStatus.THROWN -> "Jeté" to MaterialTheme.colorScheme.error
            ProductStatus.OPENED -> "Ouvert" to MaterialTheme.colorScheme.tertiary
            else -> "Actif" to MaterialTheme.colorScheme.onSurface
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(label, color = color, style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = onRestore) {
                Text("Actif")
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.historySection(
    title: String,
    products: List<Product>,
    onProductClick: (String) -> Unit,
    onRestore: (Product) -> Unit
) {
    if (products.isEmpty()) return
    item {
        Text(
            "$title (${products.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    items(products, key = { it.id }) { product ->
        HistoryItem(
            product = product,
            onClick = { onProductClick(product.id) },
            onRestore = { onRestore(product) }
        )
        HorizontalDivider()
    }
}

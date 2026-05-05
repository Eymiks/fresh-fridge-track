package com.freshtrack.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.freshtrack.domain.format.formatInstantDate
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus

@Composable
fun HistoryScreen(
    onProductClick: (String) -> Unit,
    vm: HistoryViewModel = hiltViewModel()
) {
    val products by vm.historyProducts.collectAsState()
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    val filtered = remember(products, filter) {
        when (filter) {
            HistoryFilter.ALL -> products
            HistoryFilter.OPENED -> products.filter { it.status == ProductStatus.OPENED }
            HistoryFilter.CONSUMED -> products.filter { it.status == ProductStatus.CONSUMED }
            HistoryFilter.THROWN -> products.filter { it.status == ProductStatus.THROWN }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Historique", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    formatHistorySubtitle(products),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryFilterChip("Tout (${products.size})", filter == HistoryFilter.ALL) { filter = HistoryFilter.ALL }
                    HistoryFilterChip("Ouverts (${products.count { it.status == ProductStatus.OPENED }})", filter == HistoryFilter.OPENED) { filter = HistoryFilter.OPENED }
                    HistoryFilterChip("Consommés (${products.count { it.status == ProductStatus.CONSUMED }})", filter == HistoryFilter.CONSUMED) { filter = HistoryFilter.CONSUMED }
                    HistoryFilterChip("Jetés (${products.count { it.status == ProductStatus.THROWN }})", filter == HistoryFilter.THROWN) { filter = HistoryFilter.THROWN }
                }
            }

            if (products.isEmpty()) {
                item {
                    EmptyHistoryState("Aucun produit dans l'historique", "Les produits ouverts, consommés ou jetés apparaîtront ici.")
                }
            } else if (filtered.isEmpty()) {
                item {
                    EmptyHistoryState("Aucun produit pour ce filtre", "Changez de filtre ou revenez à Tout.")
                }
            } else {
                items(filtered, key = { it.id }) { product ->
                    HistoryItem(
                        product = product,
                        onClick = { onProductClick(product.id) },
                        onRestore = { vm.restoreProduct(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun HistoryItem(product: Product, onClick: () -> Unit, onRestore: () -> Unit) {
    val status = statusMeta(product.status)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(status.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(status.icon, contentDescription = null, tint = status.color)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(product.brand, product.quantity).joinToString(" · ").ifBlank { "Produit du foyer" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    statusDate(product),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(status.label, status.color)
                TextButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Remettre actif")
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyHistoryState(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private enum class HistoryFilter { ALL, OPENED, CONSUMED, THROWN }

private data class StatusMeta(
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun statusMeta(status: ProductStatus): StatusMeta = when (status) {
    ProductStatus.OPENED -> StatusMeta("Ouvert", MaterialTheme.colorScheme.tertiary, Icons.Default.Restore)
    ProductStatus.CONSUMED -> StatusMeta("Consommé", MaterialTheme.colorScheme.primary, Icons.Default.Check)
    ProductStatus.THROWN -> StatusMeta("Jeté", MaterialTheme.colorScheme.error, Icons.Default.Delete)
    ProductStatus.ACTIVE -> StatusMeta("Actif", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Check)
}

private fun statusDate(product: Product): String {
    val instant = product.statusChangedAt ?: product.addedAt
    val date = formatInstantDate(instant).orEmpty()
    return when (product.status) {
        ProductStatus.OPENED -> "Ouvert le $date"
        ProductStatus.CONSUMED -> "Consommé le $date"
        ProductStatus.THROWN -> "Jeté le $date"
        ProductStatus.ACTIVE -> "Actif depuis le $date"
    }
}

private fun formatHistorySubtitle(products: List<Product>): String {
    if (products.isEmpty()) return "Aucun mouvement pour le moment"
    val opened = products.count { it.status == ProductStatus.OPENED }
    val consumed = products.count { it.status == ProductStatus.CONSUMED }
    val thrown = products.count { it.status == ProductStatus.THROWN }
    return "$opened ouvert(s) · $consumed consommé(s) · $thrown jeté(s)"
}

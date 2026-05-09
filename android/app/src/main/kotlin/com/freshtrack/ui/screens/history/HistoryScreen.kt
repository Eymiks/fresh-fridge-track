package com.freshtrack.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import com.freshtrack.ui.theme.ColorFresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.ui.components.ArchivedProductCard
import com.freshtrack.ui.components.ProductSectionHeader

@Composable
fun HistoryScreen(
    onProductClick: (String) -> Unit,
    vm: HistoryViewModel = hiltViewModel()
) {
    val products by vm.historyProducts.collectAsState()
    val error by vm.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    val openedList = remember(products) { products.filter { it.status == ProductStatus.OPENED } }
    val consumedList = remember(products) { products.filter { it.status == ProductStatus.CONSUMED } }
    val thrownList = remember(products) { products.filter { it.status == ProductStatus.THROWN } }

    LaunchedEffect(error) {
        val msg = error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.clearError()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "history_header", contentType = "header") {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Historique", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            formatHistorySubtitle(products),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item(key = "history_filters", contentType = "filters") {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryFilterChip(
                            "Tout",
                            products.size,
                            filter == HistoryFilter.ALL,
                            MaterialTheme.colorScheme.primary
                        ) { filter = HistoryFilter.ALL }
                        HistoryFilterChip(
                            "Ouverts",
                            openedList.size,
                            filter == HistoryFilter.OPENED,
                            MaterialTheme.colorScheme.primary
                        ) { filter = HistoryFilter.OPENED }
                        HistoryFilterChip(
                            "Consommés",
                            consumedList.size,
                            filter == HistoryFilter.CONSUMED,
                            ColorFresh
                        ) { filter = HistoryFilter.CONSUMED }
                        HistoryFilterChip(
                            "Jetés",
                            thrownList.size,
                            filter == HistoryFilter.THROWN,
                            MaterialTheme.colorScheme.error
                        ) { filter = HistoryFilter.THROWN }
                    }
                }

                if (products.isEmpty()) {
                    item(key = "history_empty", contentType = "empty_state") {
                        EmptyHistoryState("Aucun produit dans l'historique", "Les produits ouverts, consommés ou jetés apparaîtront ici.")
                    }
                }

                if ((filter == HistoryFilter.ALL || filter == HistoryFilter.OPENED) && openedList.isNotEmpty()) {
                    item(key = "history_opened_header", contentType = "section_header") {
                        ProductSectionHeader("Ouverts", openedList.size, Icons.Default.Inventory2, MaterialTheme.colorScheme.primary)
                    }
                    items(openedList, key = { "o_${it.id}" }, contentType = { "history_product" }) { product ->
                        val productId = product.id
                        val productClick = remember(productId) { { onProductClick(productId) } }
                        val restoreClick = remember(productId) { { vm.restoreProduct(product); Unit } }
                        ArchivedProductCard(product = product, onClick = productClick, onRestore = restoreClick)
                    }
                }

                if ((filter == HistoryFilter.ALL || filter == HistoryFilter.CONSUMED) && consumedList.isNotEmpty()) {
                    item(key = "history_consumed_header", contentType = "section_header") {
                        ProductSectionHeader("Consommés", consumedList.size, Icons.Default.Restaurant, ColorFresh)
                    }
                    items(consumedList, key = { "c_${it.id}" }, contentType = { "history_product" }) { product ->
                        val productId = product.id
                        val productClick = remember(productId) { { onProductClick(productId) } }
                        val restoreClick = remember(productId) { { vm.restoreProduct(product); Unit } }
                        ArchivedProductCard(product = product, onClick = productClick, onRestore = restoreClick)
                    }
                }

                if ((filter == HistoryFilter.ALL || filter == HistoryFilter.THROWN) && thrownList.isNotEmpty()) {
                    item(key = "history_thrown_header", contentType = "section_header") {
                        ProductSectionHeader("Jetés", thrownList.size, Icons.Default.Delete, MaterialTheme.colorScheme.error)
                    }
                    items(thrownList, key = { "t_${it.id}" }, contentType = { "history_product" }) { product ->
                        val productId = product.id
                        val productClick = remember(productId) { { onProductClick(productId) } }
                        val restoreClick = remember(productId) { { vm.restoreProduct(product); Unit } }
                        ArchivedProductCard(product = product, onClick = productClick, onRestore = restoreClick)
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) color.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = if (selected) color else MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selected) color else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class HistoryFilter { ALL, OPENED, CONSUMED, THROWN }

private fun formatHistorySubtitle(products: List<Product>): String {
    if (products.isEmpty()) return "Aucun mouvement pour le moment"
    val opened = products.count { it.status == ProductStatus.OPENED }
    val consumed = products.count { it.status == ProductStatus.CONSUMED }
    val thrown = products.count { it.status == ProductStatus.THROWN }
    return "$opened ouvert(s) · $consumed consommé(s) · $thrown jeté(s)"
}

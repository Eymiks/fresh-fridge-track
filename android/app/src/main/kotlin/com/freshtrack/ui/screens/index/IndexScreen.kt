package com.freshtrack.ui.screens.index

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.freshtrack.domain.catalog.PRODUCT_CATEGORIES
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getDaysUntilExpiration
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.ui.theme.ColorExpired
import com.freshtrack.ui.theme.ColorFresh
import com.freshtrack.ui.theme.ColorSoon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun IndexScreen(
    onProductClick: (String) -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    vm: IndexViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val groups by vm.groups.collectAsState()

    var searchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                SearchBar(
                    query = ui.searchQuery,
                    onQueryChange = { vm.setSearch(it) },
                    onSearch = { searchActive = false },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = { Text("Rechercher un produit…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (ui.searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { vm.setSearch("") }) { Icon(Icons.Default.Close, null) } }
                    } else {
                        { IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "Paramètres") } }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                ) {}

                // Category filter chips
                LazyRow(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(PRODUCT_CATEGORIES.filter { it.key != "all" }.let {
                        listOf(PRODUCT_CATEGORIES.first()) + it
                    }) { cat ->
                        FilterChip(
                            selected = ui.selectedCategory == cat.key,
                            onClick = { vm.setCategory(cat.key) },
                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!ui.isSelectionMode) {
                FloatingActionButton(onClick = onScanClick) {
                    Icon(Icons.Default.Add, "Ajouter un produit")
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = ui.isSelectionMode,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                BottomAppBar {
                    IconButton(onClick = { vm.exitSelectionMode() }) {
                        Icon(Icons.Default.Close, "Annuler")
                    }
                    Text("${ui.selectedIds.size} sélectionné(s)", modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.setStatusForSelected(ProductStatus.CONSUMED) }) {
                        Text("Consommé")
                    }
                    TextButton(onClick = { vm.setStatusForSelected(ProductStatus.THROWN) }) {
                        Text("Jeté")
                    }
                    IconButton(onClick = { vm.deleteSelected() }) {
                        Icon(Icons.Default.Delete, "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = ui.isLoading,
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            if (!ui.isLoading && groups.expired.isEmpty() && groups.soon.isEmpty() && groups.fresh.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🥗", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Votre frigo est vide", style = MaterialTheme.typography.titleMedium)
                        Text("Appuyez sur + pour ajouter un produit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (groups.expired.isNotEmpty()) {
                        stickyHeader {
                            SectionHeader("⚠ Périmés (${groups.expired.size})", ColorExpired)
                        }
                        items(groups.expired, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                isSelected = product.id in ui.selectedIds,
                                isSelectionMode = ui.isSelectionMode,
                                onClick = {
                                    if (ui.isSelectionMode) vm.toggleSelection(product.id)
                                    else onProductClick(product.id)
                                },
                                onLongClick = { vm.enterSelectionMode(product.id) }
                            )
                        }
                    }

                    if (groups.soon.isNotEmpty()) {
                        stickyHeader {
                            SectionHeader("⏰ Bientôt périmés (${groups.soon.size})", ColorSoon)
                        }
                        items(groups.soon, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                isSelected = product.id in ui.selectedIds,
                                isSelectionMode = ui.isSelectionMode,
                                onClick = {
                                    if (ui.isSelectionMode) vm.toggleSelection(product.id)
                                    else onProductClick(product.id)
                                },
                                onLongClick = { vm.enterSelectionMode(product.id) }
                            )
                        }
                    }

                    if (groups.fresh.isNotEmpty()) {
                        stickyHeader {
                            SectionHeader("✓ Frais (${groups.fresh.size})", ColorFresh)
                        }
                        items(groups.fresh, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                isSelected = product.id in ui.selectedIds,
                                isSelectionMode = ui.isSelectionMode,
                                onClick = {
                                    if (ui.isSelectionMode) vm.toggleSelection(product.id)
                                    else onProductClick(product.id)
                                },
                                onLongClick = { vm.enterSelectionMode(product.id) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCard(
    product: Product,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val status = product.getExpirationStatus()
    val daysLeft = product.getDaysUntilExpiration()
    val statusColor = when (status) {
        ExpirationStatus.EXPIRED -> ColorExpired
        ExpirationStatus.SOON -> ColorSoon
        ExpirationStatus.FRESH -> ColorFresh
    }
    val daysLabel = when {
        daysLeft < 0 -> "Périmé depuis ${-daysLeft}j"
        daysLeft == 0 -> "Expire aujourd'hui"
        daysLeft == 1 -> "Expire demain"
        else -> "Expire dans ${daysLeft}j"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = null)
                Spacer(Modifier.width(8.dp))
            }

            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (!product.brand.isNullOrBlank()) {
                    Text(product.brand, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (product.status == ProductStatus.OPENED) {
                    Text("Ouvert", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary)
                }
            }

            Text(
                text = daysLabel,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

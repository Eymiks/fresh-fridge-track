package com.freshtrack.ui.screens.index

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import com.freshtrack.ui.theme.Density
import com.freshtrack.ui.theme.LocalAppearance

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
    val totalCounts by vm.totalCounts.collectAsState()
    val products by vm.products.collectAsState()

    var searchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    var expiredCollapsed by rememberSaveable { mutableStateOf(false) }
    var soonCollapsed by rememberSaveable { mutableStateOf(false) }
    var freshCollapsed by rememberSaveable { mutableStateOf(false) }

    val headerBg = when {
        totalCounts.expired > 0 -> ColorExpired.copy(alpha = 0.07f)
        totalCounts.soon > 0 -> ColorSoon.copy(alpha = 0.07f)
        else -> Color.Transparent
    }

    val isInitialLoading = ui.isLoading && products.isEmpty()
    val isEmpty = !ui.isLoading &&
        groups.expired.isEmpty() && groups.soon.isEmpty() && groups.fresh.isEmpty()

    Scaffold(
        topBar = {
            Column(Modifier.background(headerBg)) {
                SearchBar(
                    query = ui.searchQuery,
                    onQueryChange = { vm.setSearch(it) },
                    onSearch = { searchActive = false },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = { Text("Rechercher un produit…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                IconButton(onClick = { showSortMenu = !showSortMenu }) {
                                    Icon(Icons.Default.Sort, "Trier")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Par expiration") },
                                        onClick = { vm.setSortOrder(SortOrder.EXPIRATION); showSortMenu = false },
                                        trailingIcon = if (ui.sortOrder == SortOrder.EXPIRATION) {
                                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Par nom") },
                                        onClick = { vm.setSortOrder(SortOrder.NAME); showSortMenu = false },
                                        trailingIcon = if (ui.sortOrder == SortOrder.NAME) {
                                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Par date d'ajout") },
                                        onClick = { vm.setSortOrder(SortOrder.ADDED_DATE); showSortMenu = false },
                                        trailingIcon = if (ui.sortOrder == SortOrder.ADDED_DATE) {
                                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                            if (ui.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { vm.setSearch("") }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            } else {
                                IconButton(onClick = onSettingsClick) {
                                    Icon(Icons.Default.Settings, "Paramètres")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                ) {}

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
            isRefreshing = ui.isLoading && products.isNotEmpty(),
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when {
                isInitialLoading -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(6) { ProductCardSkeleton() }
                    }
                }
                isEmpty -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.8f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🥗", style = MaterialTheme.typography.displayLarge)
                                Text(
                                    "Votre frigo est vide",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Appuyez sur + pour ajouter un produit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item("stat_cards") {
                            StatCardsRow(
                                expiredCount = totalCounts.expired,
                                soonCount = totalCounts.soon,
                                freshCount = totalCounts.fresh,
                                activeFilter = ui.statusFilter,
                                onFilterChange = { vm.setStatusFilter(it) }
                            )
                        }

                        if (totalCounts.expired > 0 && ui.statusFilter == StatusFilter.ALL) {
                            item("alert_banner") { AlertBanner(totalCounts.expired) }
                        }

                        if (groups.expired.isNotEmpty()) {
                            stickyHeader("header_expired") {
                                CollapsibleSectionHeader(
                                    title = "⚠ Périmés (${groups.expired.size})",
                                    color = ColorExpired,
                                    isCollapsed = expiredCollapsed,
                                    onToggle = { expiredCollapsed = !expiredCollapsed }
                                )
                            }
                            if (!expiredCollapsed) {
                                items(groups.expired, key = { it.id }) { product ->
                                    ProductCard(
                                        product = product,
                                        isSelected = product.id in ui.selectedIds,
                                        isSelectionMode = ui.isSelectionMode,
                                        onClick = {
                                            if (ui.isSelectionMode) vm.toggleSelection(product.id)
                                            else onProductClick(product.id)
                                        },
                                        onLongClick = { vm.enterSelectionMode(product.id) },
                                        onConsume = { vm.quickSetStatus(product.id, ProductStatus.CONSUMED) },
                                        onThrow = { vm.quickSetStatus(product.id, ProductStatus.THROWN) }
                                    )
                                }
                            }
                        }

                        if (groups.soon.isNotEmpty()) {
                            stickyHeader("header_soon") {
                                CollapsibleSectionHeader(
                                    title = "⏰ Bientôt périmés (${groups.soon.size})",
                                    color = ColorSoon,
                                    isCollapsed = soonCollapsed,
                                    onToggle = { soonCollapsed = !soonCollapsed }
                                )
                            }
                            if (!soonCollapsed) {
                                items(groups.soon, key = { it.id }) { product ->
                                    ProductCard(
                                        product = product,
                                        isSelected = product.id in ui.selectedIds,
                                        isSelectionMode = ui.isSelectionMode,
                                        onClick = {
                                            if (ui.isSelectionMode) vm.toggleSelection(product.id)
                                            else onProductClick(product.id)
                                        },
                                        onLongClick = { vm.enterSelectionMode(product.id) },
                                        onConsume = { vm.quickSetStatus(product.id, ProductStatus.CONSUMED) },
                                        onThrow = { vm.quickSetStatus(product.id, ProductStatus.THROWN) }
                                    )
                                }
                            }
                        }

                        if (groups.fresh.isNotEmpty()) {
                            stickyHeader("header_fresh") {
                                CollapsibleSectionHeader(
                                    title = "✓ Frais (${groups.fresh.size})",
                                    color = ColorFresh,
                                    isCollapsed = freshCollapsed,
                                    onToggle = { freshCollapsed = !freshCollapsed }
                                )
                            }
                            if (!freshCollapsed) {
                                items(groups.fresh, key = { it.id }) { product ->
                                    ProductCard(
                                        product = product,
                                        isSelected = product.id in ui.selectedIds,
                                        isSelectionMode = ui.isSelectionMode,
                                        onClick = {
                                            if (ui.isSelectionMode) vm.toggleSelection(product.id)
                                            else onProductClick(product.id)
                                        },
                                        onLongClick = { vm.enterSelectionMode(product.id) },
                                        onConsume = { vm.quickSetStatus(product.id, ProductStatus.CONSUMED) },
                                        onThrow = { vm.quickSetStatus(product.id, ProductStatus.THROWN) }
                                    )
                                }
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Stat cards (M1 + H5) ─────────────────────────────────────────────────────

@Composable
private fun StatCardsRow(
    expiredCount: Int,
    soonCount: Int,
    freshCount: Int,
    activeFilter: StatusFilter,
    onFilterChange: (StatusFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(expiredCount, "Périmés", ColorExpired,
            activeFilter == StatusFilter.EXPIRED, Modifier.weight(1f)) {
            onFilterChange(if (activeFilter == StatusFilter.EXPIRED) StatusFilter.ALL else StatusFilter.EXPIRED)
        }
        StatCard(soonCount, "Bientôt", ColorSoon,
            activeFilter == StatusFilter.SOON, Modifier.weight(1f)) {
            onFilterChange(if (activeFilter == StatusFilter.SOON) StatusFilter.ALL else StatusFilter.SOON)
        }
        StatCard(freshCount, "Frais", ColorFresh,
            activeFilter == StatusFilter.FRESH, Modifier.weight(1f)) {
            onFilterChange(if (activeFilter == StatusFilter.FRESH) StatusFilter.ALL else StatusFilter.FRESH)
        }
    }
}

@Composable
private fun StatCard(
    count: Int, label: String, color: Color, isActive: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick, modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) color.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isActive) BorderStroke(1.dp, color) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isActive || count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Alert banner (M5) ────────────────────────────────────────────────────────

@Composable
private fun AlertBanner(expiredCount: Int) {
    val plural = expiredCount > 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(ColorExpired.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, null, tint = ColorExpired, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "$expiredCount produit${if (plural) "s" else ""} ${if (plural) "sont" else "est"} périmé${if (plural) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = ColorExpired,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Collapsible section header (M3) ──────────────────────────────────────────

@Composable
private fun CollapsibleSectionHeader(
    title: String, color: Color, isCollapsed: Boolean, onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = if (isCollapsed) "Déplier" else "Replier",
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Skeleton loading (M2) ─────────────────────────────────────────────────────

@Composable
private fun ProductCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth(0.55f).height(14.dp).clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth(0.30f).height(10.dp).clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                )
            }
            Box(
                Modifier.width(64.dp).height(14.dp).clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            )
        }
    }
}

// ── Nutri-Score badge (M8) ───────────────────────────────────────────────────

@Composable
private fun NutriScoreBadge(score: String) {
    val (bg, fg) = when (score.uppercase()) {
        "A" -> Color(0xFF1B5E20) to Color.White
        "B" -> Color(0xFF558B2F) to Color.White
        "C" -> Color(0xFFF9A825) to Color.Black
        "D" -> Color(0xFFE65100) to Color.White
        "E" -> Color(0xFFB71C1C) to Color.White
        else -> return
    }
    Box(
        Modifier.size(20.dp).clip(MaterialTheme.shapes.extraSmall).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(score.uppercase(), color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

// ── ProductCard (H3/M7/M8/M9/M13) ───────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onConsume: (() -> Unit)? = null,
    onThrow: (() -> Unit)? = null
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

    val density = LocalAppearance.current.density
    val verticalPadding = when (density) {
        Density.COMPACT -> 2.dp
        Density.NORMAL -> 4.dp
        Density.SPACIOUS -> 8.dp
    }

    var showMenu by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onConsume?.invoke(); onConsume != null }
                SwipeToDismissBoxValue.EndToStart -> { onThrow?.invoke(); onThrow != null }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = verticalPadding),
        enableDismissFromStartToEnd = !isSelectionMode && onConsume != null,
        enableDismissFromEndToStart = !isSelectionMode && onThrow != null,
        backgroundContent = {
            val direction = dismissState.targetValue
            val (bgColor, alignment) = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> ColorFresh.copy(alpha = 0.15f) to Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> ColorExpired.copy(alpha = 0.15f) to Alignment.CenterEnd
                else -> Color.Transparent to Alignment.Center
            }
            Box(
                Modifier.fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = ColorFresh)
                        Spacer(Modifier.width(4.dp))
                        Text("Consommé", color = ColorFresh, fontWeight = FontWeight.Medium)
                    }
                    SwipeToDismissBoxValue.EndToStart -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Jeté", color = ColorExpired, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Delete, null, tint = ColorExpired)
                    }
                    else -> {}
                }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Left color strip (M7)
                Box(
                    Modifier.width(5.dp).fillMaxHeight()
                        .background(statusColor, shape = RectangleShape)
                )
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Checkbox(checked = isSelected, onCheckedChange = null)
                        Spacer(Modifier.width(4.dp))
                    }

                    if (product.imageUrl != null) {
                        // Image + frozen badge overlay (L1)
                        Box(contentAlignment = Alignment.TopEnd) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.small)
                            )
                            if (product.frozenUntil != null) {
                                Box(
                                    Modifier.size(18.dp)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(Color(0xFF0288D1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AcUnit,
                                        contentDescription = "Congelé",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                    } else if (product.frozenUntil != null) {
                        // No image but frozen — show standalone badge
                        Box(
                            Modifier.size(52.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(Color(0xFF0288D1).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AcUnit, contentDescription = null,
                                tint = Color(0xFF0288D1), modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        if (!product.brand.isNullOrBlank()) {
                            Text(
                                product.brand,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (product.status == ProductStatus.OPENED) {
                                Text(
                                    "Ouvert",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            // Nutri-Score badge (M8)
                            if (!product.nutriScore.isNullOrBlank()) {
                                NutriScoreBadge(product.nutriScore)
                            }
                        }
                        // Ajouté par (L2)
                        if (!product.addedByName.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    product.addedByName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = daysLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Context menu (M9)
                        if (!isSelectionMode) {
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, "Plus d'options", Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    if (onConsume != null) {
                                        DropdownMenuItem(
                                            text = { Text("Marquer consommé") },
                                            onClick = { showMenu = false; onConsume() },
                                            leadingIcon = {
                                                Icon(Icons.Default.Check, null,
                                                    tint = ColorFresh, modifier = Modifier.size(18.dp))
                                            }
                                        )
                                    }
                                    if (onThrow != null) {
                                        DropdownMenuItem(
                                            text = { Text("Jeter") },
                                            onClick = { showMenu = false; onThrow() },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, null,
                                                    tint = ColorExpired, modifier = Modifier.size(18.dp))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

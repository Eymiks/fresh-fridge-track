package com.freshtrack.ui.screens.index

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshtrack.domain.catalog.PRODUCT_CATEGORIES
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.isActive
import com.freshtrack.ui.components.ProductCard
import com.freshtrack.ui.components.ProductSectionHeader
import com.freshtrack.ui.theme.ColorExpired
import com.freshtrack.ui.theme.ColorFresh
import com.freshtrack.ui.theme.ColorSoon
import com.freshtrack.ui.theme.FreshTextStyles
import com.freshtrack.ui.theme.LocalAppearance

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun IndexScreen(
    onProductClick: (String) -> Unit,
    onScanClick: () -> Unit,
    onMultiScanClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditProduct: (String) -> Unit = {},
    onMenuOpen: () -> Unit = {},
    vm: IndexViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val groups by vm.groups.collectAsState()
    val totalCounts by vm.totalCounts.collectAsState()
    val products by vm.products.collectAsState()
    val reduceMotion = LocalAppearance.current.reduceMotion

    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    val onDismissSortMenu = remember { { showSortMenu = false } }
    val onDismissFilterMenu = remember { { showFilterMenu = false } }
    val onStatusFilterChange = remember(vm) { { f: StatusFilter -> vm.setStatusFilter(f) } }

    var expiredCollapsed by rememberSaveable { mutableStateOf(false) }
    var soonCollapsed by rememberSaveable { mutableStateOf(false) }
    var freshCollapsed by rememberSaveable { mutableStateOf(false) }

    val headerBg = when {
        totalCounts.expired > 0 -> ColorExpired.copy(alpha = 0.055f)
        totalCounts.soon > 0 -> ColorSoon.copy(alpha = 0.055f)
        else -> MaterialTheme.colorScheme.surface
    }

    val isInitialLoading = ui.isLoading && products.isEmpty()
    val hasNoActiveProducts = !ui.isLoading && products.none { it.isActive() }
    val hasActiveProducts = products.any { it.isActive() }
    val hasAlerts = totalCounts.expired > 0 || totalCounts.soon > 0
    val hasActiveFilter = ui.statusFilter != StatusFilter.ALL || ui.selectedCategory != "all"
    val hasNoFilteredProducts = !ui.isLoading &&
        groups.expired.isEmpty() && groups.soon.isEmpty() && groups.fresh.isEmpty()

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .background(headerBg)
                    .padding(top = 22.dp, bottom = 16.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Eco,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "FreshTrack",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        Surface(
                            onClick = onMenuOpen,
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 1.dp
                        ) {
                            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (hasAlerts) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ColorExpired)
                            )
                        }
                    }
                }

                if (hasActiveProducts) {
                    StatCardsRow(
                        expiredCount = totalCounts.expired,
                        soonCount = totalCounts.soon,
                        freshCount = totalCounts.fresh,
                        activeFilter = ui.statusFilter,
                        onFilterChange = onStatusFilterChange,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            if (!ui.isSelectionMode) {
                FabBubbleMenu(
                    expanded = fabExpanded,
                    reduceMotion = reduceMotion,
                    onToggle = { fabExpanded = !fabExpanded },
                    onSingleProduct = { fabExpanded = false; onScanClick() },
                    onMultiProduct = { fabExpanded = false; onMultiScanClick() }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = ui.isSelectionMode,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                BottomAppBar(modifier = Modifier.navigationBarsPadding()) {
                    IconButton(onClick = { vm.exitSelectionMode() }) {
                        Icon(Icons.Default.Close, "Annuler")
                    }
                    Text(
                        "${ui.selectedIds.size} sélectionné(s)",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    TextButton(onClick = { vm.setStatusForSelected(ProductStatus.CONSUMED) }) {
                        Text("Consommé", style = FreshTextStyles.ActionLabel)
                    }
                    TextButton(onClick = { vm.setStatusForSelected(ProductStatus.THROWN) }) {
                        Text("Jeté", style = FreshTextStyles.ActionLabel)
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
                        items(6, contentType = { "skeleton" }) { ProductCardSkeleton() }
                    }
                }
                hasNoActiveProducts -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (reduceMotion) {
                            EmptyFridgeState()
                        } else {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.8f)
                            ) {
                                EmptyFridgeState()
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item(key = "search_controls", contentType = "controls") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = ui.searchQuery,
                                    onValueChange = { vm.setSearch(it) },
                                    placeholder = {
                                        Text(
                                            "Rechercher un produit...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                                    trailingIcon = if (ui.searchQuery.isNotEmpty()) {
                                        { IconButton(onClick = { vm.setSearch("") }) { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) } }
                                    } else null,
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                Box {
                                    HeaderIconButton(
                                        onClick = { showSortMenu = !showSortMenu },
                                        icon = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Trier",
                                        isActive = ui.sortOrder != SortOrder.EXPIRATION
                                    )
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = onDismissSortMenu
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Par expiration", style = FreshTextStyles.MenuItem) },
                                            onClick = { vm.setSortOrder(SortOrder.EXPIRATION); showSortMenu = false },
                                            trailingIcon = if (ui.sortOrder == SortOrder.EXPIRATION) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Par nom", style = FreshTextStyles.MenuItem) },
                                            onClick = { vm.setSortOrder(SortOrder.NAME); showSortMenu = false },
                                            trailingIcon = if (ui.sortOrder == SortOrder.NAME) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Par date d'ajout", style = FreshTextStyles.MenuItem) },
                                            onClick = { vm.setSortOrder(SortOrder.ADDED_DATE); showSortMenu = false },
                                            trailingIcon = if (ui.sortOrder == SortOrder.ADDED_DATE) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                }
                                Box {
                                    HeaderIconButton(
                                        onClick = { showFilterMenu = !showFilterMenu },
                                        icon = Icons.Default.Tune,
                                        contentDescription = "Filtrer",
                                        isActive = hasActiveFilter
                                    )
                                    DropdownMenu(
                                        expanded = showFilterMenu,
                                        onDismissRequest = onDismissFilterMenu
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Tous les statuts", style = FreshTextStyles.MenuItem) },
                                            onClick = { vm.setStatusFilter(StatusFilter.ALL); showFilterMenu = false },
                                            trailingIcon = if (ui.statusFilter == StatusFilter.ALL) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Périmés", style = FreshTextStyles.MenuItem) },
                                            onClick = { vm.setStatusFilter(StatusFilter.EXPIRED); showFilterMenu = false },
                                            leadingIcon = { Icon(Icons.Default.Warning, null, tint = ColorExpired) },
                                            trailingIcon = if (ui.statusFilter == StatusFilter.EXPIRED) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Bientôt", style = FreshTextStyles.MenuItem) },
                                            onClick = { vm.setStatusFilter(StatusFilter.SOON); showFilterMenu = false },
                                            leadingIcon = { Icon(Icons.Default.Schedule, null, tint = ColorSoon) },
                                            trailingIcon = if (ui.statusFilter == StatusFilter.SOON) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Frais", style = FreshTextStyles.MenuItem) },
                                            onClick = { vm.setStatusFilter(StatusFilter.FRESH); showFilterMenu = false },
                                            leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = ColorFresh) },
                                            trailingIcon = if (ui.statusFilter == StatusFilter.FRESH) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                        HorizontalDivider()
                                        PRODUCT_CATEGORIES.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(if (cat.key == "all") "Toutes les catégories" else cat.label, style = FreshTextStyles.MenuItem) },
                                                onClick = {
                                                    vm.setCategory(cat.key)
                                                    showFilterMenu = false
                                                },
                                                trailingIcon = if (ui.selectedCategory == cat.key) {
                                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (totalCounts.expired > 0 && ui.statusFilter == StatusFilter.ALL) {
                            item(key = "alert_banner", contentType = "alert_banner") {
                                AlertBanner(
                                    expiredCount = totalCounts.expired,
                                    onClick = { vm.setStatusFilter(StatusFilter.EXPIRED) }
                                )
                            }
                        }

                        if (groups.expired.isNotEmpty()) {
                            stickyHeader(key = "header_expired", contentType = "section_header") {
                                ProductSectionHeader(
                                    title = "Périmés",
                                    count = groups.expired.size,
                                    color = ColorExpired,
                                    icon = Icons.Default.Warning,
                                    isCollapsed = expiredCollapsed,
                                    onToggle = { expiredCollapsed = !expiredCollapsed }
                                )
                            }
                            if (!expiredCollapsed) {
                                items(groups.expired, key = { it.id }, contentType = { "product" }) { product ->
                                    val productId = product.id
                                    val productClick = remember(productId, ui.isSelectionMode) {
                                        {
                                            if (ui.isSelectionMode) vm.toggleSelection(productId)
                                            else onProductClick(productId)
                                        }
                                    }
                                    val longClick = remember(productId) { { vm.enterSelectionMode(productId) } }
                                    val editClick = remember(productId) { { onEditProduct(productId) } }
                                    val consumeClick = remember(productId) {
                                        { vm.quickSetStatus(productId, ProductStatus.CONSUMED); Unit }
                                    }
                                    val throwClick = remember(productId) {
                                        { vm.quickSetStatus(productId, ProductStatus.THROWN); Unit }
                                    }
                                    ProductCard(
                                        product = product,
                                        isSelected = productId in ui.selectedIds,
                                        isSelectionMode = ui.isSelectionMode,
                                        onClick = productClick,
                                        onLongClick = longClick,
                                        onEdit = editClick,
                                        onConsume = consumeClick,
                                        onThrow = throwClick
                                    )
                                }
                            }
                        }

                        if (groups.soon.isNotEmpty()) {
                            stickyHeader(key = "header_soon", contentType = "section_header") {
                                ProductSectionHeader(
                                    title = "Bientôt périmés",
                                    count = groups.soon.size,
                                    color = ColorSoon,
                                    icon = Icons.Default.Schedule,
                                    isCollapsed = soonCollapsed,
                                    onToggle = { soonCollapsed = !soonCollapsed }
                                )
                            }
                            if (!soonCollapsed) {
                                items(groups.soon, key = { it.id }, contentType = { "product" }) { product ->
                                    val productId = product.id
                                    val productClick = remember(productId, ui.isSelectionMode) {
                                        {
                                            if (ui.isSelectionMode) vm.toggleSelection(productId)
                                            else onProductClick(productId)
                                        }
                                    }
                                    val longClick = remember(productId) { { vm.enterSelectionMode(productId) } }
                                    val editClick = remember(productId) { { onEditProduct(productId) } }
                                    val consumeClick = remember(productId) {
                                        { vm.quickSetStatus(productId, ProductStatus.CONSUMED); Unit }
                                    }
                                    val throwClick = remember(productId) {
                                        { vm.quickSetStatus(productId, ProductStatus.THROWN); Unit }
                                    }
                                    ProductCard(
                                        product = product,
                                        isSelected = productId in ui.selectedIds,
                                        isSelectionMode = ui.isSelectionMode,
                                        onClick = productClick,
                                        onLongClick = longClick,
                                        onEdit = editClick,
                                        onConsume = consumeClick,
                                        onThrow = throwClick
                                    )
                                }
                            }
                        }

                        if (groups.fresh.isNotEmpty()) {
                            stickyHeader(key = "header_fresh", contentType = "section_header") {
                                ProductSectionHeader(
                                    title = "Frais",
                                    count = groups.fresh.size,
                                    color = ColorFresh,
                                    icon = Icons.Default.CheckCircle,
                                    isCollapsed = freshCollapsed,
                                    onToggle = { freshCollapsed = !freshCollapsed }
                                )
                            }
                            if (!freshCollapsed) {
                                items(groups.fresh, key = { it.id }, contentType = { "product" }) { product ->
                                    val productId = product.id
                                    val productClick = remember(productId, ui.isSelectionMode) {
                                        {
                                            if (ui.isSelectionMode) vm.toggleSelection(productId)
                                            else onProductClick(productId)
                                        }
                                    }
                                    val longClick = remember(productId) { { vm.enterSelectionMode(productId) } }
                                    val editClick = remember(productId) { { onEditProduct(productId) } }
                                    val consumeClick = remember(productId) {
                                        { vm.quickSetStatus(productId, ProductStatus.CONSUMED); Unit }
                                    }
                                    val throwClick = remember(productId) {
                                        { vm.quickSetStatus(productId, ProductStatus.THROWN); Unit }
                                    }
                                    ProductCard(
                                        product = product,
                                        isSelected = productId in ui.selectedIds,
                                        isSelectionMode = ui.isSelectionMode,
                                        onClick = productClick,
                                        onLongClick = longClick,
                                        onEdit = editClick,
                                        onConsume = consumeClick,
                                        onThrow = throwClick
                                    )
                                }
                            }
                        }

                        if (hasNoFilteredProducts) {
                            item(key = "empty_filter", contentType = "empty_state") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        "Aucun produit trouvé",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        "Changez de filtre ou revenez à Tous.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item(key = "bottom_spacer", contentType = "spacer") { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFridgeState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Text(
            "Frigo vide !",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Ajoutez votre premier produit pour commencer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

// ── Stat cards (M1 + H5) ─────────────────────────────────────────────────────

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean
) {
    Box {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp
        ) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = contentDescription,
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (isActive) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun StatCardsRow(
    expiredCount: Int,
    soonCount: Int,
    freshCount: Int,
    activeFilter: StatusFilter,
    onFilterChange: (StatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatCard(expiredCount, "Périmés", ColorExpired,
            activeFilter == StatusFilter.EXPIRED, Icons.Default.Warning, Modifier.weight(1f)) {
            onFilterChange(if (activeFilter == StatusFilter.EXPIRED) StatusFilter.ALL else StatusFilter.EXPIRED)
        }
        StatCard(soonCount, "Bientôt", ColorSoon,
            activeFilter == StatusFilter.SOON, Icons.Default.Schedule, Modifier.weight(1f)) {
            onFilterChange(if (activeFilter == StatusFilter.SOON) StatusFilter.ALL else StatusFilter.SOON)
        }
        StatCard(freshCount, "Frais", ColorFresh,
            activeFilter == StatusFilter.FRESH, Icons.Default.CheckCircle, Modifier.weight(1f)) {
            onFilterChange(if (activeFilter == StatusFilter.FRESH) StatusFilter.ALL else StatusFilter.FRESH)
        }
    }
}

@Composable
private fun StatCard(
    count: Int, label: String, color: Color, isActive: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick, modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) color.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isActive) BorderStroke(1.dp, color)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(MaterialTheme.shapes.small)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(
                    count.toString(),
                    style = FreshTextStyles.StatCount,
                    color = if (isActive || count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    label,
                    style = FreshTextStyles.StatLabel,
                    color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Alert banner (M5) ────────────────────────────────────────────────────────

@Composable
private fun AlertBanner(expiredCount: Int, onClick: () -> Unit) {
    val plural = expiredCount > 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(ColorExpired.copy(alpha = 0.09f))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, null, tint = ColorExpired, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "$expiredCount produit${if (plural) "s" else ""} ${if (plural) "sont" else "est"} périmé${if (plural) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = ColorExpired,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f)
        )
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = ColorExpired.copy(alpha = 0.10f)
        ) {
            Text(
                "Voir",
                color = ColorExpired,
                style = FreshTextStyles.ActionLabel,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

// ── Skeleton loading (M2) ─────────────────────────────────────────────────────

@Composable
private fun ProductCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth(0.64f).height(12.dp).clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth(0.30f).height(10.dp).clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Box(
                Modifier.width(42.dp).height(20.dp).clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun FabBubbleMenu(
    expanded: Boolean,
    reduceMotion: Boolean,
    onToggle: () -> Unit,
    onSingleProduct: () -> Unit,
    onMultiProduct: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (reduceMotion) {
            if (expanded) {
                FabOptions(onMultiProduct = onMultiProduct, onSingleProduct = onSingleProduct)
            }
        } else {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                        scaleIn(spring(stiffness = Spring.StiffnessMedium), initialScale = 0.7f),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.7f)
            ) {
                FabOptions(onMultiProduct = onMultiProduct, onSingleProduct = onSingleProduct)
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 10.dp,
                pressedElevation = 6.dp,
                focusedElevation = 10.dp,
                hoveredElevation = 10.dp
            )
        ) {
            if (reduceMotion) {
                Icon(
                    if (expanded) Icons.Default.Close else Icons.Default.Add,
                    if (expanded) "Fermer" else "Ajouter un produit",
                    modifier = Modifier.size(32.dp)
                )
            } else {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(100)) + scaleIn(tween(100)),
                    exit = fadeOut(tween(100)) + scaleOut(tween(100))
                ) {
                    Icon(Icons.Default.Close, "Fermer", modifier = Modifier.size(32.dp))
                }
                AnimatedVisibility(
                    visible = !expanded,
                    enter = fadeIn(tween(100)) + scaleIn(tween(100)),
                    exit = fadeOut(tween(100)) + scaleOut(tween(100))
                ) {
                    Icon(Icons.Default.Add, "Ajouter un produit", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun FabOptions(
    onMultiProduct: () -> Unit,
    onSingleProduct: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BubbleOption(
            label = "Plusieurs produits",
            onClick = onMultiProduct,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        BubbleOption(
            label = "Un produit",
            onClick = onSingleProduct,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun BubbleOption(
    label: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Text(
            text = label,
            color = contentColor,
            style = FreshTextStyles.ButtonLabel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

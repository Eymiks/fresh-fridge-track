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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.freshtrack.domain.catalog.PRODUCT_CATEGORIES
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getDaysUntilExpiration
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.domain.model.isActive
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
    onMultiScanClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditProduct: (String) -> Unit = {},
    vm: IndexViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val groups by vm.groups.collectAsState()
    val totalCounts by vm.totalCounts.collectAsState()
    val products by vm.products.collectAsState()
    val reduceMotion = LocalAppearance.current.reduceMotion

    var showSortMenu by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    var expiredCollapsed by rememberSaveable { mutableStateOf(false) }
    var soonCollapsed by rememberSaveable { mutableStateOf(false) }
    var freshCollapsed by rememberSaveable { mutableStateOf(false) }

    val headerBg = when {
        totalCounts.expired > 0 -> ColorExpired.copy(alpha = 0.14f)
        totalCounts.soon > 0 -> ColorSoon.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    }

    val isInitialLoading = ui.isLoading && products.isEmpty()
    val hasNoActiveProducts = !ui.isLoading && products.none { it.isActive() }
    val hasNoFilteredProducts = !ui.isLoading &&
        groups.expired.isEmpty() && groups.soon.isEmpty() && groups.fresh.isEmpty()

    Scaffold(
        topBar = {
            Column(Modifier.background(headerBg)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = ui.searchQuery,
                        onValueChange = { vm.setSearch(it) },
                        placeholder = { Text("Rechercher un produit…", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
                        trailingIcon = if (ui.searchQuery.isNotEmpty()) {
                            { IconButton(onClick = { vm.setSearch("") }) { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) } }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                    Box {
                        IconButton(onClick = { showSortMenu = !showSortMenu }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, "Trier")
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
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Paramètres")
                    }
                }

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
                        item(key = "stat_cards", contentType = "stat_cards") {
                            StatCardsRow(
                                expiredCount = totalCounts.expired,
                                soonCount = totalCounts.soon,
                                freshCount = totalCounts.fresh,
                                activeFilter = ui.statusFilter,
                                onFilterChange = { vm.setStatusFilter(it) }
                            )
                        }

                        if (totalCounts.expired > 0 && ui.statusFilter == StatusFilter.ALL) {
                            item(key = "alert_banner", contentType = "alert_banner") {
                                AlertBanner(totalCounts.expired)
                            }
                        }

                        if (groups.expired.isNotEmpty()) {
                            stickyHeader(key = "header_expired", contentType = "section_header") {
                                CollapsibleSectionHeader(
                                    title = "⚠ Périmés (${groups.expired.size})",
                                    color = ColorExpired,
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
                                CollapsibleSectionHeader(
                                    title = "⏰ Bientôt périmés (${groups.soon.size})",
                                    color = ColorSoon,
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
                                CollapsibleSectionHeader(
                                    title = "✓ Frais (${groups.fresh.size})",
                                    color = ColorFresh,
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
                                        fontWeight = FontWeight.SemiBold
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
            containerColor = if (isActive) color.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isActive) BorderStroke(1.dp, color) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(MaterialTheme.shapes.small)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
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
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.8.sp,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
    onEdit: (() -> Unit)? = null,
    onConsume: (() -> Unit)? = null,
    onThrow: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val status = remember(
        product.status,
        product.openedAt,
        product.daysAfterOpening,
        product.expirationDate,
        product.frozenUntil
    ) { product.getExpirationStatus() }
    val daysLeft = remember(
        product.status,
        product.openedAt,
        product.daysAfterOpening,
        product.expirationDate,
        product.frozenUntil
    ) { product.getDaysUntilExpiration() }
    val statusColor = remember(status) {
        when (status) {
            ExpirationStatus.EXPIRED -> ColorExpired
            ExpirationStatus.SOON -> ColorSoon
            ExpirationStatus.FRESH -> ColorFresh
        }
    }
    val daysLabel = remember(daysLeft) {
        when {
            daysLeft < 0 -> "Périmé depuis ${-daysLeft}j"
            daysLeft == 0 -> "Expire aujourd'hui"
            daysLeft == 1 -> "Expire demain"
            else -> "Expire dans ${daysLeft}j"
        }
    }
    val imageRequest: ImageRequest? = remember(product.imageUrl) {
        product.imageUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .build()
        }
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
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRoundRect(
                            color = statusColor,
                            size = Size(4.dp.toPx(), size.height),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                    .padding(start = 4.dp)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = null)
                    Spacer(Modifier.width(4.dp))
                }

                if (imageRequest != null) {
                    // Image + frozen badge overlay (L1)
                    Box(contentAlignment = Alignment.TopEnd) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop
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
                        fontWeight = FontWeight.Bold,
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
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Ouvert",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                                if (onEdit != null) {
                                    DropdownMenuItem(
                                        text = { Text("Modifier") },
                                        onClick = { showMenu = false; onEdit() },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, null,
                                                modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
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

        FloatingActionButton(onClick = onToggle) {
            if (reduceMotion) {
                Icon(
                    if (expanded) Icons.Default.Close else Icons.Default.Add,
                    if (expanded) "Fermer" else "Ajouter un produit"
                )
            } else {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(100)) + scaleIn(tween(100)),
                    exit = fadeOut(tween(100)) + scaleOut(tween(100))
                ) {
                    Icon(Icons.Default.Close, "Fermer")
                }
                AnimatedVisibility(
                    visible = !expanded,
                    enter = fadeIn(tween(100)) + scaleIn(tween(100)),
                    exit = fadeOut(tween(100)) + scaleOut(tween(100))
                ) {
                    Icon(Icons.Default.Add, "Ajouter un produit")
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
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

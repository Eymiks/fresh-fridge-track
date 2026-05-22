package com.freshtrack.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.freshtrack.data.openfoodfacts.OpenFoodFactsImageSize
import com.freshtrack.domain.catalog.PRODUCT_CATEGORIES
import com.freshtrack.domain.catalog.getFreezeDuration
import com.freshtrack.domain.catalog.getPostExpiryNote
import com.freshtrack.domain.catalog.getRecommendedDaysAfterOpening
import com.freshtrack.domain.format.formatDate
import com.freshtrack.domain.format.formatDateLong
import com.freshtrack.domain.format.formatInstantDate
import com.freshtrack.domain.format.formatInstantDateLong
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getDaysUntilExpiration
import com.freshtrack.domain.model.getEffectiveExpirationDate
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.ui.navigation.Routes
import com.freshtrack.ui.components.FreshProductImage
import com.freshtrack.ui.theme.ColorExpired
import com.freshtrack.ui.theme.ColorFresh
import com.freshtrack.ui.theme.ColorFrozen
import com.freshtrack.ui.theme.ColorSoon
import com.freshtrack.ui.theme.Nova1
import com.freshtrack.ui.theme.Nova2
import com.freshtrack.ui.theme.Nova3
import com.freshtrack.ui.theme.Nova4
import com.freshtrack.ui.theme.NutriA
import com.freshtrack.ui.theme.NutriB
import com.freshtrack.ui.theme.NutriC
import com.freshtrack.ui.theme.NutriD
import com.freshtrack.ui.theme.NutriE
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
    vm: ProductDetailViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val product = ui.product
    val snackbarHostState = remember { SnackbarHostState() }
    var notes by rememberSaveable(product?.notes) { mutableStateOf(product?.notes ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var copiedBarcode by remember(product?.barcode) { mutableStateOf(false) }
    LaunchedEffect(copiedBarcode) {
        if (copiedBarcode) {
            delay(2_000L)
            copiedBarcode = false
        }
    }
    var scoreDialog by remember { mutableStateOf<ScoreDialogType?>(null) }
    var showFullscreenImage by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var ingredientsExpanded by remember(product?.ingredients) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val showStickyHeader by remember(density) {
        derivedStateOf { scrollState.value > with(density) { 96.dp.roundToPx() } }
    }

    LaunchedEffect(ui.feedbackId) {
        val message = ui.feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        vm.clearFeedback()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (product != null && !ui.isLoading && ui.authMessage == null && !ui.notFound) {
                StickyBottomEditBar(
                    isEnabled = !ui.isMutating,
                    onEdit = { navController.navigate(Routes.editProduct(product.id)) },
                    onDelete = { showDeleteConfirm = true }
                )
            }
        }
    ) { innerPadding ->
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (ui.authMessage != null || ui.notFound || product == null) {
            val message = ui.authMessage
                ?: if (ui.notFound) "Produit introuvable." else "Impossible d'afficher ce produit."
            Box(
                Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    ui.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                    OutlinedButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retour")
                    }
                }
            }
            return@Scaffold
        }

        val daysLeft = product.getDaysUntilExpiration()
        val expirationStatus = product.getExpirationStatus()
        val statusColor = expirationStatus.color()
        val nutritionFacts = remember(product.nutritionData) { parseNutritionFacts(product.nutritionData) }
        val nutritionMap = remember(nutritionFacts) { nutritionFacts.associateBy { it.key } }
        val categoryLabel = remember(product.category) { product.categoryLabel() }
        val translatedAllergens = remember(product.allergens) { parseAllergens(product.allergens) }
        val additives = remember(product.ingredients) { extractAdditives(product.ingredients) }
        val nutritionInsights = remember(nutritionMap, nutritionFacts) {
            nutritionInsights(nutritionMap, nutritionFacts.size)
        }
        val notesDirty = notes != (product.notes ?: "")

        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 14.dp)
            ) {
                ProductFreshnessDashboardHero(
                    product = product,
                    categoryLabel = categoryLabel,
                    daysLeft = daysLeft,
                    expirationStatus = expirationStatus,
                    statusColor = statusColor,
                    onBack = { navController.popBackStack() },
                    onImageClick = { if (!product.imageUrl.isNullOrBlank()) showFullscreenImage = true },
                    onScoreClick = { scoreDialog = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )

                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionsCard(
                        product = product,
                        isMutating = ui.isMutating,
                        onOpen = { vm.showOpeningDialog() },
                        onSetStatus = vm::setStatus,
                        onFreeze = { vm.freezeProduct() },
                        onUnfreeze = { vm.unfreezeProduct() }
                    )
                    DeadlineCard(
                        product = product,
                        daysLeft = daysLeft,
                        expirationStatus = expirationStatus,
                        statusColor = statusColor
                    )
                    ProductDetailTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (selectedTab) {
                                0 -> NutritionAccordionContent(
                                    facts = nutritionFacts,
                                    nutritionMap = nutritionMap,
                                    allergens = translatedAllergens,
                                    insights = nutritionInsights
                                )
                                1 -> IngredientsAccordionContent(
                                    ingredients = product.ingredients,
                                    ingredientsExpanded = ingredientsExpanded,
                                    onToggleIngredients = { ingredientsExpanded = !ingredientsExpanded },
                                    allergens = translatedAllergens,
                                    additives = additives
                                )
                                2 -> DetailsAccordionContent(
                                    product = product,
                                    categoryLabel = categoryLabel,
                                    copiedBarcode = copiedBarcode,
                                    onCopyBarcode = { code ->
                                        clipboard.setText(AnnotatedString(code))
                                        copiedBarcode = true
                                    },
                                    notes = notes,
                                    onNotesChange = { notes = it },
                                    notesDirty = notesDirty,
                                    isMutating = ui.isMutating,
                                    onSaveNotes = { vm.updateNotes(notes) }
                                )
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = showStickyHeader,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                FloatingDetailHeader(
                    product = product,
                    onBack = { navController.popBackStack() },
                    onImageClick = { if (!product.imageUrl.isNullOrBlank()) showFullscreenImage = true }
                )
            }
        }
    }

    if (ui.showOpeningDialog && product != null) {
        OpeningDialog(
            product = product,
            isMutating = ui.isMutating,
            onConfirm = { days -> vm.openProduct(days) },
            onDismiss = { vm.dismissOpeningDialog() }
        )
    }

    if (showDeleteConfirm && product != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer ce produit ?") },
            text = { Text("Cette action retirera ${product.name} du frigo. Elle est définitive.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        vm.deleteProduct { navController.popBackStack() }
                    },
                    enabled = !ui.isMutating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
            }
        )
    }

    scoreDialog?.let { type ->
        ScoreExplanationDialog(type = type, onDismiss = { scoreDialog = null })
    }

    if (showFullscreenImage && product?.imageUrl != null) {
        FullscreenImageDialog(
            imageUrl = product.imageUrl,
            productName = product.name,
            onDismiss = { showFullscreenImage = false }
        )
    }
}

@Composable
private fun ProductFreshnessDashboardHero(
    product: Product,
    categoryLabel: String?,
    daysLeft: Int,
    expirationStatus: ExpirationStatus,
    statusColor: Color,
    onBack: () -> Unit,
    onImageClick: () -> Unit,
    onScoreClick: (ScoreDialogType) -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveDate = product.getEffectiveExpirationDate()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(statusColor.copy(alpha = 0.035f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, label = "Retour", onClick = onBack)
                Row(
                    Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(expirationStatus)
                    if (product.status != ProductStatus.ACTIVE) {
                        StatusBadge(product.status)
                    }
                    if (product.frozenUntil != null) {
                        StatusMiniPill(Icons.Default.AcUnit, "Congelé", ColorFrozen)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FreshnessGauge(
                    daysLeft = daysLeft,
                    expirationStatus = expirationStatus,
                    statusColor = statusColor,
                    modifier = Modifier.weight(1f)
                )
                ProductThumbnail(
                    product = product,
                    modifier = Modifier.size(width = 112.dp, height = 136.dp),
                    onClick = onImageClick
                )
            }

            HeroProductIdentity(product = product, categoryLabel = categoryLabel)
            HeroScoreStrip(product = product, effectiveDateText = formatDate(effectiveDate).orEmpty(), onScoreClick = onScoreClick)
        }
    }
}

@Composable
private fun FloatingIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.clearAndSetSemantics {
            role = Role.Button
            contentDescription = label
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = contentColor)
        }
    }
}

@Composable
private fun FreshnessGauge(
    daysLeft: Int,
    expirationStatus: ExpirationStatus,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(136.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.14f))
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(82.dp).align(Alignment.TopCenter)) {
                val strokeWidth = 14.dp.toPx()
                val arcSize = Size(width = size.width - strokeWidth, height = (size.height - strokeWidth) * 2f)
                val topLeft = Offset(x = strokeWidth / 2f, y = strokeWidth / 2f)
                drawArc(
                    color = statusColor.copy(alpha = 0.16f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = statusColor,
                    startAngle = 180f,
                    sweepAngle = 180f * freshnessGaugeProgress(daysLeft),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    deadlineCounterLabel(daysLeft),
                    style = MaterialTheme.typography.titleLarge,
                    color = statusColor,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    expirationStatus.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HeroProductIdentity(product: Product, categoryLabel: String?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            product.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            listOfNotNull(product.brand, product.quantity).joinToString(" · ").ifBlank { "Produit du foyer" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        categoryLabel?.let {
            CompactPill(
                label = listOfNotNull(it, product.subcategory).joinToString(" · "),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HeroScoreStrip(product: Product, effectiveDateText: String, onScoreClick: (ScoreDialogType) -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.36f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBadgesRow(product = product, onScoreClick = onScoreClick)
            StatusMiniPill(Icons.Default.Schedule, "Date $effectiveDateText", MaterialTheme.colorScheme.primary)
        }
    }
}

internal fun freshnessGaugeProgress(daysLeft: Int): Float = when {
    daysLeft < 0 -> 0f
    daysLeft == 0 -> 0.05f
    else -> (daysLeft / 30f).coerceAtMost(1f)
}

@Composable
private fun ProductThumbnail(
    product: Product,
    modifier: Modifier,
    onClick: () -> Unit,
    semanticLabel: String = "Photo de ${product.name}"
) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = semanticLabel
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FreshProductImage(
            imageUrl = product.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(22.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun DeadlineCard(
    product: Product,
    daysLeft: Int,
    expirationStatus: ExpirationStatus,
    statusColor: Color
) {
    val effectiveDate = product.getEffectiveExpirationDate()
    val counter = deadlineCounterLabel(daysLeft)
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(16.dp)).background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(expirationStatus.icon(), contentDescription = null, tint = statusColor)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Date limite",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        expirationStatus.label(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.20f))
                ) {
                    Text(
                        counter,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }

            Surface(
                color = statusColor.copy(alpha = 0.07f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        formatDateLong(effectiveDate).orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                    if (effectiveDate != product.expirationDate) {
                        Text(
                            "Date initiale : ${formatDate(product.expirationDate).orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (product.openedAt != null && product.daysAfterOpening != null) {
                val tz = TimeZone.currentSystemDefault()
                val today = Clock.System.todayIn(tz)
                val openedDate = product.openedAt.toLocalDateTime(tz).date
                val openedDays = maxOf(0, openedDate.daysUntil(today))
                DeadlineInfoRow(
                    icon = Icons.Default.Inventory2,
                    title = "Ouvert depuis $openedDays jour${if (openedDays > 1) "s" else ""}",
                    subtitle = if (effectiveDate != product.expirationDate) {
                        "Date effective : ${formatDate(effectiveDate).orEmpty()}"
                    } else {
                        null
                    },
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (product.frozenUntil != null) {
                DeadlineInfoRow(
                    icon = Icons.Default.AcUnit,
                    title = "Congelé jusqu'au ${formatDate(product.frozenUntil).orEmpty()}",
                    color = ColorFrozen
                )
            }

            getPostExpiryNote(product.category, product.subcategory)?.let { note ->
                DeadlineInfoRow(
                    icon = Icons.Default.Info,
                    title = "Conservation possible",
                    subtitle = "Jusqu'à $note après la date d'expiration.",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DeadlineInfoRow(
    icon: ImageVector,
    title: String,
    color: Color,
    subtitle: String? = null
) {
    Surface(
        color = color.copy(alpha = 0.07f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, color.copy(alpha = 0.14f))
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(13.dp)).background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = color,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun deadlineCounterLabel(daysLeft: Int): String = when {
    daysLeft < 0 -> "${-daysLeft} j de retard"
    daysLeft == 0 -> "Aujourd'hui"
    daysLeft == 1 -> "Demain"
    else -> "Dans $daysLeft j"
}

@Composable
private fun QuickActionsCard(
    product: Product,
    isMutating: Boolean,
    onOpen: () -> Unit,
    onSetStatus: (ProductStatus) -> Unit,
    onFreeze: () -> Unit,
    onUnfreeze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionEyebrow(icon = Icons.Default.CheckCircle, eyebrow = "Statut", title = "Actions rapides")
            // Always show all 3 status buttons like the PWA; clicking an active status deactivates it
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickStatusButton(
                    label = "Ouvert",
                    icon = Icons.Default.Inventory2,
                    selected = product.status == ProductStatus.OPENED,
                    color = MaterialTheme.colorScheme.primary,
                    enabled = !isMutating,
                    onClick = {
                        if (product.status == ProductStatus.OPENED) onSetStatus(ProductStatus.ACTIVE) else onOpen()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickStatusButton(
                    label = "Consommé",
                    icon = Icons.Default.Restaurant,
                    selected = product.status == ProductStatus.CONSUMED,
                    color = ColorFresh,
                    enabled = !isMutating,
                    onClick = {
                        onSetStatus(if (product.status == ProductStatus.CONSUMED) ProductStatus.ACTIVE else ProductStatus.CONSUMED)
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickStatusButton(
                    label = "Jeté",
                    icon = Icons.Default.Delete,
                    selected = product.status == ProductStatus.THROWN,
                    color = MaterialTheme.colorScheme.error,
                    enabled = !isMutating,
                    onClick = {
                        onSetStatus(if (product.status == ProductStatus.THROWN) ProductStatus.ACTIVE else ProductStatus.THROWN)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            // "Remettre en actif" visible when not active
            if (product.status != ProductStatus.ACTIVE) {
                Surface(
                    onClick = { onSetStatus(ProductStatus.ACTIVE) },
                    enabled = !isMutating,
                    modifier = Modifier.clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = "Remettre en actif"
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("Remettre en actif", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            }
            val freezeLabel = if (product.frozenUntil == null) {
                "Mettre au congélateur (${getFreezeDuration(product.category)} mois)"
            } else {
                "Retirer du congélateur"
            }
            Surface(
                onClick = if (product.frozenUntil == null) onFreeze else onUnfreeze,
                enabled = !isMutating,
                modifier = Modifier.clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = freezeLabel
                },
                shape = RoundedCornerShape(16.dp),
                color = ColorFrozen.copy(alpha = 0.10f)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AcUnit, contentDescription = null, tint = ColorFrozen, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        freezeLabel,
                        color = ColorFrozen,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isMutating) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Mise à jour…", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun QuickStatusButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) color else color.copy(alpha = 0.10f)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.White else color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(5.dp))
            Text(
                label,
                color = if (selected) Color.White else color,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProductDetailTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        Triple("Nutrition", Icons.Default.BarChart, "Onglet Nutrition et allergènes"),
        Triple("Ingrédients", Icons.Default.Menu, "Onglet Ingrédients"),
        Triple("Détails", Icons.Default.Info, "Onglet Détails et historique")
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, (label, icon, semanticLabel) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.clearAndSetSemantics {
                        role = Role.Tab
                        contentDescription = semanticLabel
                    }
                ) {
                    Column(
                        Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Medium,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionAccordionContent(
    facts: List<NutritionFact>,
    nutritionMap: Map<String, NutritionFact>,
    allergens: List<String>,
    insights: List<String>
) {
    if (facts.isEmpty() && allergens.isEmpty()) {
        EmptyState("Aucune information nutritionnelle disponible.")
        return
    }
    if (allergens.isNotEmpty()) {
        AlertInfoCard(
            title = "Allergènes à vérifier",
            subtitle = "À confirmer sur l'emballage avant consommation.",
            items = allergens
        )
    }
    NutritionSummaryGrid(nutritionMap)
    if (insights.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            insights.forEach { insight ->
                CompactPill(insight, if (insight == "Sucré" || insight == "Salé") ColorSoon else ColorFresh)
            }
        }
    }
    if (facts.isNotEmpty()) {
        Text(
            "Valeurs nutritionnelles pour 100 g",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black
        )
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
        ) {
            Column {
                facts.forEachIndexed { index, fact ->
                    NutritionTableRow(fact, index == facts.lastIndex)
                }
            }
        }
    }
}

@Composable
private fun NutritionSummaryGrid(nutritionMap: Map<String, NutritionFact>) {
    val summary = listOf("energy_kcal", "sugars", "salt", "proteins").mapNotNull { nutritionMap[it] }
    if (summary.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        summary.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { fact ->
                    NutritionSummaryCard(fact, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NutritionSummaryCard(fact: NutritionFact, modifier: Modifier = Modifier) {
    val tone = when (fact.key) {
        "sugars", "salt" -> ColorSoon
        "proteins", "fiber" -> ColorFresh
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = modifier.height(86.dp),
        colors = CardDefaults.cardColors(containerColor = tone.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(
                fact.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(fact.formatted, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun NutritionTableRow(fact: NutritionFact, isLast: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                start = if (fact.key == "saturated_fat" || fact.key == "sugars") 28.dp else 14.dp,
                end = 14.dp,
                top = 11.dp,
                bottom = 11.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(fact.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(fact.formatted, fontWeight = FontWeight.Black)
    }
    if (!isLast) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)))
    }
}

@Composable
private fun IngredientsAccordionContent(
    ingredients: String?,
    ingredientsExpanded: Boolean,
    onToggleIngredients: () -> Unit,
    allergens: List<String>,
    additives: List<String>
) {
    if (ingredients.isNullOrBlank()) {
        EmptyState("Aucun ingrédient renseigné.")
    } else {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                val isLong = ingredients.length > 280
                val displayed = if (isLong && !ingredientsExpanded) ingredients.take(280).trim() + "…" else ingredients
                Text(displayed, style = MaterialTheme.typography.bodyMedium, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
                if (isLong) {
                    TextButton(onClick = onToggleIngredients) {
                        Text(if (ingredientsExpanded) "Voir moins" else "Voir la liste complète")
                    }
                }
            }
        }
    }
    if (allergens.isNotEmpty()) {
        AlertInfoCard(title = "Allergènes signalés", subtitle = null, items = allergens)
    }
    if (additives.isNotEmpty()) {
        Text("Additifs détectés", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            additives.forEach { CompactPill(it, ColorSoon) }
        }
    }
    Text(
        "Liste issue d'OpenFoodFacts, à vérifier sur l'emballage.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DetailsAccordionContent(
    product: Product,
    categoryLabel: String?,
    copiedBarcode: Boolean,
    onCopyBarcode: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    notesDirty: Boolean,
    isMutating: Boolean,
    onSaveNotes: () -> Unit
) {
    // ── IDENTITÉ ── icon rows like PWA's DetailRow
    DetailsSectionLabel("Identité")
    Spacer(Modifier.height(8.dp))
    val catValue = listOfNotNull(categoryLabel ?: product.category, product.subcategory).joinToString(" › ")
    if (!product.brand.isNullOrBlank()) {
        IconDetailRow(icon = Icons.Default.Sell, label = "Marque", value = product.brand)
        Spacer(Modifier.height(10.dp))
    }
    if (!product.quantity.isNullOrBlank()) {
        IconDetailRow(icon = Icons.Default.Scale, label = "Quantité", value = product.quantity)
        Spacer(Modifier.height(10.dp))
    }
    if (catValue.isNotBlank()) {
        IconDetailRow(icon = Icons.Default.Category, label = "Catégorie", value = catValue)
    }

    Spacer(Modifier.height(16.dp))

    // ── NOTE DU FOYER ──
    DetailsSectionLabel("Note du foyer")
    Spacer(Modifier.height(8.dp))
    NotesField(
        notes = notes,
        onNotesChange = onNotesChange,
        isDirty = notesDirty,
        isMutating = isMutating,
        onSave = onSaveNotes
    )

    Spacer(Modifier.height(16.dp))

    // ── HISTORIQUE ──
    DetailsSectionLabel("Historique")
    Spacer(Modifier.height(8.dp))
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // Build timeline events to determine isLast
            val events = buildList {
                add(Triple(
                    "Produit ajouté",
                    formatInstantDateLong(product.addedAt).orEmpty(),
                    Triple<String?, ImageVector, Color>(null, Icons.Default.Schedule, Color.Unspecified)
                ))
                product.openedAt?.let { openedAt ->
                    add(Triple(
                        "Produit ouvert",
                        formatInstantDateLong(openedAt).orEmpty(),
                        Triple(
                            product.daysAfterOpening?.let { d -> "À consommer idéalement sous $d jour${if (d > 1) "s" else ""} après ouverture." },
                            Icons.Default.Inventory2,
                            ColorFrozen
                        )
                    ))
                }
                product.frozenUntil?.let { frozen ->
                    add(Triple(
                        "Congélation active",
                        formatDateLong(frozen).orEmpty(),
                        Triple("La date effective prend cette congélation en compte.", Icons.Default.AcUnit, ColorFrozen)
                    ))
                }
                // Only show status change for consumed/thrown (not opened/active — like PWA)
                if (product.status == ProductStatus.CONSUMED || product.status == ProductStatus.THROWN) {
                    product.statusChangedAt?.let { changedAt ->
                        val (statusLabel, statusIcon, statusColor) = when (product.status) {
                            ProductStatus.CONSUMED -> Triple("Produit consommé", Icons.Default.Restaurant, ColorFresh)
                            ProductStatus.THROWN -> Triple("Produit jeté", Icons.Default.Delete, ColorExpired)
                            else -> return@let
                        }
                        add(Triple(
                            statusLabel,
                            formatInstantDateLong(changedAt).orEmpty(),
                            Triple<String?, ImageVector, Color>(null, statusIcon, statusColor)
                        ))
                    }
                }
            }
            events.forEachIndexed { index, (title, date, extra) ->
                val (detail, icon, color) = extra
                TimelineRow(
                    title = title,
                    date = date,
                    detail = detail,
                    icon = icon,
                    color = color,
                    isLast = index == events.lastIndex
                )
            }
        }
    }

    // Barcode — centered below timeline, like PWA
    if (!product.barcode.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.QrCode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                product.barcode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { onCopyBarcode(product.barcode) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (copiedBarcode) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = if (copiedBarcode) "Code copié" else "Copier le code-barres",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Text(
        "Les informations proviennent d'OpenFoodFacts et peuvent être inexactes ou incomplètes.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    )
}

@Composable
private fun StickyBottomEditBar(isEnabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(tonalElevation = 4.dp, shadowElevation = 6.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onEdit,
                enabled = isEnabled,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = "Modifier le produit"
                    }
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Modifier")
            }
            Button(
                onClick = onDelete,
                enabled = isEnabled,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = "Supprimer le produit"
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Supprimer")
            }
        }
    }
}

@Composable
private fun FloatingDetailHeader(product: Product, onBack: () -> Unit, onImageClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = "Retour"
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            ProductThumbnail(
                product = product,
                modifier = Modifier.size(34.dp),
                onClick = onImageClick,
                semanticLabel = "Photo de ${product.name}"
            )
            Spacer(Modifier.width(10.dp))
            Text(
                product.name,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionEyebrow(icon: ImageVector, eyebrow: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        }
        Column {
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black
            )
            Text(title, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ScoreBadgesRow(product: Product, onScoreClick: (ScoreDialogType) -> Unit) {
    normalizeVisibleScore(product.nutriScore, VisibleScoreKind.LETTER)?.let { grade ->
        ScoreMiniBadge("Nutri", grade, scoreColor(grade), onClick = { onScoreClick(ScoreDialogType.NUTRI) })
    }
    normalizeVisibleScore(product.novaGroup?.toString(), VisibleScoreKind.NOVA)?.let { group ->
        ScoreMiniBadge("NOVA", group, novaColor(group.toInt()), onClick = { onScoreClick(ScoreDialogType.NOVA) })
    }
    normalizeVisibleScore(product.ecoScore, VisibleScoreKind.LETTER)?.let { grade ->
        ScoreMiniBadge("Eco", grade, scoreColor(grade), onClick = { onScoreClick(ScoreDialogType.ECO) })
    }
}

internal enum class VisibleScoreKind { LETTER, NOVA }

internal fun normalizeVisibleScore(raw: String?, kind: VisibleScoreKind): String? {
    val value = raw?.trim()?.uppercase().orEmpty()
    if (value.isBlank()) return null
    val normalized = value.lowercase()
    if (normalized in setOf("n/a", "na", "-", "not-applicable", "not_applicable", "not applicable", "unknown", "undefined")) {
        return null
    }
    return when (kind) {
        VisibleScoreKind.LETTER -> value.takeIf { it in setOf("A", "B", "C", "D", "E") }
        VisibleScoreKind.NOVA -> value.takeIf { it in setOf("1", "2", "3", "4") }
    }
}

@Composable
private fun ScoreMiniBadge(label: String, value: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Column(
            Modifier
                .width(48.dp)
                .height(52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    value,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun StatusBadge(expirationStatus: ExpirationStatus) {
    val (label, color, icon) = when (expirationStatus) {
        ExpirationStatus.FRESH -> Triple("Frais", ColorFresh, Icons.Default.CheckCircle)
        ExpirationStatus.SOON -> Triple("Bientôt", ColorSoon, Icons.Default.Warning)
        ExpirationStatus.EXPIRED -> Triple("Périmé", ColorExpired, Icons.Default.Warning)
    }
    StatusMiniPill(icon, label, color)
}

@Composable
private fun StatusBadge(status: ProductStatus) {
    val (label, color, icon) = when (status) {
        ProductStatus.ACTIVE -> Triple("Actif", MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)
        ProductStatus.OPENED -> Triple("Ouvert", MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)
        ProductStatus.CONSUMED -> Triple("Consommé", ColorFresh, Icons.Default.Check)
        ProductStatus.THROWN -> Triple("Jeté", MaterialTheme.colorScheme.error, Icons.Default.Delete)
    }
    StatusMiniPill(icon, label, color)
}

@Composable
private fun StatusMiniPill(icon: ImageVector, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.12f))
    ) {
        Row(
            Modifier.height(32.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AlertInfoCard(title: String, subtitle: String?, items: List<String>) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Column {
                    Text(title, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                    subtitle?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item -> CompactPill(item, MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun NotesField(
    notes: String,
    onNotesChange: (String) -> Unit,
    isDirty: Boolean,
    isMutating: Boolean,
    onSave: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            placeholder = { Text("Ajouter une note utile pour tout le foyer…") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 200.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.hasFocus && isDirty) onSave()
                },
            maxLines = 7,
            shape = RoundedCornerShape(16.dp)
        )
        Surface(
            onClick = onSave,
            enabled = isDirty && !isMutating,
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
            shape = CircleShape,
            color = if (isDirty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = if (isDirty) 4.dp else 0.dp
        ) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                if (isMutating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = if (isDirty) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Sauvegarder la note",
                        modifier = Modifier.size(16.dp),
                        tint = if (isDirty) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.18f, androidx.compose.ui.unit.TextUnitType.Em)
    )
}

@Composable
private fun IconDetailRow(icon: ImageVector, label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun TimelineRow(
    title: String,
    date: String,
    detail: String? = null,
    icon: ImageVector = Icons.Default.Check,
    color: Color = Color.Unspecified,
    isLast: Boolean = false
) {
    val resolvedColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    Row(
        Modifier.fillMaxWidth().padding(bottom = if (isLast) 0.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(resolvedColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = resolvedColor)
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Column(Modifier.weight(1f).padding(top = 4.dp, bottom = if (isLast) 0.dp else 16.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!detail.isNullOrBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompactPill(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FullscreenImageDialog(imageUrl: String, productName: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            FreshProductImage(
                imageUrl = imageUrl,
                contentDescription = productName,
                modifier = Modifier.fillMaxWidth(),
                imageSize = OpenFoodFactsImageSize.FULL,
                shape = RectangleShape,
                tint = Color.White,
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            ) {
                Icon(Icons.Default.Close, "Fermer", tint = Color.White)
            }
        }
    }
}

private enum class ScoreDialogType { NUTRI, NOVA, ECO }

@Composable
private fun OpeningDialog(
    product: Product,
    isMutating: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val recommended = getRecommendedDaysAfterOpening(product.category, product.subcategory)
    var days by remember { mutableIntStateOf(recommended.first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marquer comme ouvert") },
        text = {
            Column {
                Text(recommended.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { if (days > 1) days-- }, enabled = !isMutating) { Text("-") }
                    Text("$days jour(s)", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { if (days < 365) days++ }, enabled = !isMutating) { Text("+") }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(days) }, enabled = !isMutating) {
                Text("Confirmer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun ScoreExplanationDialog(type: ScoreDialogType, onDismiss: () -> Unit) {
    val explanation = when (type) {
        ScoreDialogType.NUTRI -> ScoreExplanation(
            title = "Nutri-Score",
            description = "Le Nutri-Score évalue la qualité nutritionnelle globale d'un aliment de A (meilleur) à E (à limiter).",
            grades = listOf(
                "A" to "Excellente qualité nutritionnelle",
                "B" to "Bonne qualité nutritionnelle",
                "C" to "Qualité nutritionnelle moyenne",
                "D" to "Qualité nutritionnelle médiocre",
                "E" to "Mauvaise qualité nutritionnelle"
            )
        )
        ScoreDialogType.NOVA -> ScoreExplanation(
            title = "Score NOVA",
            description = "La classification NOVA évalue le degré de transformation des aliments.",
            grades = listOf(
                "1" to "Aliments non transformés ou peu transformés",
                "2" to "Ingrédients culinaires transformés",
                "3" to "Aliments transformés",
                "4" to "Produits ultra-transformés"
            )
        )
        ScoreDialogType.ECO -> ScoreExplanation(
            title = "Eco-Score",
            description = "L'Eco-Score mesure l'impact environnemental d'un produit alimentaire de A (faible impact) à E (fort impact).",
            grades = listOf(
                "A" to "Très faible impact environnemental",
                "B" to "Faible impact environnemental",
                "C" to "Impact environnemental modéré",
                "D" to "Impact environnemental élevé",
                "E" to "Impact environnemental très élevé"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(explanation.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(explanation.description)
                explanation.grades.forEach { (grade, desc) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(if (type == ScoreDialogType.NOVA) novaColor(grade.toInt()) else scoreColor(grade)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(grade, color = Color.White, fontWeight = FontWeight.Black)
                        }
                        Text(desc, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Compris") } }
    )
}

private data class ScoreExplanation(
    val title: String,
    val description: String,
    val grades: List<Pair<String, String>>
)

private fun ExpirationStatus.color(): Color = when (this) {
    ExpirationStatus.EXPIRED -> ColorExpired
    ExpirationStatus.SOON -> ColorSoon
    ExpirationStatus.FRESH -> ColorFresh
}

private fun ExpirationStatus.icon(): ImageVector = when (this) {
    ExpirationStatus.EXPIRED -> Icons.Default.Warning
    ExpirationStatus.SOON -> Icons.Default.Warning
    ExpirationStatus.FRESH -> Icons.Default.CheckCircle
}

private fun ExpirationStatus.label(): String = when (this) {
    ExpirationStatus.EXPIRED -> "Périmé"
    ExpirationStatus.SOON -> "Bientôt périmé"
    ExpirationStatus.FRESH -> "Frais"
}

private fun Product.categoryLabel(): String? =
    category?.let { key -> PRODUCT_CATEGORIES.firstOrNull { it.key == key }?.label ?: key }

private data class NutritionFact(
    val key: String,
    val label: String,
    val value: Double,
    val unit: String
) {
    val formatted: String get() = "${formatNutritionValue(value, unit)} $unit"
}

private fun parseNutritionFacts(raw: String?): List<NutritionFact> {
    if (raw.isNullOrBlank()) return emptyList()
    val obj = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyList()
    val specs = listOf(
        Triple("energy_kcal", "Énergie", "kcal"),
        Triple("fat", "Matières grasses", "g"),
        Triple("saturated_fat", "dont saturées", "g"),
        Triple("carbohydrates", "Glucides", "g"),
        Triple("sugars", "dont sucres", "g"),
        Triple("proteins", "Protéines", "g"),
        Triple("fiber", "Fibres", "g"),
        Triple("salt", "Sel", "g")
    )
    return specs.mapNotNull { (key, label, unit) ->
        val value = obj[key]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        NutritionFact(key, label, value, unit)
    }
}

private fun formatNutritionValue(value: Double, unit: String): String =
    if (unit == "kcal" || value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

private fun nutritionInsights(nutrition: Map<String, NutritionFact>, rowCount: Int): List<String> = buildList {
    if ((nutrition["sugars"]?.value ?: 0.0) >= 15) add("Sucré")
    if ((nutrition["salt"]?.value ?: 0.0) >= 1.5) add("Salé")
    if ((nutrition["proteins"]?.value ?: 0.0) >= 12) add("Source de protéines")
    if ((nutrition["fiber"]?.value ?: 0.0) >= 3) add("Source de fibres")
    if (rowCount in 1..4) add("Données partielles")
}

private val allergenTranslations = mapOf(
    "gluten" to "Gluten",
    "crustaceans" to "Crustacés",
    "eggs" to "Œufs",
    "fish" to "Poisson",
    "peanuts" to "Arachides",
    "soybeans" to "Soja",
    "soya" to "Soja",
    "milk" to "Lait",
    "nuts" to "Fruits à coque",
    "celery" to "Céleri",
    "mustard" to "Moutarde",
    "sesame" to "Sésame",
    "sesame-seeds" to "Sésame",
    "sulphites" to "Sulfites",
    "sulphur-dioxide-and-sulphites" to "Sulfites",
    "lupin" to "Lupin",
    "molluscs" to "Mollusques",
    "wheat" to "Blé",
    "barley" to "Orge",
    "oats" to "Avoine",
    "rye" to "Seigle",
    "lactose" to "Lactose"
)

private fun parseAllergens(raw: String?): List<String> =
    raw.orEmpty()
        .split(",")
        .map { it.trim().replace(Regex("^(en|fr):"), "").lowercase().replace(" ", "-") }
        .filter { it.isNotBlank() }
        .map { allergenTranslations[it] ?: it.replace("-", " ").replaceFirstChar { ch -> ch.uppercase() } }
        .distinct()

private fun extractAdditives(ingredients: String?): List<String> =
    Regex("\\bE\\s?\\d{3}[a-z]?\\b", RegexOption.IGNORE_CASE)
        .findAll(ingredients.orEmpty())
        .map { it.value.replace("\\s+".toRegex(), "").uppercase() }
        .distinct()
        .toList()

private fun scoreColor(score: String): Color = when (score.uppercase()) {
    "A" -> NutriA
    "B" -> NutriB
    "C" -> NutriC
    "D" -> NutriD
    "E" -> NutriE
    else -> Color.Gray
}

private fun novaColor(group: Int): Color = when (group) {
    1 -> Nova1
    2 -> Nova2
    3 -> Nova3
    4 -> Nova4
    else -> Color.Gray
}

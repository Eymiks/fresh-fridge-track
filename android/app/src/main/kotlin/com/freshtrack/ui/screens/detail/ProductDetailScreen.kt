package com.freshtrack.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.freshtrack.domain.catalog.PRODUCT_CATEGORIES
import com.freshtrack.domain.catalog.getFreezeDuration
import com.freshtrack.domain.catalog.getPostExpiryNote
import com.freshtrack.domain.catalog.getRecommendedDaysAfterOpening
import com.freshtrack.domain.format.formatDate
import com.freshtrack.domain.format.formatInstantDate
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getDaysUntilExpiration
import com.freshtrack.domain.model.getEffectiveExpirationDate
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.ui.navigation.Routes
import com.freshtrack.ui.theme.ColorExpired
import com.freshtrack.ui.theme.ColorFresh
import com.freshtrack.ui.theme.ColorSoon
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
    var notes by remember(product?.notes) { mutableStateOf(product?.notes ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var copiedBarcode by remember(product?.barcode) { mutableStateOf(false) }
    var scoreDialog by remember { mutableStateOf<ScoreDialogType?>(null) }
    var showFullscreenImage by remember { mutableStateOf(false) }
    var nutritionOpen by remember { mutableStateOf(true) }
    var ingredientsOpen by remember { mutableStateOf(true) }
    var detailsOpen by remember { mutableStateOf(false) }
    var ingredientsExpanded by remember(product?.ingredients) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val showStickyHeader by remember(density) {
        derivedStateOf { scrollState.value > with(density) { 110.dp.roundToPx() } }
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(message, style = MaterialTheme.typography.titleMedium)
                    ui.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
                Box(Modifier.fillMaxWidth().height(384.dp)) {
                    ProductHeroBackdrop(product = product, statusColor = statusColor)
                    TopFloatingActions(
                        onBack = { navController.popBackStack() }
                    )
                    ProductSummaryCard(
                        product = product,
                        categoryLabel = categoryLabel,
                        expirationStatus = expirationStatus,
                        onImageClick = { if (!product.imageUrl.isNullOrBlank()) showFullscreenImage = true },
                        onScoreClick = { scoreDialog = it },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp)
                    )
                }

                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DeadlineCard(
                        product = product,
                        daysLeft = daysLeft,
                        expirationStatus = expirationStatus,
                        statusColor = statusColor
                    )
                    QuickActionsCard(
                        product = product,
                        isMutating = ui.isMutating,
                        onOpen = { vm.showOpeningDialog() },
                        onSetStatus = vm::setStatus,
                        onFreeze = { vm.freezeProduct() },
                        onUnfreeze = { vm.unfreezeProduct() }
                    )
                    DetailAccordionCard(
                        title = "Nutrition & Allergènes",
                        icon = Icons.Default.BarChart,
                        expanded = nutritionOpen,
                        onToggle = { nutritionOpen = !nutritionOpen }
                    ) {
                        NutritionAccordionContent(
                            facts = nutritionFacts,
                            nutritionMap = nutritionMap,
                            allergens = translatedAllergens,
                            insights = nutritionInsights
                        )
                    }
                    DetailAccordionCard(
                        title = "Ingrédients",
                        icon = Icons.Default.Menu,
                        expanded = ingredientsOpen,
                        onToggle = { ingredientsOpen = !ingredientsOpen }
                    ) {
                        IngredientsAccordionContent(
                            ingredients = product.ingredients,
                            ingredientsExpanded = ingredientsExpanded,
                            onToggleIngredients = { ingredientsExpanded = !ingredientsExpanded },
                            allergens = translatedAllergens,
                            additives = additives
                        )
                    }
                    DetailAccordionCard(
                        title = "Détails & historique",
                        icon = Icons.Default.Info,
                        expanded = detailsOpen,
                        onToggle = { detailsOpen = !detailsOpen }
                    ) {
                        DetailsAccordionContent(
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
            AnimatedVisibility(
                visible = showStickyHeader,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                FloatingDetailHeader(product = product, onBack = { navController.popBackStack() })
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
private fun ProductHeroBackdrop(product: Product, statusColor: Color) {
    Box(Modifier.fillMaxWidth().height(260.dp)) {
        if (!product.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(18.dp),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.28f),
                                Color.Black.copy(alpha = 0.42f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                statusColor.copy(alpha = 0.26f),
                                statusColor.copy(alpha = 0.10f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun TopFloatingActions(
    onBack: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, label = "Retour", onClick = onBack)
        Spacer(Modifier.size(44.dp))
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
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = contentColor)
        }
    }
}

@Composable
private fun ProductSummaryCard(
    product: Product,
    categoryLabel: String?,
    expirationStatus: ExpirationStatus,
    onImageClick: () -> Unit,
    onScoreClick: (ScoreDialogType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductThumbnail(product = product, modifier = Modifier.size(width = 104.dp, height = 132.dp), onClick = onImageClick)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Fiche produit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(product.brand, product.quantity).joinToString(" · ")
                            .ifBlank { "Produit du foyer" },
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
            Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreBadgesRow(product = product, onScoreClick = onScoreClick)
                    StatusBadge(expirationStatus)
                    if (product.status != ProductStatus.ACTIVE) {
                        StatusBadge(product.status)
                    }
                    if (product.frozenUntil != null) {
                        CompactStatusPill(Icons.Default.AcUnit, "Congelé", Color(0xFF1976D2))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductThumbnail(product: Product, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!product.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DeadlineCard(
    product: Product,
    daysLeft: Int,
    expirationStatus: ExpirationStatus,
    statusColor: Color
) {
    val counter = when {
        daysLeft < 0 -> (-daysLeft).toString()
        daysLeft == 0 -> "!"
        else -> daysLeft.toString()
    }
    val label = when {
        daysLeft < 0 -> "jour${if (-daysLeft > 1) "s" else ""} de retard"
        daysLeft == 0 -> "Expire aujourd'hui"
        daysLeft == 1 -> "Expire demain"
        else -> "jours restants"
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(expirationStatus.icon(), contentDescription = null, tint = statusColor)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Date limite",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black
            )
            Text(
                counter,
                style = MaterialTheme.typography.displayLarge,
                color = statusColor,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(
                formatDate(product.getEffectiveExpirationDate()).orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            getPostExpiryNote(product.category, product.subcategory)?.takeIf { daysLeft < 0 }?.let { note ->
                Spacer(Modifier.height(10.dp))
                Text(
                    "Peut encore se consommer : $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    product: Product,
    isMutating: Boolean,
    onOpen: () -> Unit,
    onSetStatus: (ProductStatus) -> Unit,
    onFreeze: () -> Unit,
    onUnfreeze: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionEyebrow(icon = Icons.Default.CheckCircle, eyebrow = "Statut", title = "Actions rapides")
            if (product.status == ProductStatus.CONSUMED || product.status == ProductStatus.THROWN) {
                Button(
                    onClick = { onSetStatus(ProductStatus.ACTIVE) },
                    enabled = !isMutating,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remettre actif") }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickStatusButton(
                        label = "Ouvert",
                        icon = Icons.Default.CheckCircle,
                        selected = product.status == ProductStatus.OPENED,
                        color = MaterialTheme.colorScheme.primary,
                        enabled = !isMutating,
                        onClick = onOpen,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatusButton(
                        label = "Consommé",
                        icon = Icons.Default.Check,
                        selected = product.status == ProductStatus.CONSUMED,
                        color = ColorFresh,
                        enabled = !isMutating,
                        onClick = { onSetStatus(ProductStatus.CONSUMED) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatusButton(
                        label = "Jeté",
                        icon = Icons.Default.Delete,
                        selected = product.status == ProductStatus.THROWN,
                        color = MaterialTheme.colorScheme.error,
                        enabled = !isMutating,
                        onClick = { onSetStatus(ProductStatus.THROWN) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Surface(
                onClick = if (product.frozenUntil == null) onFreeze else onUnfreeze,
                enabled = !isMutating,
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1976D2).copy(alpha = 0.10f)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AcUnit, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (product.frozenUntil == null) {
                            "Mettre au congélateur (${getFreezeDuration(product.category)} mois)"
                        } else {
                            "Retirer du congélateur"
                        },
                        color = Color(0xFF1976D2),
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
private fun DetailAccordionCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content()
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
    InfoCard(title = "Identité") {
        InfoRow("Catégorie", listOfNotNull(categoryLabel ?: product.category, product.subcategory).joinToString(" · "))
        InfoRow("Marque", product.brand)
        InfoRow("Quantité", product.quantity)
        InfoRow("Péremption", formatDate(product.expirationDate))
        InfoRow("Congelé jusqu'au", formatDate(product.frozenUntil))
        InfoRow("Ajouté par", product.addedByName)
        BarcodeRow(product.barcode, copiedBarcode, onCopyBarcode)
    }
    InfoCard(title = "Note du foyer") {
        NotesField(
            notes = notes,
            onNotesChange = onNotesChange,
            isDirty = notesDirty,
            isMutating = isMutating,
            onSave = onSaveNotes
        )
    }
    InfoCard(title = "Historique") {
        TimelineRow("Produit ajouté", formatInstantDate(product.addedAt).orEmpty())
        product.openedAt?.let {
            TimelineRow(
                "Produit ouvert",
                formatInstantDate(it).orEmpty(),
                product.daysAfterOpening?.let { days -> "À consommer dans $days jour(s) après ouverture" }
            )
        }
        product.frozenUntil?.let {
            TimelineRow("Congélation active", formatDate(it).orEmpty(), "La date effective prend cette congélation en compte.")
        }
        product.statusChangedAt?.let { changedAt ->
            val statusLabel = when (product.status) {
                ProductStatus.ACTIVE -> "Produit remis actif"
                ProductStatus.OPENED -> "Statut mis à ouvert"
                ProductStatus.CONSUMED -> "Produit consommé"
                ProductStatus.THROWN -> "Produit jeté"
            }
            TimelineRow(statusLabel, formatInstantDate(changedAt).orEmpty())
        }
    }
}

@Composable
private fun StickyBottomEditBar(isEnabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(tonalElevation = 4.dp, shadowElevation = 6.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onEdit,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Modifier")
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Supprimer")
            }
        }
    }
}

@Composable
private fun FloatingDetailHeader(product: Product, onBack: () -> Unit) {
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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
            }
            ProductThumbnail(product = product, modifier = Modifier.size(34.dp), onClick = {})
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
    product.nutriScore?.let {
        ScoreBadge("Nutri", it.uppercase(), scoreColor(it), onClick = { onScoreClick(ScoreDialogType.NUTRI) })
    }
    product.novaGroup?.let {
        ScoreBadge("NOVA", it.toString(), novaColor(it), onClick = { onScoreClick(ScoreDialogType.NOVA) })
    }
    product.ecoScore?.let {
        ScoreBadge("Eco", it.uppercase(), scoreColor(it), onClick = { onScoreClick(ScoreDialogType.ECO) })
    }
}

@Composable
private fun ScoreBadge(label: String, value: String, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(13.dp), color = Color.Transparent) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(54.dp)) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(value, color = Color.White, fontWeight = FontWeight.Black)
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
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
    CompactStatusPill(icon, label, color)
}

@Composable
private fun StatusBadge(status: ProductStatus) {
    val (label, color, icon) = when (status) {
        ProductStatus.ACTIVE -> Triple("Actif", MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)
        ProductStatus.OPENED -> Triple("Ouvert", MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)
        ProductStatus.CONSUMED -> Triple("Consommé", ColorFresh, Icons.Default.Check)
        ProductStatus.THROWN -> Triple("Jeté", MaterialTheme.colorScheme.error, Icons.Default.Delete)
    }
    CompactStatusPill(icon, label, color)
}

@Composable
private fun CompactStatusPill(icon: ImageVector, label: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.14f)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AlertInfoCard(title: String, subtitle: String?, items: List<String>) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.70f)),
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
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Notes") },
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus && isDirty) onSave()
            },
        maxLines = 7
    )
    if (isDirty || isMutating) {
        Text(
            if (isMutating) "Sauvegarde…" else "Modification non sauvegardée",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.45f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            title?.let {
                Text(it, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(0.42f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            value,
            modifier = Modifier.weight(0.58f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BarcodeRow(barcode: String?, copied: Boolean, onCopy: (String) -> Unit) {
    if (barcode.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Code-barres",
            modifier = Modifier.weight(0.42f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Text(barcode, modifier = Modifier.weight(0.46f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { onCopy(barcode) }) {
            Icon(
                if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = if (copied) "Code copié" else "Copier le code-barres"
            )
        }
    }
}

@Composable
private fun TimelineRow(title: String, date: String, detail: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
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
            AsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier.fillMaxWidth(),
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
                    TextButton(onClick = { days++ }, enabled = !isMutating) { Text("+") }
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
    "A" -> Color(0xFF1B5E20)
    "B" -> Color(0xFF558B2F)
    "C" -> Color(0xFFF9A825)
    "D" -> Color(0xFFE65100)
    "E" -> Color(0xFFB71C1C)
    else -> Color.Gray
}

private fun novaColor(group: Int): Color = when (group) {
    1 -> Color(0xFF1B5E20)
    2 -> Color(0xFFF9A825)
    3 -> Color(0xFFE65100)
    4 -> Color(0xFFB71C1C)
    else -> Color.Gray
}

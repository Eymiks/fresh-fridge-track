package com.freshtrack.ui.screens.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
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
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.ui.navigation.Routes
import com.freshtrack.ui.theme.ColorExpired
import com.freshtrack.ui.theme.ColorFresh
import com.freshtrack.ui.theme.ColorSoon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
    vm: ProductDetailViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val product = ui.product
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var notes by remember(product?.notes) { mutableStateOf(product?.notes ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var copiedBarcode by remember(product?.barcode) { mutableStateOf(false) }
    var scoreDialog by remember { mutableStateOf<ScoreDialogType?>(null) }
    var showFullscreenImage by remember { mutableStateOf(false) }
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        product?.name ?: "Produit",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (product != null) {
                        IconButton(
                            onClick = { navController.navigate(Routes.editProduct(product.id)) },
                            enabled = !ui.isMutating
                        ) {
                            Icon(Icons.Default.Edit, "Modifier")
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = !ui.isMutating
                        ) {
                            Icon(Icons.Default.Delete, "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
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
        val notesDirty = notes != (product.notes ?: "")

        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                ProductHero(
                    product = product,
                    categoryLabel = categoryLabel,
                    expirationStatus = expirationStatus,
                    statusColor = statusColor,
                    daysLeft = daysLeft,
                    onImageClick = { if (!product.imageUrl.isNullOrBlank()) showFullscreenImage = true }
                )

                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusActions(
                        product = product,
                        isMutating = ui.isMutating,
                        onOpen = { vm.showOpeningDialog() },
                        onSetStatus = vm::setStatus,
                        onFreeze = { vm.freezeProduct() },
                        onUnfreeze = { vm.unfreezeProduct() }
                    )

                    ScoreBadgesRow(product = product, onScoreClick = { scoreDialog = it })
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Infos") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Nutrition") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Notes") })
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Historique") })
                }

                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> InfoTab(
                            product = product,
                            categoryLabel = categoryLabel,
                            copiedBarcode = copiedBarcode,
                            onCopyBarcode = { code ->
                                clipboard.setText(AnnotatedString(code))
                                copiedBarcode = true
                            }
                        )
                        1 -> NutritionTab(
                            facts = nutritionFacts,
                            nutritionMap = nutritionMap,
                            allergens = product.allergens,
                            ingredients = product.ingredients,
                            ingredientsExpanded = ingredientsExpanded,
                            onToggleIngredients = { ingredientsExpanded = !ingredientsExpanded }
                        )
                        2 -> NotesTab(
                            notes = notes,
                            onNotesChange = { notes = it },
                            isDirty = notesDirty,
                            isMutating = ui.isMutating,
                            onSave = { vm.updateNotes(notes) }
                        )
                        3 -> HistoryTab(product)
                    }
                }
            }
            AnimatedVisibility(
                visible = showStickyHeader,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                StickyProductHeader(product = product, onBack = { navController.popBackStack() })
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
                    enabled = !ui.isMutating
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
private fun ProductHero(
    product: Product,
    categoryLabel: String?,
    expirationStatus: ExpirationStatus,
    statusColor: Color,
    daysLeft: Int,
    onImageClick: () -> Unit
) {
    val daysLabel = when {
        daysLeft < 0 -> "Périmé depuis ${-daysLeft} jour(s)"
        daysLeft == 0 -> "Expire aujourd'hui"
        daysLeft == 1 -> "Expire demain"
        else -> "Expire dans $daysLeft jours"
    }
    val statusLabel = when (expirationStatus) {
        ExpirationStatus.EXPIRED -> "Périmé"
        ExpirationStatus.SOON -> "Bientôt périmé"
        ExpirationStatus.FRESH -> "Frais"
    }
    val productStatusLabel = when (product.status) {
        ProductStatus.ACTIVE -> "Actif"
        ProductStatus.OPENED -> "Ouvert"
        ProductStatus.CONSUMED -> "Consommé"
        ProductStatus.THROWN -> "Jeté"
    }

    Box(Modifier.fillMaxWidth().height(300.dp)) {
        if (!product.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize().clickable(onClick = onImageClick),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(statusColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Produit", style = MaterialTheme.typography.headlineSmall, color = statusColor)
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactPill(statusLabel, statusColor)
                CompactPill(productStatusLabel, MaterialTheme.colorScheme.primary)
                if (product.frozenUntil != null) {
                    CompactPill("Congelé", Color(0xFF1976D2))
                }
            }
            Text(
                product.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(daysLabel, color = statusColor, fontWeight = FontWeight.Bold)
            Text(
                listOfNotNull(product.brand, product.quantity, categoryLabel).joinToString(" · ")
                    .ifBlank { "Produit du foyer" },
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (daysLeft < 0) {
                getPostExpiryNote(product.category, product.subcategory)?.let { note ->
                    Text(
                        "Peut encore se consommer : $note",
                        color = Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusActions(
    product: Product,
    isMutating: Boolean,
    onOpen: () -> Unit,
    onSetStatus: (ProductStatus) -> Unit,
    onFreeze: () -> Unit,
    onUnfreeze: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (product.status == ProductStatus.CONSUMED || product.status == ProductStatus.THROWN) {
                Button(
                    onClick = { onSetStatus(ProductStatus.ACTIVE) },
                    enabled = !isMutating,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remettre actif") }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onOpen, enabled = !isMutating, modifier = Modifier.weight(1f)) {
                        Text("Ouvert")
                    }
                    OutlinedButton(onClick = { onSetStatus(ProductStatus.CONSUMED) }, enabled = !isMutating, modifier = Modifier.weight(1f)) {
                        Text("Consommé")
                    }
                    OutlinedButton(onClick = { onSetStatus(ProductStatus.THROWN) }, enabled = !isMutating, modifier = Modifier.weight(1f)) {
                        Text("Jeté")
                    }
                }
            }
            if (product.frozenUntil == null) {
                TextButton(onClick = onFreeze, enabled = !isMutating) {
                    Icon(Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mettre au congélateur (${getFreezeDuration(product.category)} mois)")
                }
            } else {
                TextButton(onClick = onUnfreeze, enabled = !isMutating) {
                    Text("Retirer du congélateur")
                }
                Text(
                    "Congelé jusqu'au ${formatDate(product.frozenUntil).orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isMutating) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Mise à jour…", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ScoreBadgesRow(product: Product, onScoreClick: (ScoreDialogType) -> Unit) {
    val hasScore = !product.nutriScore.isNullOrBlank() || product.novaGroup != null || !product.ecoScore.isNullOrBlank()
    if (!hasScore) return

    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        product.nutriScore?.let { ScoreBadge("Nutri-Score", it.uppercase(), scoreColor(it), onClick = { onScoreClick(ScoreDialogType.NUTRI) }) }
        product.novaGroup?.let { ScoreBadge("NOVA", it.toString(), novaColor(it), onClick = { onScoreClick(ScoreDialogType.NOVA) }) }
        product.ecoScore?.let { ScoreBadge("Eco-Score", it.uppercase(), scoreColor(it), onClick = { onScoreClick(ScoreDialogType.ECO) }) }
    }
}

@Composable
private fun ScoreBadge(label: String, value: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(value, color = Color.White, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun InfoTab(
    product: Product,
    categoryLabel: String?,
    copiedBarcode: Boolean,
    onCopyBarcode: (String) -> Unit
) {
    InfoCard {
        InfoRow("Catégorie", categoryLabel ?: product.category)
        InfoRow("Sous-catégorie", product.subcategory)
        InfoRow("Marque", product.brand)
        InfoRow("Quantité", product.quantity)
        BarcodeRow(product.barcode, copiedBarcode, onCopyBarcode)
        InfoRow("Péremption", formatDate(product.expirationDate))
        InfoRow("Congelé jusqu'au", formatDate(product.frozenUntil))
        InfoRow("Ajouté par", product.addedByName)
        if (product.status == ProductStatus.OPENED) {
            InfoRow("Ouvert le", formatInstantDate(product.openedAt))
            InfoRow("DLC ouverture", product.daysAfterOpening?.let { "$it jours" })
        }
    }
}

@Composable
private fun NutritionTab(
    facts: List<NutritionFact>,
    nutritionMap: Map<String, NutritionFact>,
    allergens: String?,
    ingredients: String?,
    ingredientsExpanded: Boolean,
    onToggleIngredients: () -> Unit
) {
    val translatedAllergens = remember(allergens) { parseAllergens(allergens) }
    val additives = remember(ingredients) { extractAdditives(ingredients) }
    val insights = remember(nutritionMap, facts) { nutritionInsights(nutritionMap, facts.size) }
    val hasContent = facts.isNotEmpty() || translatedAllergens.isNotEmpty() || !ingredients.isNullOrBlank()

    if (!hasContent) {
        EmptyState("Aucune information nutritionnelle disponible.")
        return
    }

    val summaryKeys = listOf("energy_kcal", "sugars", "salt", "proteins")
    val summary = summaryKeys.mapNotNull { nutritionMap[it] }
    if (summary.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            summary.forEach { fact ->
                NutritionSummaryCard(fact)
            }
        }
    }

    if (insights.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            insights.forEach { insight ->
                CompactPill(insight, MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (facts.isNotEmpty()) {
        InfoCard(title = "Valeurs nutritionnelles (100 g)") {
            facts.forEach { fact -> InfoRow(fact.label, fact.formatted) }
        }
    }

    if (translatedAllergens.isNotEmpty()) {
        InfoCard(title = "Allergènes") {
            Text(translatedAllergens.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (!ingredients.isNullOrBlank()) {
        InfoCard(title = "Ingrédients") {
            val isLong = ingredients.length > 280
            val displayed = if (isLong && !ingredientsExpanded) ingredients.take(280).trim() + "…" else ingredients
            Text(displayed, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isLong) {
                TextButton(onClick = onToggleIngredients) {
                    Text(if (ingredientsExpanded) "Réduire" else "Voir plus")
                }
            }
            if (additives.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Additifs détectés : ${additives.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NotesTab(
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
            .height(200.dp)
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus && isDirty) onSave()
            },
        maxLines = 10
    )
    if (isDirty || isMutating) {
        Text(
            if (isMutating) "Sauvegarde…" else "Modification non sauvegardée",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HistoryTab(product: Product) {
    InfoCard(title = "Historique du produit") {
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
private fun InfoCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            title?.let {
                Text(it, fontWeight = FontWeight.SemiBold)
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
private fun NutritionSummaryCard(fact: NutritionFact) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        modifier = Modifier.width(132.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(fact.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(fact.formatted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
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
            fontWeight = FontWeight.Bold
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

private fun ExpirationStatus.color(): Color = when (this) {
    ExpirationStatus.EXPIRED -> ColorExpired
    ExpirationStatus.SOON -> ColorSoon
    ExpirationStatus.FRESH -> ColorFresh
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
        Triple("sugars", "Sucres", "g"),
        Triple("fiber", "Fibres", "g"),
        Triple("proteins", "Protéines", "g"),
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

@Composable
private fun StickyProductHeader(product: Product, onBack: () -> Unit) {
    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
            }
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                product.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

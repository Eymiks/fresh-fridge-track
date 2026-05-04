package com.freshtrack.ui.screens.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.freshtrack.domain.catalog.getPostExpiryNote
import com.freshtrack.domain.catalog.getRecommendedDaysAfterOpening
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getDaysUntilExpiration
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.ui.navigation.Routes
import com.freshtrack.ui.theme.ColorExpired
import com.freshtrack.ui.theme.ColorFresh
import com.freshtrack.ui.theme.ColorSoon
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
    var selectedTab by remember { mutableIntStateOf(0) }
    var notes by remember(product?.notes) { mutableStateOf(product?.notes ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var copiedBarcode by remember(product?.barcode) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Produit") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (product != null) {
                        IconButton(onClick = { navController.navigate(Routes.editProduct(product.id)) }) {
                            Icon(Icons.Default.Edit, "Modifier")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
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
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
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
        val nutritionRows = remember(product.nutritionData) { parseNutritionRows(product.nutritionData) }
        val statusColor = when (expirationStatus) {
            ExpirationStatus.EXPIRED -> ColorExpired
            ExpirationStatus.SOON -> ColorSoon
            ExpirationStatus.FRESH -> ColorFresh
        }

        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            // Image
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Toujours visibles : badges + compteur + boutons statut
            Column(Modifier.padding(16.dp)) {
                ui.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }

                Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (!product.brand.isNullOrBlank()) {
                    Text(product.brand, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(8.dp))

                // Compteur jours
                val daysLabel = when {
                    daysLeft < 0 -> "Périmé depuis ${-daysLeft} jour(s)"
                    daysLeft == 0 -> "Expire aujourd'hui"
                    daysLeft == 1 -> "Expire demain"
                    else -> "Expire dans $daysLeft jours"
                }
                Text(daysLabel, color = statusColor, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium)

                // Note post-expiry
                if (daysLeft < 0) {
                    getPostExpiryNote(product.category, product.subcategory)?.let { note ->
                        Text("Peut encore se consommer : $note", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Badges scores (M10)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    product.nutriScore?.let { NutriScoreBadge(it) }
                    product.novaGroup?.let { NovaGroupBadge(it) }
                    product.ecoScore?.let { EcoScoreBadge(it) }
                }

                Spacer(Modifier.height(12.dp))

                // Boutons statut
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (product.status != ProductStatus.ACTIVE) {
                        TextButton(onClick = { vm.setStatus(ProductStatus.ACTIVE) }) { Text("Remettre actif") }
                    }
                    if (product.status == ProductStatus.ACTIVE) {
                        Button(onClick = { vm.showOpeningDialog() }) { Text("Ouvrir") }
                        TextButton(onClick = { vm.setStatus(ProductStatus.CONSUMED) }) { Text("Consommé") }
                        TextButton(onClick = { vm.setStatus(ProductStatus.THROWN) }) { Text("Jeté") }
                    }
                    if (product.status == ProductStatus.OPENED) {
                        TextButton(onClick = { vm.setStatus(ProductStatus.CONSUMED) }) { Text("Consommé") }
                        TextButton(onClick = { vm.setStatus(ProductStatus.THROWN) }) { Text("Jeté") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (product.frozenUntil == null) {
                        TextButton(onClick = { vm.freezeProduct() }) { Text("Congeler") }
                    } else {
                        TextButton(onClick = { vm.unfreezeProduct() }) { Text("Retirer du congélateur") }
                    }
                }
            }

            // Onglets
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Infos") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Nutrition") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Notes") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Historique") })
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        InfoRow("Catégorie", product.category)
                        InfoRow("Sous-catégorie", product.subcategory)
                        InfoRow("Quantité", product.quantity)
                        BarcodeRow(
                            barcode = product.barcode,
                            copied = copiedBarcode,
                            onCopy = { code ->
                                clipboard.setText(AnnotatedString(code))
                                copiedBarcode = true
                            }
                        )
                        InfoRow("Péremption", product.expirationDate.toString())
                        InfoRow("Congelé jusqu'au", product.frozenUntil?.toString())
                        InfoRow("Ajouté par", product.addedByName)
                        if (product.status == ProductStatus.OPENED) {
                            InfoRow("Ouvert le", product.openedAt?.toLocalDateTime(
                                TimeZone.currentSystemDefault())?.date?.toString())
                            InfoRow("DLC ouverture", product.daysAfterOpening?.let { "$it jours" })
                        }
                    }
                    1 -> {
                        if (nutritionRows.isNotEmpty()) {
                            Text("Valeurs nutritionnelles (100 g)", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            nutritionRows.forEach { row ->
                                InfoRow(row.label, row.value)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        if (!product.allergens.isNullOrBlank()) {
                            Text("Allergènes", fontWeight = FontWeight.SemiBold)
                            Text(
                                translateAllergens(product.allergens),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (!product.ingredients.isNullOrBlank()) {
                            Text("Ingrédients", fontWeight = FontWeight.SemiBold)
                            Text(product.ingredients, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (nutritionRows.isEmpty() && product.allergens.isNullOrBlank() && product.ingredients.isNullOrBlank()) {
                            Text("Aucune information nutritionnelle disponible.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    2 -> {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            maxLines = 10
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.updateNotes(notes) }, modifier = Modifier.align(Alignment.End)) {
                            Text("Sauvegarder")
                        }
                    }
                    3 -> {
                        Text("Historique du produit", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        TimelineRow("Produit ajouté", formatInstantDate(product.addedAt))
                        product.openedAt?.let {
                            TimelineRow(
                                "Produit ouvert",
                                formatInstantDate(it),
                                product.daysAfterOpening?.let { days -> "À consommer dans $days jour(s) après ouverture" }
                            )
                        }
                        product.frozenUntil?.let {
                            TimelineRow("Congélation active", it.toString())
                        }
                        product.statusChangedAt?.let { changedAt ->
                            val statusLabel = when (product.status) {
                                ProductStatus.ACTIVE -> "Produit remis actif"
                                ProductStatus.OPENED -> "Statut mis à ouvert"
                                ProductStatus.CONSUMED -> "Produit consommé"
                                ProductStatus.THROWN -> "Produit jeté"
                            }
                            TimelineRow(statusLabel, formatInstantDate(changedAt))
                        }
                    }
                }
            }
        }
    }

    // Dialog ouverture produit
    if (ui.showOpeningDialog && product != null) {
        OpeningDialog(
            product = product,
            onConfirm = { days -> vm.openProduct(days) },
            onDismiss = { vm.dismissOpeningDialog() }
        )
    }

    if (showDeleteConfirm && product != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer ce produit ?") },
            text = {
                Text(
                    "Cette action retirera ${product.name} du frigo. Elle est définitive."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        vm.deleteProduct { navController.popBackStack() }
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(0.4f), color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        Text(value, modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BarcodeRow(
    barcode: String?,
    copied: Boolean,
    onCopy: (String) -> Unit
) {
    if (barcode.isNullOrBlank()) return
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Code-barres",
            modifier = Modifier.weight(0.4f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            barcode,
            modifier = Modifier.weight(0.5f),
            style = MaterialTheme.typography.bodyMedium
        )
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
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!detail.isNullOrBlank()) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatInstantDate(instant: kotlinx.datetime.Instant): String =
    instant.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

private data class NutritionRow(val label: String, val value: String)

private fun parseNutritionRows(raw: String?): List<NutritionRow> {
    if (raw.isNullOrBlank()) return emptyList()
    val obj = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyList()
    val specs = listOf(
        Triple("energy_kcal", "Énergie", "kcal"),
        Triple("fat", "Matières grasses", "g"),
        Triple("saturated_fat", "dont saturées", "g"),
        Triple("carbohydrates", "Glucides", "g"),
        Triple("sugars", "dont sucres", "g"),
        Triple("fiber", "Fibres", "g"),
        Triple("proteins", "Protéines", "g"),
        Triple("salt", "Sel", "g")
    )
    return specs.mapNotNull { (key, label, unit) ->
        val value = obj[key]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        NutritionRow(label, "${formatNutritionValue(value)} $unit")
    }
}

private fun formatNutritionValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

// ── Score badges (M10) ───────────────────────────────────────────────────────

@Composable
private fun ScoreBadge(letter: String, bg: Color, fg: Color = Color.White) {
    Box(
        Modifier.size(26.dp).clip(RoundedCornerShape(4.dp)).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ScoreBadge(score.uppercase(), bg, fg)
        Text("Nutri-Score ${score.uppercase()}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NovaGroupBadge(group: Int) {
    val (bg, fg) = when (group) {
        1 -> Color(0xFF1B5E20) to Color.White
        2 -> Color(0xFF558B2F) to Color.White
        3 -> Color(0xFFE65100) to Color.White
        4 -> Color(0xFFB71C1C) to Color.White
        else -> return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ScoreBadge(group.toString(), bg, fg)
        Text("NOVA $group", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EcoScoreBadge(score: String) {
    val (bg, fg) = when (score.uppercase()) {
        "A" -> Color(0xFF1B5E20) to Color.White
        "B" -> Color(0xFF558B2F) to Color.White
        "C" -> Color(0xFFF9A825) to Color.Black
        "D" -> Color(0xFFE65100) to Color.White
        "E" -> Color(0xFFB71C1C) to Color.White
        else -> return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ScoreBadge(score.uppercase(), bg, fg)
        Text("Éco-Score ${score.uppercase()}", style = MaterialTheme.typography.bodySmall)
    }
}

// ── Allergen translation (M11) ───────────────────────────────────────────────

private val allergenTranslations = mapOf(
    "en:gluten" to "Gluten",
    "en:crustaceans" to "Crustacés",
    "en:eggs" to "Œufs",
    "en:fish" to "Poisson",
    "en:peanuts" to "Arachides",
    "en:soybeans" to "Soja",
    "en:milk" to "Lait",
    "en:nuts" to "Fruits à coque",
    "en:celery" to "Céleri",
    "en:mustard" to "Moutarde",
    "en:sesame" to "Sésame",
    "en:sulphites" to "Sulfites",
    "en:lupin" to "Lupin",
    "en:molluscs" to "Mollusques"
)

private fun translateAllergens(raw: String): String =
    raw.split(",")
        .map { it.trim().lowercase() }
        .joinToString(", ") { code ->
            allergenTranslations[code]
                ?: code.substringAfter(":").replaceFirstChar { it.uppercase() }
        }

@Composable
private fun OpeningDialog(
    product: com.freshtrack.domain.model.Product,
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
                Text(recommended.second, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { if (days > 1) days-- }) { Text("-") }
                    Text("$days jour(s)", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { days++ }) { Text("+") }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(days) }) { Text("Confirmer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

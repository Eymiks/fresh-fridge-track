package com.freshtrack.ui.screens.add

import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.freshtrack.domain.catalog.PRODUCT_CATEGORIES
import com.freshtrack.domain.catalog.matchCategory
import com.freshtrack.domain.format.formatDate
import com.freshtrack.domain.format.parseUserDate
import com.freshtrack.ui.navigation.Routes
import com.freshtrack.ui.components.FreshProductImage
import com.freshtrack.ui.theme.FreshTextStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private val ALLOWED_IMAGE_MIME = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    barcode: String,
    isMultiMode: Boolean = false,
    initialDate: String = "",
    vm: AddProductViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val isGuest by vm.isGuest.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pré-remplir la date détectée par le scanner (mode multi)
    LaunchedEffect(initialDate) {
        if (initialDate.isNotBlank()) vm.setExpirationDate(initialDate)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        if (isGuest) {
            vm.setImageUrl(uri.toString())
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType !in ALLOWED_IMAGE_MIME) {
                vm.setError("Format non supporté. Utilisez JPEG, PNG ou WebP.")
                return@launch
            }
            val ext = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?: "jpg"
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val b = stream.readBytes()
                    if (b.size > MAX_IMAGE_BYTES) null else b
                }
            }
            if (bytes == null) {
                vm.setError("Image trop volumineuse (max 5 Mo).")
                return@launch
            }
            vm.uploadImage(bytes, ext)
        }
    }

    // Récupérer la date scannée quand on revient du DateScannerScreen
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("detected_date", null)?.collect { date ->
            if (date != null) {
                vm.setExpirationDate(date)
                savedStateHandle.remove<String>("detected_date")
            }
        }
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var subcategoryExpanded by remember { mutableStateOf(false) }
    var conservationExpanded by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var showExpirationDatePicker by remember { mutableStateOf(false) }
    val categories = remember { PRODUCT_CATEGORIES.filter { it.key != "all" } }
    val resolvedCategoryKey = remember(ui.category, ui.name) {
        ui.category.ifBlank { matchCategory(productName = ui.name) }
    }
    val currentCategory = categories.firstOrNull { it.key == ui.category }
    val subcategoryOptions = remember(resolvedCategoryKey) {
        categories.firstOrNull { it.key == resolvedCategoryKey }?.subcategories.orEmpty()
    }
    val screenTitle = when {
        ui.isEditing -> "Modifier le produit"
        isMultiMode -> "Ajouter — mode chaîne"
        else -> "Ajouter un produit"
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Retour"
                        },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            screenTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (ui.isEditing) "Corrigez les informations utiles"
                            else if (isMultiMode) "Vérifiez puis scannez le suivant"
                            else "Renseignez l'essentiel, le reste peut attendre",
                            style = FreshTextStyles.FormHelper,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (!ui.isLoadingBarcode && !ui.isLoadingProduct) {
                AddProductBottomBar(
                    ui = ui,
                    isMultiMode = isMultiMode,
                    onSave = { vm.save { navController.popBackStack() } },
                    onSaveAndScanNext = {
                        vm.save {
                            navController.navigate(Routes.BARCODE_SCANNER_MULTI) {
                                popUpTo(Routes.BARCODE_SCANNER_MULTI) { inclusive = true }
                            }
                        }
                    },
                    onFinishMultiScan = {
                        navController.popBackStack(Routes.BARCODE_SCANNER_MULTI, true)
                    }
                )
            }
        }
    ) { innerPadding ->
        if (ui.isLoadingBarcode || ui.isLoadingProduct) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (ui.isLoadingProduct) "Chargement du produit…" else "Recherche du produit…",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactFormSection(title = "Essentiel") {
                OutlinedTextField(
                    value = ui.name,
                    onValueChange = vm::setName,
                    label = { Text("Nom du produit *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    isError = ui.error != null && ui.name.isBlank()
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ui.expirationDate,
                        onValueChange = vm::setExpirationDate,
                        label = { Text("Date de péremption *") },
                        placeholder = { Text("JJ/MM/AAAA") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        isError = ui.error?.contains("Date") == true
                    )
                    OutlinedButton(
                        onClick = { showExpirationDatePicker = true },
                        modifier = Modifier.clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Choisir la date de péremption"
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                    OutlinedButton(
                        onClick = { navController.navigate(Routes.dateScanner(ui.barcode.ifBlank { "_" })) },
                        modifier = Modifier.clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Scanner la date avec la caméra"
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currentCategory?.label ?: "Automatique",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Catégorie") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Automatique", style = FreshTextStyles.MenuItem) },
                                onClick = {
                                    vm.setCategory("")
                                    categoryExpanded = false
                                }
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.label, style = FreshTextStyles.MenuItem) },
                                    onClick = {
                                        vm.setCategory(cat.key)
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = subcategoryExpanded,
                        onExpandedChange = { subcategoryExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = ui.subcategory.ifBlank { "Automatique" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sous-cat.") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subcategoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = subcategoryExpanded,
                            onDismissRequest = { subcategoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Automatique", style = FreshTextStyles.MenuItem) },
                                onClick = {
                                    vm.setSubcategory("")
                                    subcategoryExpanded = false
                                }
                            )
                            subcategoryOptions.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.label, style = FreshTextStyles.MenuItem) },
                                    onClick = {
                                        vm.setSubcategory(sub.label)
                                        subcategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ui.quantity,
                        onValueChange = vm::setQuantity,
                        label = { Text("Quantité") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ui.brand,
                        onValueChange = vm::setBrand,
                        label = { Text("Marque") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ui.barcode,
                        onValueChange = vm::setBarcode,
                        label = { Text("Code-barres") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = { vm.lookupBarcode(ui.barcode) },
                        enabled = ui.barcode.isNotBlank(),
                        modifier = Modifier.clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Rechercher le code-barres"
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("OK", style = FreshTextStyles.ActionLabel)
                    }
                }
            }

            CompactFormSection(title = "Image") {
                if (ui.imageUrl.isNotBlank()) {
                    FreshProductImage(
                        imageUrl = ui.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = MaterialTheme.shapes.large,
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .clearAndSetSemantics {
                                role = Role.Button
                                contentDescription = if (ui.isUploadingImage) "Envoi de l'image en cours" else "Choisir une image du produit"
                            },
                        enabled = !ui.isUploadingImage,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Text(
                            if (ui.isUploadingImage) "Envoi…" else "Choisir une image",
                            style = FreshTextStyles.ActionLabel
                        )
                    }
                    if (ui.isUploadingImage) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
                OutlinedTextField(
                    value = ui.imageUrl,
                    onValueChange = vm::setImageUrl,
                    label = { Text("Image produit (URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
            }

            CompactFormSection(
                title = "Conservation",
                expanded = conservationExpanded,
                onToggle = { conservationExpanded = !conservationExpanded }
            ) {
                OutlinedTextField(
                    value = ui.frozenUntil,
                    onValueChange = vm::setFrozenUntil,
                    label = { Text("Congelé jusqu'au") },
                    placeholder = { Text("JJ/MM/AAAA") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    isError = ui.error?.contains("congélation", ignoreCase = true) == true
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Produit ouvert",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Utilise une durée après ouverture pour la date effective",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = ui.isOpened, onCheckedChange = vm::setOpened)
                }

                if (ui.isOpened) {
                    OutlinedTextField(
                        value = ui.daysAfterOpening,
                        onValueChange = vm::setDaysAfterOpening,
                        label = { Text("Jours après ouverture") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }
            }

            CompactFormSection(
                title = "Informations avancées",
                expanded = advancedExpanded,
                onToggle = { advancedExpanded = !advancedExpanded }
            ) {
                OutlinedTextField(
                    value = ui.notes,
                    onValueChange = vm::setNotes,
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = MaterialTheme.shapes.medium,
                    maxLines = 4
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ui.nutriScore,
                        onValueChange = vm::setNutriScore,
                        label = { Text("Nutri") },
                        placeholder = { Text("A-E") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ui.novaGroup,
                        onValueChange = vm::setNovaGroup,
                        label = { Text("NOVA") },
                        placeholder = { Text("1-4") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ui.ecoScore,
                        onValueChange = vm::setEcoScore,
                        label = { Text("Eco") },
                        placeholder = { Text("A-E") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = ui.allergens,
                    onValueChange = vm::setAllergens,
                    label = { Text("Allergènes") },
                    placeholder = { Text("Ex: Lait, gluten") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                OutlinedTextField(
                    value = ui.ingredients,
                    onValueChange = vm::setIngredients,
                    label = { Text("Ingrédients") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = MaterialTheme.shapes.medium,
                    maxLines = 4
                )

                OutlinedTextField(
                    value = ui.nutritionData,
                    onValueChange = vm::setNutritionData,
                    label = { Text("Données nutritionnelles JSON") },
                    placeholder = { Text("""{"energy_kcal":120,"proteins":4}""") },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    shape = MaterialTheme.shapes.medium,
                    maxLines = 3
                )
            }

            // Message d'erreur
            ui.error?.let { error ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.68f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (ui.showImageDialog && ui.offImages.isNotEmpty()) {
        OffImageSelectionDialog(
            images = ui.offImages,
            onSelect = { url -> vm.selectOffImage(url) },
            onDismiss = { vm.dismissImageDialog() }
        )
    }

    if (showExpirationDatePicker) {
        ProductDatePickerDialog(
            initialDate = ui.expirationDate,
            onDateSelected = vm::setExpirationDate,
            onDismiss = { showExpirationDatePicker = false }
        )
    }
}

@Composable
private fun CompactFormSection(
    title: String,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (title) {
                            "Image" -> Icons.Default.Image
                            "Conservation" -> Icons.Default.DateRange
                            "Informations avancées" -> Icons.Default.Info
                            else -> Icons.Default.CameraAlt
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    title,
                    style = FreshTextStyles.FormSectionTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (onToggle != null) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Replier" else "Déplier"
                    )
                }
            }
            if (expanded) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = remember(initialDate) { dateInputToEpochMillis(initialDate) }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(epochMillisToDateInput(millis))
                    }
                    onDismiss()
                }
            ) {
                Text("OK", style = FreshTextStyles.ActionLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", style = FreshTextStyles.ActionLabel)
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun AddProductBottomBar(
    ui: AddProductUiState,
    isMultiMode: Boolean,
    onSave: () -> Unit,
    onSaveAndScanNext: () -> Unit,
    onFinishMultiScan: () -> Unit
) {
    val canSave = !ui.isSaving && ui.name.isNotBlank() && ui.expirationDate.isNotBlank()

    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 6.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isMultiMode && !ui.isEditing) {
                Button(
                    onClick = onSaveAndScanNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Ajouter et scanner le produit suivant"
                        },
                    enabled = canSave,
                    shape = MaterialTheme.shapes.large
                ) {
                    SaveProgressLabel(
                        isSaving = ui.isSaving,
                        savingLabel = "Enregistrement…",
                        idleLabel = "Ajouter & scanner le suivant"
                    )
                }
                TextButton(
                    onClick = onFinishMultiScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Terminer le mode chaîne"
                        }
                ) {
                    Text("Terminer", style = FreshTextStyles.ActionLabel)
                }
            } else {
                val saveLabel = if (ui.isEditing) "Enregistrer les modifications" else "Ajouter au frigo"
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = saveLabel
                        },
                    enabled = canSave,
                    shape = MaterialTheme.shapes.large
                ) {
                    SaveProgressLabel(
                        isSaving = ui.isSaving,
                        savingLabel = "Enregistrement…",
                        idleLabel = saveLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveProgressLabel(
    isSaving: Boolean,
    savingLabel: String,
    idleLabel: String
) {
    if (isSaving) {
        CircularProgressIndicator(
            modifier = Modifier.padding(end = 8.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp
        )
    }
    Text(if (isSaving) savingLabel else idleLabel, style = FreshTextStyles.ButtonLabel)
}

@Composable
private fun OffImageSelectionDialog(
    images: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Choisir une image",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Sélectionnez l'image produit à utiliser.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    items(images, key = { it }, contentType = { "off_image" }) { url ->
                        FreshProductImage(
                            imageUrl = url,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onSelect(url) },
                            shape = RoundedCornerShape(12.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ignorer", style = FreshTextStyles.ActionLabel)
                }
            }
        }
    }
}

private fun dateInputToEpochMillis(value: String): Long? =
    parseUserDate(value)?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()

private fun epochMillisToDateInput(value: Long): String =
    formatDate(
        Instant.fromEpochMilliseconds(value)
            .toLocalDateTime(TimeZone.UTC)
            .date
    ).orEmpty()

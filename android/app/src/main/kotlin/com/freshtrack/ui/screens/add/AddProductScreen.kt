package com.freshtrack.ui.screens.add

import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.freshtrack.domain.catalog.PRODUCT_CATEGORIES
import com.freshtrack.domain.format.formatDate
import com.freshtrack.domain.format.parseUserDate
import com.freshtrack.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

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
            val ext = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?: "jpg"
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes != null) {
                vm.uploadImage(bytes, ext)
            }
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
    var showExpirationDatePicker by remember { mutableStateOf(false) }
    val categories = remember { PRODUCT_CATEGORIES.filter { it.key != "all" } }
    val currentCategory = categories.firstOrNull { it.key == ui.category }
    val screenTitle = when {
        ui.isEditing -> "Modifier le produit"
        isMultiMode -> "Ajouter — mode chaîne"
        else -> "Ajouter un produit"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (ui.isLoadingBarcode || ui.isLoadingProduct) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (ui.isLoadingProduct) "Chargement du produit…" else "Recherche du produit…",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
            // Nom (requis)
            OutlinedTextField(
                value = ui.name,
                onValueChange = vm::setName,
                label = { Text("Nom du produit *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = ui.error != null && ui.name.isBlank()
            )

            // Marque
            OutlinedTextField(
                value = ui.brand,
                onValueChange = vm::setBrand,
                label = { Text("Marque") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Catégorie
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentCategory?.label ?: ui.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.label) },
                            onClick = {
                                vm.setCategory(cat.key)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = ui.subcategory,
                onValueChange = vm::setSubcategory,
                label = { Text("Sous-catégorie") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Quantité
            OutlinedTextField(
                value = ui.quantity,
                onValueChange = vm::setQuantity,
                label = { Text("Quantité (ex: 500g, 1L)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                    singleLine = true
                )
                OutlinedButton(
                    onClick = { vm.lookupBarcode(ui.barcode) },
                    enabled = ui.barcode.isNotBlank()
                ) {
                    Text("OK")
                }
            }

            // Date de péremption
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
                    singleLine = true,
                    isError = ui.error?.contains("Date") == true
                )
                OutlinedButton(
                    onClick = { showExpirationDatePicker = true }
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Choisir une date")
                }
                OutlinedButton(
                    onClick = { navController.navigate(Routes.dateScanner(ui.barcode.ifBlank { "_" })) }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                }
            }

            OutlinedTextField(
                value = ui.frozenUntil,
                onValueChange = vm::setFrozenUntil,
                label = { Text("Congelé jusqu'au") },
                placeholder = { Text("JJ/MM/AAAA") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = ui.error?.contains("congélation", ignoreCase = true) == true
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Produit ouvert", style = MaterialTheme.typography.bodyLarge)
                    Text("Utilise une durée après ouverture pour la date effective",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    singleLine = true
                )
            }

            if (ui.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ui.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f),
                    enabled = !ui.isUploadingImage
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Text(if (ui.isUploadingImage) "Envoi…" else "Choisir une image")
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
                singleLine = true
            )

            OutlinedTextField(
                value = ui.notes,
                onValueChange = vm::setNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )

            Text(
                "Informations avancées",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ui.nutriScore,
                    onValueChange = vm::setNutriScore,
                    label = { Text("Nutri-Score") },
                    placeholder = { Text("A-E") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ui.novaGroup,
                    onValueChange = vm::setNovaGroup,
                    label = { Text("NOVA") },
                    placeholder = { Text("1-4") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ui.ecoScore,
                    onValueChange = vm::setEcoScore,
                    label = { Text("Eco-Score") },
                    placeholder = { Text("A-E") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = ui.allergens,
                onValueChange = vm::setAllergens,
                label = { Text("Allergènes") },
                placeholder = { Text("Ex: Lait, gluten") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = ui.ingredients,
                onValueChange = vm::setIngredients,
                label = { Text("Ingrédients") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )

            OutlinedTextField(
                value = ui.nutritionData,
                onValueChange = vm::setNutritionData,
                label = { Text("Données nutritionnelles JSON") },
                placeholder = { Text("""{"energy_kcal":120,"proteins":4}""") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            // Message d'erreur
            ui.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))

            if (isMultiMode && !ui.isEditing) {
                // Bouton "Ajouter & scanner le suivant"
                Button(
                    onClick = {
                        vm.save {
                            // Réinitialiser en naviguant vers un BARCODE_SCANNER_MULTI frais
                            navController.navigate(Routes.BARCODE_SCANNER_MULTI) {
                                popUpTo(Routes.BARCODE_SCANNER_MULTI) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !ui.isSaving && ui.name.isNotBlank() && ui.expirationDate.isNotBlank()
                ) {
                    if (ui.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(if (ui.isSaving) "Enregistrement…" else "Ajouter & scanner le suivant")
                }
                // Bouton "Terminer" (quitter le mode multi-scan)
                TextButton(
                    onClick = {
                        navController.popBackStack(Routes.BARCODE_SCANNER_MULTI, true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Terminer")
                }
            } else {
                // Bouton sauvegarder classique
                Button(
                    onClick = {
                        vm.save { navController.popBackStack() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !ui.isSaving && ui.name.isNotBlank() && ui.expirationDate.isNotBlank()
                ) {
                    if (ui.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        when {
                            ui.isSaving -> "Enregistrement…"
                            ui.isEditing -> "Enregistrer les modifications"
                            else -> "Ajouter au frigo"
                        }
                    )
                }
            }
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
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun OffImageSelectionDialog(
    images: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Choisir une image",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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
                    items(images) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(url) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ignorer")
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

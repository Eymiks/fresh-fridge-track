package com.freshtrack.ui.screens.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.openfoodfacts.OffApi
import com.freshtrack.data.openfoodfacts.OffResult
import com.freshtrack.data.products.ProductRepository
import com.freshtrack.domain.catalog.matchCategory
import com.freshtrack.domain.catalog.matchSubcategory
import com.freshtrack.domain.format.formatDate
import com.freshtrack.domain.format.normalizeDateInput
import com.freshtrack.domain.format.parseUserDate
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

data class AddProductUiState(
    val isEditing: Boolean = false,
    val isLoadingProduct: Boolean = false,
    val isLoadingBarcode: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false,
    // Form fields
    val name: String = "",
    val brand: String = "",
    val category: String = "",
    val subcategory: String = "",
    val quantity: String = "",
    val expirationDate: String = "",
    val frozenUntil: String = "",
    val imageUrl: String = "",
    val nutriScore: String = "",
    val novaGroup: String = "",
    val ecoScore: String = "",
    val barcode: String = "",
    val notes: String = "",
    val allergens: String = "",
    val ingredients: String = "",
    val nutritionData: String = "",
    val isOpened: Boolean = false,
    val daysAfterOpening: String = "3",
    val offImages: List<String> = emptyList(),
    val showImageDialog: Boolean = false
)

@HiltViewModel
class AddProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val offApi: OffApi,
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val initialBarcode: String = savedStateHandle["barcode"] ?: ""
    private val productId: String = savedStateHandle["productId"] ?: ""
    private var editingProduct: Product? = null

    private val _ui = MutableStateFlow(AddProductUiState(barcode = initialBarcode))
    val ui = _ui.asStateFlow()

    private val authState = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    val isGuest = authState
        .map { it is AuthState.Guest }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        if (productId.isNotBlank()) {
            _ui.update { it.copy(isEditing = true, isLoadingProduct = true) }
            loadProductForEdit()
        } else if (initialBarcode.isNotBlank()) {
            lookupBarcode(initialBarcode)
        }
    }

    private fun loadProductForEdit() = viewModelScope.launch {
        authState.flatMapLatest { state ->
            when (state) {
                is AuthState.Authenticated -> {
                    val hId = state.household?.id ?: return@flatMapLatest flowOf(null)
                    productRepository.observeProduct(hId, productId, state.members)
                }
                is AuthState.Guest -> productRepository.observeGuestProduct(productId)
                else -> flowOf(null)
            }
        }.map { product ->
            if (product == null) {
                _ui.update { it.copy(isLoadingProduct = false, error = "Produit introuvable") }
                return@map
            }
            if (editingProduct?.id == product.id) return@map
            editingProduct = product
            _ui.update {
                it.copy(
                    isLoadingProduct = false,
                    name = product.name,
                    brand = product.brand.orEmpty(),
                    category = product.category.orEmpty(),
                    subcategory = product.subcategory.orEmpty(),
                    quantity = product.quantity.orEmpty(),
                    expirationDate = formatDate(product.expirationDate).orEmpty(),
                    frozenUntil = formatDate(product.frozenUntil).orEmpty(),
                    imageUrl = product.imageUrl.orEmpty(),
                    nutriScore = product.nutriScore.orEmpty(),
                    novaGroup = product.novaGroup?.toString().orEmpty(),
                    ecoScore = product.ecoScore.orEmpty(),
                    barcode = product.barcode.orEmpty(),
                    notes = product.notes.orEmpty(),
                    allergens = product.allergens.orEmpty(),
                    ingredients = product.ingredients.orEmpty(),
                    nutritionData = product.nutritionData.orEmpty(),
                    isOpened = product.status == ProductStatus.OPENED,
                    daysAfterOpening = product.daysAfterOpening?.toString() ?: "3",
                    error = null
                )
            }
        }.collect {}
    }

    fun lookupBarcode(barcode: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoadingBarcode = true, barcode = barcode) }
        val result: OffResult? = offApi.fetchByBarcodeRaw(barcode)
        if (result != null) {
            val product = result.product
            val fallbackImage = if (result.availableImages.isEmpty()) {
                val householdId = (authState.value as? AuthState.Authenticated)?.household?.id
                householdId?.let { productRepository.findExistingImageForBarcode(it, barcode) }
            } else null
            _ui.update {
                it.copy(
                    isLoadingBarcode = false,
                    name = product.name,
                    brand = product.brand ?: it.brand,
                    category = if (it.category.isBlank()) product.category.orEmpty() else it.category,
                    subcategory = if (it.subcategory.isBlank()) product.subcategory.orEmpty() else it.subcategory,
                    imageUrl = fallbackImage ?: it.imageUrl,
                    quantity = product.quantity ?: it.quantity,
                    nutriScore = product.nutriScore ?: it.nutriScore,
                    novaGroup = product.novaGroup?.toString() ?: it.novaGroup,
                    ecoScore = product.ecoScore ?: it.ecoScore,
                    allergens = product.allergens ?: it.allergens,
                    ingredients = product.ingredients ?: it.ingredients,
                    nutritionData = product.nutritionData ?: it.nutritionData,
                    offImages = result.availableImages,
                    showImageDialog = result.availableImages.isNotEmpty()
                )
            }
        } else {
            _ui.update { it.copy(isLoadingBarcode = false) }
        }
    }

    fun selectOffImage(url: String) = _ui.update { it.copy(imageUrl = url, showImageDialog = false) }
    fun dismissImageDialog() = _ui.update { it.copy(showImageDialog = false) }

    fun setExpirationDate(date: String) = _ui.update { it.copy(expirationDate = normalizeDateInput(date)) }
    fun setName(v: String) = _ui.update { it.copy(name = v) }
    fun setBrand(v: String) = _ui.update { it.copy(brand = v) }
    fun setCategory(v: String) = _ui.update {
        if (it.category == v) it else it.copy(category = v, subcategory = "")
    }
    fun setSubcategory(v: String) = _ui.update { it.copy(subcategory = v) }
    fun setQuantity(v: String) = _ui.update { it.copy(quantity = v) }
    fun setBarcode(v: String) = _ui.update { it.copy(barcode = v) }
    fun setImageUrl(v: String) = _ui.update { it.copy(imageUrl = v) }
    fun setNotes(v: String) = _ui.update { it.copy(notes = v) }
    fun setFrozenUntil(v: String) = _ui.update { it.copy(frozenUntil = normalizeDateInput(v)) }
    fun setOpened(v: Boolean) = _ui.update { it.copy(isOpened = v) }
    fun setDaysAfterOpening(v: String) = _ui.update { it.copy(daysAfterOpening = v) }
    fun setNutriScore(v: String) = _ui.update { it.copy(nutriScore = v.uppercase().take(1)) }
    fun setNovaGroup(v: String) = _ui.update { it.copy(novaGroup = v.filter { ch -> ch.isDigit() }.take(1)) }
    fun setEcoScore(v: String) = _ui.update { it.copy(ecoScore = v.uppercase().take(1)) }
    fun setAllergens(v: String) = _ui.update { it.copy(allergens = v) }
    fun setIngredients(v: String) = _ui.update { it.copy(ingredients = v) }
    fun setNutritionData(v: String) = _ui.update { it.copy(nutritionData = v) }
    fun setError(msg: String?) = _ui.update { it.copy(error = msg) }

    fun uploadImage(bytes: ByteArray, ext: String) = viewModelScope.launch {
        val auth = authState.value as? AuthState.Authenticated ?: run {
            _ui.update { it.copy(error = "Connectez-vous pour téléverser une image.") }
            return@launch
        }
        val householdId = auth.household?.id ?: run {
            _ui.update { it.copy(error = "Pas de foyer") }
            return@launch
        }
        val productPathId = editingProduct?.id?.takeIf { it.isNotBlank() } ?: "upload"
        _ui.update { it.copy(isUploadingImage = true, error = null) }
        runCatching { productRepository.uploadImage(householdId, productPathId, bytes, ext) }
            .onSuccess { url -> _ui.update { it.copy(isUploadingImage = false, imageUrl = url) } }
            .onFailure { e -> _ui.update { it.copy(isUploadingImage = false, error = e.message) } }
    }

    fun save(onSaved: () -> Unit) = viewModelScope.launch {
        val state = _ui.value

        val expDate = parseUserDate(state.expirationDate) ?: run {
            _ui.update { it.copy(error = "Date invalide (format : JJ/MM/AAAA)") }; return@launch
        }

        _ui.update { it.copy(isSaving = true, error = null) }

        val existing = editingProduct
        val frozenUntil = state.frozenUntil.takeIf { it.isNotBlank() }?.let {
            parseUserDate(it) ?: run {
                _ui.update { current -> current.copy(error = "Date de congélation invalide (format : JJ/MM/AAAA)") }
                return@launch
            }
        }
        val daysAfterOpening = if (state.isOpened) {
            state.daysAfterOpening.toIntOrNull()?.takeIf { it > 0 } ?: run {
                _ui.update { it.copy(error = "Durée après ouverture invalide") }
                return@launch
            }
        } else {
            null
        }
        val status = when {
            state.isOpened -> ProductStatus.OPENED
            existing?.status == ProductStatus.CONSUMED -> ProductStatus.CONSUMED
            existing?.status == ProductStatus.THROWN -> ProductStatus.THROWN
            else -> ProductStatus.ACTIVE
        }
        val finalCategory = state.category.takeIf { it.isNotBlank() }
            ?: matchCategory(productName = state.name.trim())
        val finalSubcategory = state.subcategory.takeIf { it.isNotBlank() }
            ?: matchSubcategory(finalCategory, productName = state.name.trim())

        val product = Product(
            id = existing?.id.orEmpty(),
            name = state.name.trim(),
            barcode = state.barcode.takeIf { it.isNotBlank() },
            expirationDate = expDate,
            addedAt = existing?.addedAt ?: Clock.System.now(),
            brand = state.brand.takeIf { it.isNotBlank() },
            category = finalCategory,
            subcategory = finalSubcategory,
            quantity = state.quantity.takeIf { it.isNotBlank() },
            imageUrl = state.imageUrl.takeIf { it.isNotBlank() },
            nutriScore = state.nutriScore.takeIf { it.isNotBlank() },
            novaGroup = state.novaGroup.toIntOrNull(),
            ecoScore = state.ecoScore.takeIf { it.isNotBlank() },
            status = status,
            statusChangedAt = if (existing?.status != status) Clock.System.now() else existing?.statusChangedAt,
            openedAt = if (state.isOpened) existing?.openedAt ?: Clock.System.now() else null,
            daysAfterOpening = daysAfterOpening,
            addedBy = existing?.addedBy,
            notes = state.notes.takeIf { it.isNotBlank() },
            frozenUntil = frozenUntil,
            allergens = state.allergens.takeIf { it.isNotBlank() },
            ingredients = state.ingredients.takeIf { it.isNotBlank() },
            nutritionData = state.nutritionData.takeIf { it.isNotBlank() }
        )

        val saveResult = when (val auth = authState.value) {
            is AuthState.Guest -> runCatching {
                if (existing == null) productRepository.addGuestProduct(product)
                else {
                    productRepository.updateGuestProduct(product)
                    product
                }
            }
            is AuthState.Authenticated -> {
                val householdId = auth.household?.id ?: run {
                    _ui.update { it.copy(isSaving = false, error = "Pas de foyer") }
                    return@launch
                }
                val userId = authRepository.currentUserId() ?: run {
                    _ui.update { it.copy(isSaving = false, error = "Utilisateur introuvable") }
                    return@launch
                }
                runCatching {
                    if (existing == null) productRepository.addProduct(product, householdId, userId)
                    else {
                        productRepository.updateProduct(product, householdId)
                        product
                    }
                }
            }
            else -> {
                _ui.update { it.copy(isSaving = false, error = "Non connecté") }
                return@launch
            }
        }

        saveResult
            .onSuccess { _ui.update { it.copy(isSaving = false, savedSuccessfully = true) }; onSaved() }
            .onFailure { e -> _ui.update { it.copy(isSaving = false, error = e.message) } }
    }
}

package com.freshtrack.ui.screens.scanner

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.data.ocr.OcrEdgeFunction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class DateScannerUiState(
    val isCloudLoading: Boolean = false,
    val cloudError: String? = null,
    val cooldownSeconds: Int = 0
)

@HiltViewModel
class DateScannerViewModel @Inject constructor(
    private val ocrEdgeFunction: OcrEdgeFunction
) : ViewModel() {

    private val _ui = MutableStateFlow(DateScannerUiState())
    val ui = _ui.asStateFlow()

    private var cooldownJob: Job? = null

    val canCallCloud: Boolean
        get() = _ui.value.cooldownSeconds == 0 && !_ui.value.isCloudLoading

    fun callCloudOcr(bitmap: Bitmap, onDateDetected: (String) -> Unit) {
        if (!canCallCloud) return
        viewModelScope.launch {
            _ui.update { it.copy(isCloudLoading = true, cloudError = null) }

            val bytes = ByteArrayOutputStream().also {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
            }.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
            val dataUrl = "data:image/jpeg;base64,$base64"

            val date = ocrEdgeFunction.parseDate(dataUrl)
            _ui.update { it.copy(isCloudLoading = false) }

            if (date != null) {
                onDateDetected(date)
            } else {
                _ui.update { it.copy(cloudError = "Date non détectée par l'OCR cloud") }
            }
            startCooldown()
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var remaining = 30
            while (remaining > 0) {
                _ui.update { it.copy(cooldownSeconds = remaining) }
                delay(1000)
                remaining--
            }
            _ui.update { it.copy(cooldownSeconds = 0, cloudError = null) }
        }
    }
}

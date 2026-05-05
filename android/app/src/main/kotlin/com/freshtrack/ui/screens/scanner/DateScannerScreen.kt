package com.freshtrack.ui.screens.scanner

import android.Manifest
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.freshtrack.domain.format.normalizeDateInput
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.text.Normalizer
import java.util.concurrent.TimeUnit

// ─── date parsing (port de src/lib/dateOcr.ts) ───────────────────────────────

private val MONTH_WORDS = mapOf(
    "JANVIER" to 1, "JANV" to 1, "JAN" to 1, "JANUARY" to 1,
    "FEVRIER" to 2, "FEVR" to 2, "FEV" to 2, "FEBRUARY" to 2, "FEB" to 2,
    "MARS" to 3, "MAR" to 3, "MARCH" to 3,
    "AVRIL" to 4, "AVR" to 4, "APRIL" to 4, "APR" to 4,
    "MAI" to 5, "MAY" to 5,
    "JUIN" to 6, "JUN" to 6, "JUNE" to 6,
    "JUILLET" to 7, "JUIL" to 7, "JULY" to 7, "JUL" to 7,
    "AOUT" to 8, "AOU" to 8, "AUGUST" to 8, "AUG" to 8,
    "SEPTEMBRE" to 9, "SEPT" to 9, "SEP" to 9, "SEPTEMBER" to 9,
    "OCTOBRE" to 10, "OCT" to 10, "OCTOBER" to 10,
    "NOVEMBRE" to 11, "NOV" to 11, "NOVEMBER" to 11,
    "DECEMBRE" to 12, "DEC" to 12, "DECEMBER" to 12
)

private fun resolveYear(raw: String): Int? {
    val y = raw.toIntOrNull() ?: return null
    return if (y < 100) (if (y <= 79) 2000 + y else 1900 + y) else y
}

private fun lastDayOfMonth(year: Int, month: Int) = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 30
}

private fun isValidDate(year: Int, month: Int, day: Int): Boolean {
    if (month !in 1..12) return false
    if (day !in 1..lastDayOfMonth(year, month)) return false
    val currentYear = Clock.System.todayIn(TimeZone.currentSystemDefault()).year
    return year in (currentYear - 2)..(currentYear + 10)
}

private fun correctDigits(s: String) = s
    .replace(Regex("[OoQ]"), "0")
    .replace(Regex("[Il|]"), "1")
    .replace("B", "8")

private fun stripAccents(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

fun parseExpirationDate(rawText: String): String? {
    val normalized = stripAccents(rawText).uppercase()
        .replace("'", " ").replace("’", " ")
        .replace(Regex("\\s+"), " ").trim()

    // Apply digit corrections only to segments that look like numbers/OCR artifacts
    val corrected = normalized.split(Regex("(?<=[^A-Z0-9/.\\-\\s])|(?=[^A-Z0-9/.\\-\\s])"))
        .joinToString("") { part ->
            if (part.any { it.isDigit() || it in "OoQIlB" }) correctDigits(part) else part
        }

    val candidates = mutableListOf<String>()

    fun addCandidate(year: Int, month: Int, day: Int) {
        if (isValidDate(year, month, day)) {
            candidates.add("%04d-%02d-%02d".format(year, month, day))
        }
    }

    fun addMonthOnly(year: Int, month: Int) {
        if (month in 1..12) addCandidate(year, month, lastDayOfMonth(year, month))
    }

    val monthAlt = MONTH_WORDS.keys.sortedByDescending { it.length }.joinToString("|")

    for (text in listOf(normalized, corrected)) {
        // DD/MM/YYYY, DD.MM.YYYY, DD-MM-YYYY, DD MM YYYY
        Regex("""(\d{1,2})[/.\- ](\d{1,2})[/.\- ](\d{2,4})""").findAll(text).forEach { m ->
            val d = m.groupValues[1].toIntOrNull() ?: return@forEach
            val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
            val y = resolveYear(m.groupValues[3]) ?: return@forEach
            if (d in 1..31 && mo in 1..12) addCandidate(y, mo, d)
        }
        // YYYY/MM/DD
        Regex("""(\d{4})[/.\- ](\d{1,2})[/.\- ](\d{1,2})""").findAll(text).forEach { m ->
            val y = m.groupValues[1].toIntOrNull() ?: return@forEach
            val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
            val d = m.groupValues[3].toIntOrNull() ?: return@forEach
            addCandidate(y, mo, d)
        }
        // YYYYMMDD (compact)
        Regex("""\b(\d{4})(\d{2})(\d{2})\b""").findAll(text).forEach { m ->
            val y = m.groupValues[1].toIntOrNull() ?: return@forEach
            val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
            val d = m.groupValues[3].toIntOrNull() ?: return@forEach
            addCandidate(y, mo, d)
        }
        // MM/YYYY
        Regex("""\b(\d{1,2})[/.\-](\d{4})\b""").findAll(text).forEach { m ->
            val mo = m.groupValues[1].toIntOrNull() ?: return@forEach
            val y = m.groupValues[2].toIntOrNull() ?: return@forEach
            addMonthOnly(y, mo)
        }
        // YYYY/MM
        Regex("""\b(\d{4})[/.\-](\d{1,2})\b""").findAll(text).forEach { m ->
            val y = m.groupValues[1].toIntOrNull() ?: return@forEach
            val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
            addMonthOnly(y, mo)
        }
        // "15 JANVIER 2024" or "15JANVIER2024"
        Regex("""(\d{1,2})\s*($monthAlt)\s*(\d{2,4})""").findAll(text).forEach { m ->
            val d = m.groupValues[1].toIntOrNull() ?: return@forEach
            val mo = MONTH_WORDS[m.groupValues[2]] ?: return@forEach
            val y = resolveYear(m.groupValues[3]) ?: return@forEach
            addCandidate(y, mo, d)
        }
        // "JANVIER 2024" (month only)
        Regex("""($monthAlt)\s*(\d{2,4})""").findAll(text).forEach { m ->
            val mo = MONTH_WORDS[m.groupValues[1]] ?: return@forEach
            val y = resolveYear(m.groupValues[2]) ?: return@forEach
            addMonthOnly(y, mo)
        }
        // EXP/DLC/DDM/BB prefix: "EXP 250125" or "BB 012025"
        Regex("""\b(?:EXP|DLC|DDM|BB|BEST\s*BEFORE|USE\s*BY)[:\s]*(\d{2})(\d{2})(\d{2,4})\b""")
            .findAll(text).forEach { m ->
                val d = m.groupValues[1].toIntOrNull() ?: return@forEach
                val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
                val y = resolveYear(m.groupValues[3]) ?: return@forEach
                addCandidate(y, mo, d)
            }
    }

    // Prefer the earliest valid date (soonest expiration)
    return candidates.distinct().sorted().firstOrNull()
}

// ─── composables ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScannerScreen(
    navController: NavController,
    barcode: String,
    vm: DateScannerViewModel = hiltViewModel()
) {
    var hasPermission by remember { mutableStateOf(false) }
    var detectedDate by remember { mutableStateOf<String?>(null) }
    var dateAccepted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ui by vm.ui.collectAsState()

    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    fun acceptDate(date: String) {
        if (!dateAccepted) {
            dateAccepted = true
            navController.previousBackStackEntry?.savedStateHandle?.set("detected_date", normalizeDateInput(date))
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner la date") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (hasPermission) {
                DateCameraPreview(
                    onDateDetected = { date -> if (!dateAccepted) detectedDate = date },
                    onImageCaptureBound = { ic -> imageCaptureRef.value = ic }
                )

                // Date détectée localement → bouton "Utiliser"
                detectedDate?.let { date ->
                    Button(
                        onClick = { acceptDate(date) },
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    ) { Text("Utiliser : ${normalizeDateInput(date)}") }
                }

                if (detectedDate == null) {
                    Text(
                        "Pointez vers la date de péremption",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Bouton Cloud OCR (fallback Gemini)
                val fabLabel = when {
                    ui.isCloudLoading -> "Analyse en cours…"
                    ui.cooldownSeconds > 0 -> "Attendre ${ui.cooldownSeconds}s"
                    else -> "OCR cloud"
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        if (vm.canCallCloud) {
                            imageCaptureRef.value?.let { ic ->
                                val executor = ContextCompat.getMainExecutor(context)
                                ic.takePicture(executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = image.toBitmap()
                                            image.close()
                                            vm.callCloudOcr(bitmap) { date -> acceptDate(date) }
                                        }
                                        override fun onError(e: ImageCaptureException) {
                                            Log.e("DateScan", "Capture failed", e)
                                        }
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    icon = {
                        if (ui.isCloudLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        } else {
                            Icon(Icons.Default.Cloud, null)
                        }
                    },
                    text = { Text(fabLabel) },
                    containerColor = if (vm.canCallCloud)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )

                ui.cloudError?.let { err ->
                    Text(
                        err,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Card(Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp)) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Permission caméra requise", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Autorisez la caméra pour lire une date, ou revenez au formulaire pour la saisir à la main.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Autoriser la caméra") }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Saisir la date manuellement") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCameraPreview(
    onDateDetected: (String) -> Unit,
    onImageCaptureBound: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    val imageCaptureUseCase = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    DisposableEffect(Unit) { onDispose { recognizer.close() } }

    LaunchedEffect(imageCaptureUseCase) { onImageCaptureBound(imageCaptureUseCase) }

    // Autofocus continu toutes les 2,5 s
    LaunchedEffect(cameraRef.value) {
        val cam = cameraRef.value ?: return@LaunchedEffect
        while (true) {
            val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
            val point = factory.createPoint(0.5f, 0.5f)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(2, TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
            delay(2500)
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                @Suppress("DEPRECATION")
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(1280, 720))
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    processDateOcr(imageProxy, recognizer, onDateDetected)
                }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraRef.value = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, analysis, imageCaptureUseCase
                    )
                }.onFailure { Log.e("DateScan", "Bind failed", it) }
            }, executor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun processDateOcr(
    imageProxy: ImageProxy,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    onDateDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                parseExpirationDate(visionText.text)?.let { onDateDetected(it) }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

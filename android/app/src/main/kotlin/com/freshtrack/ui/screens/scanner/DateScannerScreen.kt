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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.freshtrack.domain.format.normalizeDateInput
import com.freshtrack.domain.ocr.parseExpirationDate
import com.freshtrack.ui.navigation.Routes
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// ─── composables ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScannerScreen(
    navController: NavController,
    barcode: String,
    isMultiScan: Boolean = false,
    isAddFlow: Boolean = false,
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
            val normalized = normalizeDateInput(date)
            if (isMultiScan) {
                // Mode multi : naviguer en avant vers le formulaire avec barcode + date
                navController.navigate(Routes.addProductMulti(barcode, normalized)) {
                    popUpTo(Routes.DATE_SCANNER_MULTI) { inclusive = true }
                }
            } else if (isAddFlow) {
                navController.navigate(Routes.addProductWithDate(barcode, normalized)) {
                    popUpTo(Routes.DATE_SCANNER_ADD) { inclusive = true }
                }
            } else {
                navController.previousBackStackEntry?.savedStateHandle?.set("detected_date", normalized)
                navController.popBackStack()
            }
        }
    }

    fun openManualEntry() {
        if (dateAccepted) return
        dateAccepted = true
        if (isMultiScan) {
            navController.navigate(Routes.addProductMulti(barcode, "")) {
                popUpTo(Routes.DATE_SCANNER_MULTI) { inclusive = true }
            }
        } else if (isAddFlow) {
            navController.navigate(Routes.addProduct(barcode)) {
                popUpTo(Routes.DATE_SCANNER_ADD) { inclusive = true }
            }
        } else {
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
                    Button(
                        onClick = { openManualEntry() },
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                    ) {
                        Text("Saisir manuellement")
                    }
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
                            onClick = { openManualEntry() },
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
    val imageCaptureUseCase = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val scope = rememberCoroutineScope()
    val holder = remember { object { var camera: Camera? = null; var previewView: PreviewView? = null; var job: Job? = null } }
    var focusRingOffset by remember { mutableStateOf<Offset?>(null) }

    DisposableEffect(Unit) { onDispose { recognizer.close() } }

    LaunchedEffect(imageCaptureUseCase) { onImageCaptureBound(imageCaptureUseCase) }

    // Efface l'anneau de mise au point après 700 ms
    LaunchedEffect(focusRingOffset) {
        if (focusRingOffset != null) { delay(700); focusRingOffset = null }
    }

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures { offset ->
                val cam = holder.camera ?: return@detectTapGestures
                val pv = holder.previewView ?: return@detectTapGestures
                focusRingOffset = offset
                holder.job?.cancel()
                val point = pv.meteringPointFactory.createPoint(offset.x, offset.y)
                val action = FocusMeteringAction.Builder(
                    point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                ).setAutoCancelDuration(2, TimeUnit.SECONDS).build()
                cam.cameraControl.startFocusAndMetering(action)
                // Relancer la boucle auto-focus après 3 s
                holder.job = scope.launch {
                    delay(3000)
                    while (true) {
                        val w = pv.width.toFloat(); val h = pv.height.toFloat()
                        if (w > 0f && h > 0f) {
                            val p = pv.meteringPointFactory.createPoint(w / 2f, h / 2f)
                            val a = FocusMeteringAction.Builder(
                                p, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            ).setAutoCancelDuration(2, TimeUnit.SECONDS).build()
                            cam.cameraControl.startFocusAndMetering(a)
                        }
                        delay(2000)
                    }
                }
            }
        }
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                holder.previewView = previewView
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
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                            preview, analysis, imageCaptureUseCase
                        )
                        holder.camera = cam
                        holder.job?.cancel()
                        holder.job = scope.launch {
                            while (true) {
                                val w = previewView.width.toFloat(); val h = previewView.height.toFloat()
                                if (w > 0f && h > 0f) {
                                    val point = previewView.meteringPointFactory.createPoint(w / 2f, h / 2f)
                                    val action = FocusMeteringAction.Builder(
                                        point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                    ).setAutoCancelDuration(2, TimeUnit.SECONDS).build()
                                    cam.cameraControl.startFocusAndMetering(action)
                                }
                                delay(2000)
                            }
                        }
                    }.onFailure { Log.e("DateScan", "Bind failed", it) }
                }, executor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Anneau de mise au point (tap-to-focus)
        focusRingOffset?.let { pos ->
            Box(
                Modifier
                    .offset { IntOffset(
                        (pos.x - 30.dp.toPx()).roundToInt(),
                        (pos.y - 30.dp.toPx()).roundToInt()
                    )}
                    .size(60.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            )
        }
    }
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

package com.freshtrack.ui.screens.scanner

import android.Manifest
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.freshtrack.ui.navigation.Routes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(navController: NavController, isMultiScan: Boolean = false) {
    var hasPermission by remember { mutableStateOf(false) }
    var barcodeDetected by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val selfRoute = if (isMultiScan) Routes.BARCODE_SCANNER_MULTI else Routes.BARCODE_SCANNER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMultiScan) "Scanner — produit suivant" else "Scanner un code-barres") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (hasPermission) {
                BarcodeCameraPreview(
                    onBarcodeDetected = { barcode ->
                        if (!barcodeDetected) {
                            barcodeDetected = true
                            if (isMultiScan) {
                                // Mode multi : aller au scanner de date sans popper le scanner barcode
                                navController.navigate(Routes.dateScannerMulti(barcode))
                            } else {
                                navController.navigate(Routes.dateScannerAdd(barcode)) {
                                    popUpTo(selfRoute) { inclusive = true }
                                }
                            }
                        }
                    }
                )
                ScannerActions(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    prompt = if (isMultiScan) "Pointez vers le code-barres du produit" else "Pointez vers un code-barres",
                    detail = "Le scan démarre automatiquement dès qu'un code est lisible.",
                    promptColor = MaterialTheme.colorScheme.onSurface,
                    onManualClick = {
                        if (isMultiScan) {
                            navController.navigate(Routes.addProductMulti("", ""))
                        } else {
                            navController.navigate(Routes.addProduct()) {
                                popUpTo(selfRoute) { inclusive = true }
                            }
                        }
                    },
                    onPermissionClick = null
                )
            } else {
                ScannerActions(
                    modifier = Modifier.align(Alignment.Center),
                    prompt = "Permission caméra requise",
                    detail = "Autorisez la caméra pour scanner un code-barres, ou ajoutez le produit à la main.",
                    promptColor = MaterialTheme.colorScheme.onSurface,
                    onManualClick = {
                        if (isMultiScan) {
                            navController.navigate(Routes.addProductMulti("", ""))
                        } else {
                            navController.navigate(Routes.addProduct()) {
                                popUpTo(selfRoute) { inclusive = true }
                            }
                        }
                    },
                    onPermissionClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
    }
}

@Composable
private fun ScannerActions(
    modifier: Modifier,
    prompt: String,
    detail: String,
    promptColor: Color,
    onManualClick: () -> Unit,
    onPermissionClick: (() -> Unit)?
) {
    Card(modifier = modifier.fillMaxWidth().padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = prompt, color = promptColor, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            onPermissionClick?.let {
                Button(onClick = it, modifier = Modifier.fillMaxWidth()) {
                    Text("Autoriser la caméra")
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = onManualClick, modifier = Modifier.fillMaxWidth()) {
                Text("Saisir manuellement")
            }
        }
    }
}

@Composable
private fun BarcodeCameraPreview(onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember { BarcodeScanning.getClient() }
    val scope = rememberCoroutineScope()
    val holder = remember { object { var camera: Camera? = null; var previewView: PreviewView? = null; var job: Job? = null } }
    var focusRingOffset by remember { mutableStateOf<Offset?>(null) }

    DisposableEffect(Unit) { onDispose { scanner.close() } }

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
                        processBarcode(imageProxy, scanner, onBarcodeDetected)
                    }

                    runCatching {
                        cameraProvider.unbindAll()
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
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
                    }.onFailure { Log.e("BarcodeScan", "Bind failed", it) }
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

private fun processBarcode(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let { onDetected(it) }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

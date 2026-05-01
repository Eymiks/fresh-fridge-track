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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(navController: NavController) {
    var hasPermission by remember { mutableStateOf(false) }
    var barcodeDetected by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner un code-barres") },
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
                            navController.navigate(Routes.addProduct(barcode)) {
                                popUpTo(Routes.BARCODE_SCANNER) { inclusive = true }
                            }
                        }
                    }
                )
                ScannerActions(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    prompt = "Pointez vers un code-barres",
                    promptColor = Color.White,
                    onManualClick = {
                        navController.navigate(Routes.addProduct()) {
                            popUpTo(Routes.BARCODE_SCANNER) { inclusive = true }
                        }
                    }
                )
            } else {
                ScannerActions(
                    modifier = Modifier.align(Alignment.Center),
                    prompt = "Permission caméra requise",
                    promptColor = MaterialTheme.colorScheme.onSurface,
                    onManualClick = {
                        navController.navigate(Routes.addProduct()) {
                            popUpTo(Routes.BARCODE_SCANNER) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ScannerActions(
    modifier: Modifier,
    prompt: String,
    promptColor: Color,
    onManualClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = prompt,
            color = promptColor,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onManualClick) {
            Text("Saisir manuellement")
        }
    }
}

@Composable
private fun BarcodeCameraPreview(onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember { BarcodeScanning.getClient() }
    val cameraRef = remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose { scanner.close() }
    }

    // Autofocus continu : trigger AF au centre toutes les 2,5 s
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
                    processBarcode(imageProxy, scanner, onBarcodeDetected)
                }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraRef.value = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, analysis
                    )
                }.onFailure { Log.e("BarcodeScan", "Bind failed", it) }
            }, executor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
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

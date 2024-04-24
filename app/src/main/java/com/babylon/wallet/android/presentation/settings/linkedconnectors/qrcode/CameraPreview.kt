package com.babylon.wallet.android.presentation.settings.linkedconnectors.qrcode

import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier,
    disableBackHandler: Boolean = true,
    isVisible: Boolean = true,
    onQrCodeDetected: (qrCode: String) -> Unit,
) {
    BarcodePreviewView(
        modifier = modifier,
        onQrCodeDetected = onQrCodeDetected,
        isVisible = isVisible
    )
    BackHandler(enabled = disableBackHandler) { }
}

@Composable
private fun BarcodePreviewView(
    onQrCodeDetected: (qrCode: String) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { context ->
            PreviewView(context)
        },
        update = { previewView ->
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(previewView.context)

            cameraProviderFuture.addListener({
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val barcodeAnalyser = QrcodeAnalyser { qrCodes ->
                    qrCodes.forEach { barcode ->
                        barcode.rawValue?.let { barcodeValue ->
                            onQrCodeDetected(barcodeValue.replace("radix:", ""))
                        }
                    }
                }
                val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(cameraExecutor, barcodeAnalyser)
                    }
                if (isVisible) {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } else {
                    cameraProvider.unbindAll()
                }
            }, ContextCompat.getMainExecutor(previewView.context))
        },
        modifier = modifier
    )
}

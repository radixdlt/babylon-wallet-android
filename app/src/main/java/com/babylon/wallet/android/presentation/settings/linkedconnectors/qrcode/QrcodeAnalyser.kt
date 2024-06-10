package com.babylon.wallet.android.presentation.settings.linkedconnectors.qrcode

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber

@SuppressLint("UnsafeOptInUsageError")
class QrcodeAnalyser(
    private val onBarcodeDetected: (barcodes: List<Barcode>) -> Unit,
    private val onError: (Throwable) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        image.image?.let { imageToAnalyze ->
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val barcodeScanner = BarcodeScanning.getClient(options)
            val imageToProcess = InputImage.fromMediaImage(imageToAnalyze, image.imageInfo.rotationDegrees)

            barcodeScanner.process(imageToProcess)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        onBarcodeDetected(barcodes)
                    }
                }
                .addOnFailureListener { error ->
                    Timber.d("Failed to scan QR. Error: ${error.message}")
                    onError(error)
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }
}

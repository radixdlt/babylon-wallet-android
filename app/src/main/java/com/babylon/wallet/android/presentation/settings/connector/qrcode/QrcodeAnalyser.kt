package com.babylon.wallet.android.presentation.settings.connector.qrcode

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.TimeUnit

@SuppressLint("UnsafeOptInUsageError")
class QrcodeAnalyser(
    private val onBarcodeDetected: (barcode: Barcode) -> Unit,
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimeStamp = 0L

    override fun analyze(image: ImageProxy) {
        // This is the hack to prevent the same qr will be delivered multiple times and trigger navigation more than once
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimeStamp >= TimeUnit.SECONDS.toMillis(1)) {
            image.image?.let { imageToAnalyze ->
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(options)
                val imageToProcess = InputImage.fromMediaImage(imageToAnalyze, image.imageInfo.rotationDegrees)
                barcodeScanner.process(imageToProcess)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.let {
                            onBarcodeDetected(it)
                        }
                    }
                    .addOnCompleteListener {
                        image.close()
                    }
            }
            lastAnalyzedTimeStamp = currentTimestamp
        } else {
            image.close()
        }
    }
}

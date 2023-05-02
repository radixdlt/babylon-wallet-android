package rdx.works.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QRCodeGenerator {

    fun forAccount(address: String): Bitmap {
        val qrContent = "$ADDRESS_QR_PREFIX$address"
        val size = 500
        val matrix = QRCodeWriter().encode(
            qrContent,
            BarcodeFormat.QR_CODE,
            size,
            size
        )

        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also {
            for (x in 0 until matrix.height) {
                for (y in 0 until matrix.width) {
                    it.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }

    private const val ADDRESS_QR_PREFIX = "radix:"

}

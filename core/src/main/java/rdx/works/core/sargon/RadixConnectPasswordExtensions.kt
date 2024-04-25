package rdx.works.core.sargon

import android.util.Log
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBagOfBytes
import okio.ByteString.Companion.decodeHex

fun RadixConnectPassword.Companion.init(connectionPassword: String): RadixConnectPassword? = try {
    val bytes = connectionPassword.decodeHex().toByteArray().toBagOfBytes()
    RadixConnectPassword.init(bytes = Exactly32Bytes.init(bytes))
} catch (exception: IllegalArgumentException) {
    Log.e("RadixConnectPassword", "Failed to parse encryption key from connection id", exception)
    null
}

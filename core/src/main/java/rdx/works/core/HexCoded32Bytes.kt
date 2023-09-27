package rdx.works.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class HexCoded32Bytes(@SerialName("value") val value: String) {
    init {
        val byteArray = value.decodeHex()
        require(byteArray.size == byteCount) { "value must be 32 bytes but it is ${byteArray.size}" }
    }

    companion object {
        private const val byteCount = 32
    }
}

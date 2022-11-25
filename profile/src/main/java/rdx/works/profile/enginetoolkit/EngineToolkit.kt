package rdx.works.profile.enginetoolkit

import com.radixdlt.hex.extensions.toHexString
import rdx.works.profile.data.model.pernetwork.EntityAddress

/**
 * This will depend on KotlinEngineToolkit
 */
interface EngineToolkit {
    fun deriveAddress(
        compressedPublicKey: ByteArray
    ): EntityAddress
}

class EngineToolkitImpl : EngineToolkit {
    override fun deriveAddress(
        compressedPublicKey: ByteArray
    ): EntityAddress {
        //TODO once engine toolkit ready, we will used it derive address
        val shortenedPublicKey = compressedPublicKey.toHexString().subSequence(0, 25)
        return EntityAddress("mocked_account_address_$shortenedPublicKey)")
    }
}

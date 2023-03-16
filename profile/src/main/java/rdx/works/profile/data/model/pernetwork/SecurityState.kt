package rdx.works.profile.data.model.pernetwork

import com.radixdlt.hex.extensions.toHexString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator(discriminator = "discriminator")
sealed class SecurityState {

    @Serializable
    @SerialName("unsecured")
    data class Unsecured(
        @SerialName("unsecuredEntityControl")
        val unsecuredEntityControl: UnsecuredEntityControl
    ) : SecurityState()

    /**
     * Basic security control of an unsecured entity. When said entity
     * is "securified" it will no longer be controlled by this `UnsecuredEntityControl`
     * but rather by an `AccessControl`. It is a name space holding the
     * single factor instance which was used to create
     */
    @Serializable
    data class UnsecuredEntityControl(
        @SerialName("genesisFactorInstance")
        val genesisFactorInstance: FactorInstance
    )

    companion object {

        fun unsecured(
            compressedPublicKey: ByteArray,
            derivationPath: DerivationPath,
            factorSourceId: String
        ): Unsecured = Unsecured(
            unsecuredEntityControl = UnsecuredEntityControl(
                genesisFactorInstance = FactorInstance(
                    derivationPath = derivationPath,
                    factorSourceId = factorSourceId,
                    publicKey = FactorInstance.PublicKey.curve25519PublicKey(
                        compressedPublicKey = compressedPublicKey.toHexString()
                    )
                )
            )
        )
    }
}

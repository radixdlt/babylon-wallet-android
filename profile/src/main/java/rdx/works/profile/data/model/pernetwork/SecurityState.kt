package rdx.works.profile.data.model.pernetwork

import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.hex.extensions.toHexString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.hashToFactorId
import rdx.works.profile.data.model.factorsources.FactorSources
import java.util.Date

@Serializable
data class SecurityState(
    @SerialName("discriminator")
    val discriminator: String,

    @SerialName("unsecuredEntityControl")
    val unsecuredEntityControl: UnsecuredEntityControl
) {
    companion object {
        /**
         * A non-"securitfied" Security state used for entity (Account/Persona). Protected with single factor instance
         * until securified, and thus protected with an "AccessControl".
         */
        fun unsecuredSecurityState(
            compressedPublicKey: ByteArray,
            derivationPath: DerivationPath,
            factorSources: FactorSources
        ): SecurityState {
            return SecurityState(
                discriminator = "unsecured",
                unsecuredEntityControl = UnsecuredEntityControl(
                    genesisFactorInstance = GenesisFactorInstance(
                        derivationPath = derivationPath,
                        factorInstanceID = compressedPublicKey.hashToFactorId(),
                        factorSourceReference = FactorSourceReference.curve25519FactorSourceReference(
                            factorSource = factorSources
                        ),
                        initializationDate = Date().toString(),
                        publicKey = PublicKey.curve25519PublicKey(
                            compressedPublicKey = compressedPublicKey.removeLeadingZero().toHexString()
                        )
                    )
                )
            )
        }
    }

    /**
     * Basic security control of an unsecured entity. When said entity
     * is "securified" it will no longer be controlled by this `UnsecuredEntityControl`
     * but rather by an `AccessControl`. It is a name space holding the
     * single factor instance which was used to create
     */
    @Serializable
    data class UnsecuredEntityControl(
        @SerialName("genesisFactorInstance")
        val genesisFactorInstance: GenesisFactorInstance
    )
}
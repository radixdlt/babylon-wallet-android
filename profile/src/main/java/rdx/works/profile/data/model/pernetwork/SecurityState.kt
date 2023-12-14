package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import rdx.works.profile.data.model.factorsources.FactorSource

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

        @SerialName("transactionSigning")
        val transactionSigning: FactorInstance,

        @SerialName("authenticationSigning")
        val authenticationSigning: FactorInstance? = null
    )

    companion object {

        fun unsecured(
            publicKey: FactorInstance.PublicKey,
            derivationPath: DerivationPath,
            factorSourceId: FactorSource.FactorSourceID.FromHash
        ): Unsecured = Unsecured(
            unsecuredEntityControl = UnsecuredEntityControl(
                transactionSigning = FactorInstance(
                    badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                        derivationPath = derivationPath,
                        publicKey = publicKey
                    ),
                    factorSourceId = factorSourceId
                )
            )
        )
    }
}

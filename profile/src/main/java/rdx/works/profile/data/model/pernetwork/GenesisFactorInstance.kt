package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenesisFactorInstance(
    @SerialName("derivationPath")
    val derivationPath: DerivationPath,

    @SerialName("factorInstanceID")
    val factorInstanceID: String,

    @SerialName("factorSourceReference")
    val factorSourceReference: FactorSourceReference,

    @SerialName("initializationDate")
    val initializationDate: String,

    @SerialName("publicKey")
    val publicKey: PublicKey
)
package rdx.works.profile.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DerivationPath(
    @SerialName("derivationPath")
    val derivationPath: String,

    @SerialName("discriminator")
    val discriminator: String
) {
    companion object {

        private const val accountDiscriminator = "accountPath"
        private const val identityDiscriminator = "identityPath"

        fun accountDerivationPath(
            derivationPath: String
        ): DerivationPath {
            return DerivationPath(
                derivationPath = derivationPath,
                discriminator = accountDiscriminator
            )
        }

        fun identityDerivationPath(
            derivationPath: String
        ): DerivationPath {
            return DerivationPath(
                derivationPath = derivationPath,
                discriminator = identityDiscriminator
            )
        }
    }
}

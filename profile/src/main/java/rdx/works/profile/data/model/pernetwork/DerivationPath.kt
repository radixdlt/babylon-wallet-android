package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.DerivationPathScheme.CAP_26

@Serializable
data class DerivationPath(
    @SerialName("path")
    val path: String,

    @SerialName("scheme")
    val scheme: DerivationPathScheme
) {
    companion object {

        fun forAccount(
            derivationPath: String
        ): DerivationPath {
            return DerivationPath(
                path = derivationPath,
                scheme = CAP_26
            )
        }

        fun forIdentity(
            derivationPath: String
        ): DerivationPath {
            return DerivationPath(
                path = derivationPath,
                scheme = CAP_26
            )
        }
    }
}

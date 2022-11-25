package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.factorsources.FactorSources

@Serializable
data class FactorSourceReference(
    @SerialName("factorSourceID")
    val factorSourceID: String,

    @SerialName("factorSourceKind")
    val factorSourceKind: String
) {
    companion object {
        private const val curve25510OnDeviceFactorSourceKind =
            "curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSourceKind"

        fun curve25519FactorSourceReference(
            factorSource: FactorSources
        ): FactorSourceReference {
            return FactorSourceReference(
                factorSourceID = factorSource.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                    .first().factorSourceID,
                factorSourceKind = curve25510OnDeviceFactorSourceKind
            )
        }
    }
}
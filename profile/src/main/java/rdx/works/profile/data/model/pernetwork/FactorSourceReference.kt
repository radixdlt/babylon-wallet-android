package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FactorSourceReference(
    @SerialName("factorSourceID")
    val factorSourceID: String,

    @SerialName("factorSourceKind")
    val factorSourceKind: String
)

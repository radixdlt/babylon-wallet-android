package rdx.works.profile.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntityAddress(
    @SerialName("address")
    val address: String,
)
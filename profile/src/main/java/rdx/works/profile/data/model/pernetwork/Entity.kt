package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import rdx.works.profile.data.model.factorsources.EntityFlag

sealed class Entity {
    abstract val networkID: Int
    abstract val address: String
    abstract val securityState: SecurityState

    @SerialName("flags")
    @EncodeDefault
    abstract val flags: List<EntityFlag>
}

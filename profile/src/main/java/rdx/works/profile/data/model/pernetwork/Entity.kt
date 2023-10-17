package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import rdx.works.profile.data.model.factorsources.EntityFlag

sealed class Entity {
    abstract val networkID: Int
    abstract val address: String
    abstract val securityState: SecurityState

    @SerialName("flags")
    abstract val flags: Set<EntityFlag>
}

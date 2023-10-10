package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EntityFlag {
    @SerialName("deletedByUser")
    DeletedByUser
}

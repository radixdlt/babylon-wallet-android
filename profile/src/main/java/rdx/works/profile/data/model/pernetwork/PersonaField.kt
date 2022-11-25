package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PersonaField(
    @SerialName("id")
    val id: String,

    @SerialName("kind")
    val kind: String,

    @SerialName("value")
    val value: String
) {
    companion object {
        fun init(
            kind: String,
            value: String
        ): PersonaField {
            return PersonaField(
                id = UUID.randomUUID().toString(),
                kind = kind,
                value = value
            )
        }
    }
}

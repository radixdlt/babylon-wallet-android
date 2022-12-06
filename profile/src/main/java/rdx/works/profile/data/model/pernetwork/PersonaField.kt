package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PersonaField(
    @SerialName("id")
    val id: String,

    @SerialName("kind")
    val kind: PersonaFieldKind,

    @SerialName("value")
    val value: String
) {

    @Serializable
    enum class PersonaFieldKind {
        @SerialName("firstName")
        FirstName,
        @SerialName("lastName")
        LastName,
        @SerialName("email")
        Email,
        @SerialName("personalIdentificationNumber")
        PersonalIdentificationNumber,
        @SerialName("zipCode")
        ZipCode
    }

    companion object {
        fun init(
            kind: PersonaFieldKind,
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

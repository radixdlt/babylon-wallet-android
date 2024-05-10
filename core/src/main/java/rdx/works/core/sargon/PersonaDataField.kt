package rdx.works.core.sargon

import com.radixdlt.sargon.CollectionOfEmailAddresses
import com.radixdlt.sargon.CollectionOfPhoneNumbers
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.PersonaDataEntryEmailAddress
import com.radixdlt.sargon.PersonaDataEntryId
import com.radixdlt.sargon.PersonaDataEntryName
import com.radixdlt.sargon.PersonaDataEntryPhoneNumber
import com.radixdlt.sargon.PersonaDataIdentifiedEmailAddress
import com.radixdlt.sargon.PersonaDataIdentifiedName
import com.radixdlt.sargon.PersonaDataIdentifiedPhoneNumber
import com.radixdlt.sargon.PersonaDataNameVariant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.UUIDGenerator

sealed interface PersonaDataField {

    val kind: Kind
        get() = when (this) {
            is Email -> Kind.EmailAddress
            is Name -> Kind.Name
            is PhoneNumber -> Kind.PhoneNumber
        }

    @JvmInline
    @Serializable
    value class PhoneNumber(val value: String) : PersonaDataField

    @JvmInline
    @Serializable
    value class Email(val value: String) : PersonaDataField

    @Serializable
    data class Name(

        @SerialName("variant")
        val variant: Variant,

        @SerialName("givenNames")
        val given: String,

        @SerialName("familyName")
        val family: String,

        @SerialName("nickname")
        val nickname: String

    ) : PersonaDataField {
        @Serializable
        enum class Variant {
            @SerialName("western")
            Western,

            @SerialName("eastern")
            Eastern
        }
    }

    enum class Kind {
        Name, PhoneNumber, EmailAddress;

        companion object {
            val supportedKindsSingleValue: List<Kind>
                get() = listOf(Name)
            val supportedKindsMultipleValues: List<Kind>
                get() = listOf(EmailAddress, PhoneNumber)
            val supportedKinds: List<Kind>
                get() = listOf(Name, EmailAddress, PhoneNumber)
        }
    }
}

@Serializable
data class IdentifiedEntry<T>(
    @SerialName("value")
    val value: T,
    @SerialName("id")
    val id: String
) {
    val uuid: PersonaDataEntryId
        get() = PersonaDataEntryId.fromString(id)

    companion object {
        fun <T> init(value: T): IdentifiedEntry<T> {
            return IdentifiedEntry(value = value, id = UUIDGenerator.uuid().toString())
        }

        fun <T> init(value: T, id: String): IdentifiedEntry<T> {
            return IdentifiedEntry(value = value, id = id)
        }
    }
}

fun List<PersonaDataField>.toPersonaData(): PersonaData {
    return PersonaData(
        name = filterIsInstance<PersonaDataField.Name>().firstOrNull()?.let {
            PersonaDataIdentifiedName(
                id = PersonaDataEntryId.randomUUID(),
                value = PersonaDataEntryName(
                    variant = when (it.variant) {
                        PersonaDataField.Name.Variant.Western -> PersonaDataNameVariant.WESTERN
                        PersonaDataField.Name.Variant.Eastern -> PersonaDataNameVariant.EASTERN
                    },
                    familyName = it.family,
                    givenNames = it.given,
                    nickname = it.nickname
                )
            )
        },
        emailAddresses = filterIsInstance<PersonaDataField.Email>().let { field ->
            CollectionOfEmailAddresses(
                collection = field.map {
                    PersonaDataIdentifiedEmailAddress(id = PersonaDataEntryId.randomUUID(), value = PersonaDataEntryEmailAddress(it.value))
                }
            )
        },
        phoneNumbers = filterIsInstance<PersonaDataField.PhoneNumber>().let { field ->
            CollectionOfPhoneNumbers(
                collection = field.map {
                    PersonaDataIdentifiedPhoneNumber(id = PersonaDataEntryId.randomUUID(), value = PersonaDataEntryPhoneNumber(it.value))
                }
            )
        }
    )
}

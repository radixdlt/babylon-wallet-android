package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import rdx.works.core.UUIDGenerator
import java.time.Instant

@Serializable
data class PersonaData(

    @SerialName("name")
    val name: IdentifiedEntry<PersonaDataField.Name>? = null,

    @SerialName("dateOfBirth")
    val dateOfBirth: IdentifiedEntry<PersonaDataField.DateOfBirth>? = null,

    @SerialName("companyName")
    val companyName: IdentifiedEntry<PersonaDataField.CompanyName>? = null,

    @SerialName("emailAddresses")
    val emailAddresses: List<IdentifiedEntry<PersonaDataField.Email>> = emptyList(),

    @SerialName("phoneNumbers")
    val phoneNumbers: List<IdentifiedEntry<PersonaDataField.PhoneNumber>> = emptyList(),

    @SerialName("urls")
    val urls: List<IdentifiedEntry<PersonaDataField.Url>> = emptyList(),

    @SerialName("postalAddresses")
    val postalAddresses: List<IdentifiedEntry<PersonaDataField.PostalAddress>> = emptyList(),

    @SerialName("creditCards")
    val creditCards: List<IdentifiedEntry<PersonaDataField.CreditCard>> = emptyList()

) {

    val allFields: List<IdentifiedEntry<out PersonaDataField>>
        get() = listOfNotNull(
            name,
            dateOfBirth,
            companyName
        ) + phoneNumbers + emailAddresses + urls + postalAddresses + creditCards

    val allFieldIds: List<PersonaDataEntryID>
        get() = allFields.map { it.id }

    fun getDataFieldKind(id: PersonaDataEntryID): PersonaDataField.Kind? {
        return allFields.firstOrNull {
            it.id == id
        }?.value?.kind
    }

    sealed interface PersonaDataField {

        val kind: Kind
            get() = when (this) {
                is Email -> Kind.EmailAddress
                is CompanyName -> Kind.CompanyName
                is CreditCard -> Kind.CreditCard
                is DateOfBirth -> Kind.DateOfBirth
                is Name -> Kind.Name
                is PhoneNumber -> Kind.PhoneNumber
                is PostalAddress -> Kind.PostalAddress
                is Url -> Kind.Url
            }

        @JvmInline
        @Serializable
        value class CompanyName(val companyName: String) : PersonaDataField

        @JvmInline
        @Serializable
        value class DateOfBirth(val dateOfBirth: @Contextual Instant) : PersonaDataField

        @JvmInline
        @Serializable
        value class Url(val value: String) : PersonaDataField

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

            @SerialName("given")
            val given: String,

            @SerialName("family")
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

        @Serializable(PostalAddressSerializer::class)
        data class PostalAddress(
            @SerialName("fields")
            val fields: List<Field> = emptyList()
        ) : PersonaDataField {

            val countryOrRegion: Field.CountryOrRegion?
                get() = fields.filterIsInstance<Field.CountryOrRegion>().firstOrNull()

            @Serializable
            @JsonClassDiscriminator("discriminator")
            sealed interface Field {

                @Serializable
                @SerialName("countryOrRegion")
                data class CountryOrRegion(val value: rdx.works.profile.data.model.pernetwork.CountryOrRegion) : Field

                @Serializable
                @SerialName("streetLine0")
                data class StreetLine0(val value: String) : Field

                @Serializable
                @SerialName("streetLine1")
                data class StreetLine1(val value: String) : Field

                @Serializable
                @SerialName("postalCode")
                data class PostalCode(val value: String) : Field

                @Serializable
                @SerialName("postcode")
                data class Postcode(val value: String) : Field

                // / US
                @Serializable
                @SerialName("zip")
                data class Zip(val value: String) : Field

                @Serializable
                @SerialName("city")
                data class City(val value: String) : Field

                @Serializable
                @SerialName("state")
                data class State(val value: String) : Field

                // / Australia
                @Serializable
                @SerialName("suburb")
                data class Suburb(val value: String) : Field

                // / Brazil
                @Serializable
                @SerialName("neighbourhood")
                data class Neighbourhood(val value: String) : Field

                // / Canada
                @Serializable
                @SerialName("province")
                data class Province(val value: String) : Field

                // / Egypt
                @Serializable
                @SerialName("governorate")
                data class Governorate(val value: String) : Field

                // / Hong Kong
                @Serializable
                @SerialName("district")
                data class District(val value: String) : Field

                // / Hong Kong, Somalia
                @Serializable
                @SerialName("region")
                data class Region(val value: String) : Field

                // / United Arab Emirates
                @SerialName("area")
                @Serializable
                data class Area(val value: String) : Field

                // / Carribean Netherlands
                @SerialName("islandName")
                @Serializable
                data class IslandName(val value: String) : Field

                // / China
                @Serializable
                @SerialName("prefectureLevelCity")
                data class PrefectureLevelCity(val value: String) : Field

                // / Russia
                @Serializable
                @SerialName("subjectOfTheFederation")
                data class SubjectOfTheFederation(val value: String) : Field

                @Serializable
                @SerialName("county")
                data class County(val value: String) : Field

                // / Japan
                @Serializable
                @SerialName("prefecture")
                data class Prefecture(val value: String) : Field

                // / Japan
                @Serializable
                @SerialName("countySlashCity")
                data class CountySlashCity(val value: String) : Field

                // / Japan
                @Serializable
                @SerialName("furtherDivisionsLine0")
                data class FurtherDivisionsLine0(val value: String) : Field

                // / Japan
                @Serializable
                @SerialName("furtherDivisionsLine1")
                data class FurtherDivisionsLine1(val value: String) : Field

                // / Taiwan
                @Serializable
                @SerialName("townshipSlashDistrict")
                data class TownshipSlashDistrict(val value: String) : Field

                // / Colombia
                @Serializable
                @SerialName("department")
                data class Department(val value: String) : Field

                // / UK
                @Serializable
                @SerialName("townSlashCity")
                data class TownSlashCity(val value: String) : Field

                // / Jordan
                @Serializable
                @SerialName("postalDistrict")
                data class PostalDistrict(val value: String) : Field

                // / Philippines
                @Serializable
                @SerialName("districtSlashSubdivision")
                data class DistrictSlashSubdivision(val value: String) : Field
            }
        }

        @Serializable
        data class CreditCard(
            @SerialName("expiry")
            val expiry: Expiry,
            @SerialName("holder")
            val holder: String,
            @SerialName("number")
            val number: String,
            @SerialName("cvc")
            val cvc: Int,
        ) : PersonaDataField {
            @Serializable
            data class Expiry(
                @SerialName("year")
                val year: Int,
                @SerialName("month")
                val month: Int,
            )
        }

        enum class Kind {
            Name, DateOfBirth, CompanyName, PhoneNumber, EmailAddress, Url, PostalAddress, CreditCard;

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
}

@Serializable
data class IdentifiedEntry<T>(
    @SerialName("value")
    val value: T,
    @SerialName("id")
    val id: PersonaDataEntryID
) {
    companion object {
        fun <T> init(value: T): IdentifiedEntry<T> {
            return IdentifiedEntry(value = value, id = UUIDGenerator.uuid().toString())
        }

        fun <T> init(value: T, id: String): IdentifiedEntry<T> {
            return IdentifiedEntry(value = value, id = id)
        }
    }
}

typealias PersonaDataEntryID = String

object PostalAddressSerializer : KSerializer<PersonaData.PersonaDataField.PostalAddress> {

    private val delegateSerializer = ListSerializer(PersonaData.PersonaDataField.PostalAddress.Field.serializer())
    override val descriptor: SerialDescriptor
        get() = delegateSerializer.descriptor

    override fun deserialize(decoder: Decoder): PersonaData.PersonaDataField.PostalAddress {
        return PersonaData.PersonaDataField.PostalAddress(decoder.decodeSerializableValue(delegateSerializer))
    }

    override fun serialize(encoder: Encoder, value: PersonaData.PersonaDataField.PostalAddress) {
        encoder.encodeSerializableValue(delegateSerializer, value.fields)
    }
}

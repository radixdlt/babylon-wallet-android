package rdx.works.core.sargon

import com.radixdlt.sargon.CollectionOfEmailAddresses
import com.radixdlt.sargon.CollectionOfPhoneNumbers
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.PersonaDataEntryId
import com.radixdlt.sargon.PersonaDataNameVariant
import com.radixdlt.sargon.RequestedQuantity
import com.radixdlt.sargon.SharedPersonaData
import com.radixdlt.sargon.SharedToDappWithPersonaIDsOfPersonaDataEntries
import com.radixdlt.sargon.extensions.exactly

fun PersonaData.Companion.empty() = PersonaData(
    name = null,
    phoneNumbers = CollectionOfPhoneNumbers(emptyList()),
    emailAddresses = CollectionOfEmailAddresses(emptyList())
)

val PersonaData.fields: List<IdentifiedEntry<out PersonaDataField>>
    get() = name?.let {
        listOf(
            IdentifiedEntry.init(
                value = PersonaDataField.Name(
                    variant = when (it.value.variant) {
                        PersonaDataNameVariant.WESTERN -> PersonaDataField.Name.Variant.Western
                        PersonaDataNameVariant.EASTERN -> PersonaDataField.Name.Variant.Eastern
                    },
                    given = it.value.givenNames,
                    family = it.value.familyName,
                    nickname = it.value.nickname
                ),
                id = it.id.toString()
            )
        )
    }.orEmpty() + phoneNumbers.collection.map {
        IdentifiedEntry.init(
            value = PersonaDataField.PhoneNumber(it.value.number),
            id = it.id.toString()
        )
    } + emailAddresses.collection.map {
        IdentifiedEntry.init(
            value = PersonaDataField.Email(it.value.email),
            id = it.id.toString()
        )
    }

val PersonaData.fieldIds: List<PersonaDataEntryId>
    get() = emailAddresses.collection.map { it.id } + phoneNumbers.collection.map { it.id } + name?.id?.let { listOf(it) }.orEmpty()

@Suppress("ReturnCount")
fun PersonaData.getDataFieldKind(id: PersonaDataEntryId): PersonaDataField.Kind? {
    if (name?.id == id) return PersonaDataField.Kind.Name

    if (id in phoneNumbers.collection.map { it.id }) return PersonaDataField.Kind.PhoneNumber
    if (id in emailAddresses.collection.map { it.id }) return PersonaDataField.Kind.EmailAddress

    return null
}

fun PersonaData.toSharedPersonaData(
    requestedFields: Map<PersonaDataField.Kind, Int>
): SharedPersonaData = SharedPersonaData(
    name = if (requestedFields.containsKey(PersonaDataField.Kind.Name)) name?.id else null,
    emailAddresses = if (requestedFields.containsKey(PersonaDataField.Kind.EmailAddress)) {
        SharedToDappWithPersonaIDsOfPersonaDataEntries(
            request = RequestedQuantity.exactly(quantity = 1),
            ids = emailAddresses.collection.map { it.id }
        )
    } else {
        null
    },
    phoneNumbers = if (requestedFields.containsKey(PersonaDataField.Kind.PhoneNumber)) {
        SharedToDappWithPersonaIDsOfPersonaDataEntries(
            request = RequestedQuantity.exactly(1),
            ids = phoneNumbers.collection.map { it.id }
        )
    } else {
        null
    }
)

val SharedPersonaData.alreadyGrantedIds: List<PersonaDataEntryId>
    get() = listOfNotNull(name) + emailAddresses?.ids.orEmpty() + phoneNumbers?.ids.orEmpty()

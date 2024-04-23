package rdx.works.core.sargon

import com.radixdlt.sargon.AuthorizedPersonaSimple
import com.radixdlt.sargon.PersonaDataEntryID
import com.radixdlt.sargon.SharedToDappWithPersonaIDsOfPersonaDataEntries

fun AuthorizedPersonaSimple.ensurePersonaDataExist(
    existingFieldIds: List<PersonaDataEntryID>
): AuthorizedPersonaSimple {
    return copy(
        sharedPersonaData = sharedPersonaData.copy(
            name = if (existingFieldIds.contains(sharedPersonaData.name)) sharedPersonaData.name else null,
            emailAddresses = sharedPersonaData.emailAddresses?.removeNonExistingIn(existingFieldIds)?.takeIf { it.ids.isNotEmpty() },
            phoneNumbers = sharedPersonaData.phoneNumbers?.removeNonExistingIn(existingFieldIds)?.takeIf { it.ids.isNotEmpty() }
        )
    )
}

private fun SharedToDappWithPersonaIDsOfPersonaDataEntries.removeNonExistingIn(
    existingIds: List<PersonaDataEntryID>
): SharedToDappWithPersonaIDsOfPersonaDataEntries = copy(ids = ids.filter { it in existingIds })
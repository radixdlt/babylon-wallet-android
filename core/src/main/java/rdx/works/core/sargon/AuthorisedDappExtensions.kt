package rdx.works.core.sargon

import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedPersonaSimple
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.ProfileNetwork
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.RequestedQuantity
import com.radixdlt.sargon.SharedPersonaData
import com.radixdlt.sargon.SharedToDappWithPersonaAccountAddresses
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.ReferencesToAuthorizedPersonas
import rdx.works.core.mapWhen

fun AuthorizedDapp.hasAuthorizedPersona(personaAddress: IdentityAddress): Boolean {
    return referencesToAuthorizedPersonas.asIdentifiable().getBy(personaAddress) != null
}

fun AuthorizedDapp.updateAuthorizedDAppPersonas(
    authorizedDAppPersonas: List<AuthorizedPersonaSimple>
): AuthorizedDapp = copy(
    referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas(
        authorizedDAppPersonas + referencesToAuthorizedPersonas
    ).asList()
)

fun AuthorizedDapp.updateAuthorizedDAppPersonaFields(
    personaAddress: IdentityAddress,
    personaData: PersonaData,
    requiredFields: Map<PersonaDataField.Kind, Int>
): AuthorizedDapp {
    val updatedAuthPersonas = referencesToAuthorizedPersonas.mapWhen(
        predicate = { it.identityAddress == personaAddress },
        mutation = { persona ->
            val sharedPersonaData = personaData.toSharedPersonaData(requiredFields)
            persona.copy(sharedPersonaData = sharedPersonaData)
        }
    )
    return copy(referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas(updatedAuthPersonas).asList())
}

fun AuthorizedDapp.addOrUpdateAuthorizedDAppPersona(
    persona: Persona,
    lastUsed: Timestamp
): AuthorizedDapp {
    val existing = referencesToAuthorizedPersonas.asIdentifiable().getBy(persona.address)
    val updatedAuthPersonas = if (existing != null) {
        referencesToAuthorizedPersonas.toMutableList().apply {
            val index = indexOf(existing)
            if (index != -1) {
                removeAt(index)
                add(index, existing.copy(lastLogin = lastUsed))
            }
        }
    } else {
        val newAuthorizedPersona = AuthorizedPersonaSimple(
            identityAddress = persona.address,
            lastLogin = lastUsed,
            sharedAccounts = SharedToDappWithPersonaAccountAddresses(
                request = RequestedQuantity(
                    quantifier = RequestedNumberQuantifier.EXACTLY,
                    quantity = 0u
                ),
                ids = emptyList()
            ),
            sharedPersonaData = SharedPersonaData(
                name = null,
                emailAddresses = null,
                phoneNumbers = null
            )
        )

        listOf(newAuthorizedPersona) + referencesToAuthorizedPersonas
    }

    return copy(referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas(updatedAuthPersonas).asList())
}

fun AuthorizedDapp.updateDAppAuthorizedPersonaSharedAccounts(
    personaAddress: IdentityAddress,
    sharedAccounts: SharedToDappWithPersonaAccountAddresses
): AuthorizedDapp {
    val persona = referencesToAuthorizedPersonas.asIdentifiable().getBy(personaAddress)
    requireNotNull(persona)
    return copy(
        referencesToAuthorizedPersonas = referencesToAuthorizedPersonas.asIdentifiable().updateOrAppend(
            element = persona.copy(sharedAccounts = sharedAccounts)
        ).asList()
    )
}

fun ProfileNetwork.validateAuthorizedPersonas(authorizedDApp: AuthorizedDapp): AuthorizedDapp {
    require(id == authorizedDApp.networkId)

    // Validate that all Personas are known and that every Field.ID is known for each Persona.
    for (personaNeedle in authorizedDApp.referencesToAuthorizedPersonas) {
        val persona = personas.asIdentifiable().getBy(identifier = personaNeedle.identityAddress)
        val fieldIDNeedles = personaNeedle.sharedPersonaData.alreadyGrantedIds.toSet()
        val fieldIDHaystack = persona?.personaData?.fieldIds?.toSet().orEmpty()

        require(fieldIDHaystack.containsAll(fieldIDNeedles))
    }

    // Validate that all Accounts are known
    val accountAddressNeedles = authorizedDApp.referencesToAuthorizedPersonas.flatMap {
        it.sharedAccounts?.ids.orEmpty()
    }.toSet()

    val accountAddressHaystack = accounts.map { it.address }.toSet()

    require(accountAddressHaystack.containsAll(accountAddressNeedles))

    // All good
    return authorizedDApp
}

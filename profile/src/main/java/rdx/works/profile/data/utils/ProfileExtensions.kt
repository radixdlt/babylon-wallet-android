@file:Suppress("TooManyFunctions")

package rdx.works.profile.data.utils

import com.radixdlt.ret.AccountDefaultDepositRule
import com.radixdlt.ret.ResourcePreference
import rdx.works.core.InstantGenerator
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Transaction
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.model.factorsources.EntityFlag
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.IdentifiedEntry
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.RequestedNumber
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.Shared

fun Profile.updateLastUsed(id: FactorSource.FactorSourceID): Profile {
    return copy(
        factorSources = this.factorSources.mapWhen(predicate = { it.id == id }) { factorSource ->
            factorSource.common.lastUsedOn = InstantGenerator()
            factorSource
        }
    )
}

fun Profile.renameAccountDisplayName(
    accountToRename: Network.Account,
    newDisplayName: String
): Profile {
    val networkId = currentGateway.network.networkId()
    val renamedAccount = accountToRename.copy(
        displayName = newDisplayName
    )

    return copy(
        networks = networks.mapWhen(
            predicate = { it.networkID == networkId.value },
            mutation = { network ->
                network.copy(
                    accounts = network.accounts.mapWhen(
                        predicate = { it == accountToRename },
                        mutation = { renamedAccount }
                    )
                )
            }
        )
    )
}

fun Network.Account.isOlympiaAccount(): Boolean {
    val unsecuredEntityControl = (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl
    return when (val virtualBadge = unsecuredEntityControl?.transactionSigning?.badge) {
        is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
            virtualBadge.publicKey.curve == Slip10Curve.SECP_256K1
        }

        null -> false
    }
}

fun Profile.changeDefaultDepositGuarantee(
    defaultDepositGuarantee: Double
): Profile {
    return copy(
        appPreferences = AppPreferences(
            transaction = Transaction(defaultDepositGuarantee = defaultDepositGuarantee),
            display = appPreferences.display,
            security = appPreferences.security,
            gateways = appPreferences.gateways,
            p2pLinks = appPreferences.p2pLinks
        )
    )
}

fun Entity.usesCurve25519(): Boolean {
    val unsecuredEntityControl = (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl
    return when (val virtualBadge = unsecuredEntityControl?.transactionSigning?.badge) {
        is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
            virtualBadge.publicKey.curve == Slip10Curve.CURVE_25519
        }

        null -> false
    }
}

fun Entity.factorSourceId(): FactorSource.FactorSourceID {
    return (this.securityState as SecurityState.Unsecured).unsecuredEntityControl.transactionSigning.factorSourceId
}

fun Entity.hasAuthSigning(): Boolean {
    return when (val state = securityState) {
        is SecurityState.Unsecured -> {
            state.unsecuredEntityControl.authenticationSigning != null
        }
    }
}

fun Entity.networkId() {
    this.networkID
}

fun PersonaData.PersonaDataField.sortOrderInt(): Int {
    return kind.ordinal
}

@Suppress("TooGenericExceptionThrown")
fun PersonaData.PersonaDataField.Kind.empty(): IdentifiedEntry<PersonaData.PersonaDataField> {
    val value = when (this) {
        PersonaData.PersonaDataField.Kind.Name -> PersonaData.PersonaDataField.Name(
            variant = PersonaData.PersonaDataField.Name.Variant.Western,
            given = "",
            family = "",
            nickname = ""
        )

        PersonaData.PersonaDataField.Kind.EmailAddress -> PersonaData.PersonaDataField.Email("")
        PersonaData.PersonaDataField.Kind.PhoneNumber -> PersonaData.PersonaDataField.PhoneNumber("")
        else -> throw RuntimeException("Field $this not supported")
    }
    return IdentifiedEntry.init(value)
}

fun PersonaData.toSharedPersonaData(
    requestedFields: Map<PersonaData.PersonaDataField.Kind, Int>
): Network.AuthorizedDapp.SharedPersonaData {
    // TODO properly store requests when we will allow multiple values for entries
    return Network.AuthorizedDapp.SharedPersonaData(
        name = if (requestedFields.containsKey(PersonaData.PersonaDataField.Kind.Name)) name?.id else null,
        dateOfBirth = if (requestedFields.containsKey(PersonaData.PersonaDataField.Kind.DateOfBirth)) dateOfBirth?.id else null,
        companyName = if (requestedFields.containsKey(PersonaData.PersonaDataField.Kind.Name)) companyName?.id else null,
        emailAddresses = if (requestedFields.containsKey(PersonaData.PersonaDataField.Kind.EmailAddress)) {
            Shared(
                emailAddresses.map { it.id },
                RequestedNumber.exactly(1)
            )
        } else {
            null
        },
        phoneNumbers = if (requestedFields.containsKey(PersonaData.PersonaDataField.Kind.PhoneNumber)) {
            Shared(
                phoneNumbers.map { it.id },
                RequestedNumber.exactly(1)
            )
        } else {
            null
        },
        urls = if (requestedFields.containsKey(PersonaData.PersonaDataField.Kind.Url)) {
            Shared(
                urls.map { it.id },
                RequestedNumber.exactly(1)
            )
        } else {
            null
        },
        postalAddresses = if (requestedFields.containsKey(PersonaData.PersonaDataField.Kind.PostalAddress)) {
            Shared(
                postalAddresses.map { it.id },
                RequestedNumber.exactly(1)
            )
        } else {
            null
        },
        creditCards = if (requestedFields.containsKey(PersonaData.PersonaDataField.Kind.CreditCard)) {
            Shared(
                creditCards.map { it.id },
                RequestedNumber.exactly(1)
            )
        } else {
            null
        }
    )
}

fun Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.toRETDepositRule(): AccountDefaultDepositRule {
    return when (this) {
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll -> AccountDefaultDepositRule.ACCEPT
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown -> AccountDefaultDepositRule.ALLOW_EXISTING
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll -> AccountDefaultDepositRule.REJECT
    }
}

fun Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.toRETResourcePreference(): ResourcePreference {
    return when (this) {
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Allow -> ResourcePreference.ALLOWED
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Deny -> ResourcePreference.DISALLOWED
    }
}

fun Profile.hidePersona(address: String): Profile {
    val networkId = currentGateway.network.networkId()
    val updatedNetworks = networks.mapWhen(predicate = { it.networkID == networkId.value }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps.mapWhen(predicate = { authorizedDapp ->
            authorizedDapp.referencesToAuthorizedPersonas.any { it.identityAddress == address }
        }, mutation = { authorizedDapp ->
            val updatedReferences = authorizedDapp.referencesToAuthorizedPersonas.filter { it.identityAddress != address }
            authorizedDapp.copy(referencesToAuthorizedPersonas = updatedReferences)
        })
        network.copy(
            personas = network.personas.mapWhen(
                predicate = { it.address == address },
                mutation = { persona ->
                    persona.copy(flags = persona.flags + EntityFlag.DeletedByUser)
                }
            ),
            authorizedDapps = updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas.isNotEmpty() }
        )
    })
    val numberOfPersonasOnAllNetworks = updatedNetworks.sumOf { network ->
        network.personas.count { it.isNotHidden() }
    }
    val updatedContentHint = header.contentHint.copy(numberOfPersonasOnAllNetworksInTotal = numberOfPersonasOnAllNetworks)
    return copy(
        header = header.copy(contentHint = updatedContentHint),
        networks = updatedNetworks
    )
}

fun Profile.hideAccount(address: String): Profile {
    val networkId = currentGateway.network.networkId()
    val updatedNetworks = networks.mapWhen(predicate = { it.networkID == networkId.value }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps.mapWhen(predicate = { authorizedDapp ->
            authorizedDapp.referencesToAuthorizedPersonas.any { reference ->
                reference.sharedAccounts.ids.any { it == address }
            }
        }, mutation = { authorizedDapp ->
            val updatedReferences =
                authorizedDapp.referencesToAuthorizedPersonas.filter { reference ->
                    reference.sharedAccounts.ids.none { it == address }
                }
            authorizedDapp.copy(referencesToAuthorizedPersonas = updatedReferences)
        })
        val updatedAccounts = network.accounts.mapWhen(
            predicate = { it.address == address },
            mutation = { account ->
                account.copy(flags = account.flags + EntityFlag.DeletedByUser)
            }
        )
        network.copy(
            accounts = updatedAccounts,
            authorizedDapps = updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas.isNotEmpty() }
        )
    })
    val numberOfAccountsOnAllNetworks = updatedNetworks.sumOf { network ->
        network.accounts.count { it.isNotHidden() }
    }
    val updatedContentHint = header.contentHint.copy(numberOfAccountsOnAllNetworksInTotal = numberOfAccountsOnAllNetworks)
    return copy(
        header = header.copy(contentHint = updatedContentHint),
        networks = updatedNetworks
    )
}

fun Profile.unhideAllEntities(): Profile {
    val networkId = currentGateway.network.networkId()
    val updatedNetworks = networks.mapWhen(predicate = { it.networkID == networkId.value }, mutation = { network ->
        network.copy(
            personas = network.personas.map { persona ->
                persona.copy(flags = persona.flags - EntityFlag.DeletedByUser)
            },
            accounts = network.accounts.map { persona ->
                persona.copy(flags = persona.flags - EntityFlag.DeletedByUser)
            }
        )
    })
    val numberOfAccountsOnAllNetworks = updatedNetworks.sumOf { network ->
        network.accounts.count { it.isNotHidden() }
    }
    val numberOfPersonasOnAllNetworks = updatedNetworks.sumOf { network ->
        network.personas.count { it.isNotHidden() }
    }
    val updatedContentHint = header.contentHint.copy(
        numberOfAccountsOnAllNetworksInTotal = numberOfAccountsOnAllNetworks,
        numberOfPersonasOnAllNetworksInTotal = numberOfPersonasOnAllNetworks
    )
    return copy(
        header = header.copy(contentHint = updatedContentHint),
        networks = updatedNetworks
    )
}

fun Entity.isNotHidden(): Boolean {
    return flags.contains(EntityFlag.DeletedByUser).not()
}

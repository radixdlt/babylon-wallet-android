@file:Suppress("TooManyFunctions")

package rdx.works.profile.data.utils

import com.radixdlt.ret.AccountDefaultDepositRule
import com.radixdlt.ret.ResourcePreference
import rdx.works.core.InstantGenerator
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.currentGateway
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

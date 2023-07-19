package rdx.works.profile.data.utils

import rdx.works.core.InstantGenerator
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.factorsources.WasNotDeviceFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.IdentifiedEntry
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.RequestedNumber
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.Shared
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId

fun Profile.updateLastUsed(id: FactorSource.FactorSourceID): Profile {
    return copy(
        factorSources = this.factorSources.mapWhen(predicate = { it.id == id }) { factorSource ->
            factorSource.common.lastUsedOn = InstantGenerator()
            factorSource
        }
    )
}

fun Network.Account.isOlympiaAccount(): Boolean {
    return (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl
        ?.transactionSigning?.publicKey?.curve == Slip10Curve.SECP_256K1
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

fun List<Network.NextDerivationIndices>?.getNextAccountDerivationIndex(forNetworkId: NetworkId): Int {
    if (this == null) throw WasNotDeviceFactorSource()

    return this.find {
        it.networkId == forNetworkId.value
    }?.forAccount ?: 0
}

fun List<Network.NextDerivationIndices>?.getNextIdentityDerivationIndex(forNetworkId: NetworkId): Int {
    if (this == null) throw WasNotDeviceFactorSource()

    return this.find {
        it.networkId == forNetworkId.value
    }?.forIdentity ?: 0
}

fun LedgerHardwareWalletFactorSource.getNextDerivationPathForAccount(
    networkId: NetworkId
): DerivationPath {
    val index = nextDerivationIndicesPerNetwork?.find {
        it.networkId == networkId.value
    }?.forAccount ?: 0

    return DerivationPath.forAccount(
        networkId = networkId,
        accountIndex = index,
        keyType = KeyType.TRANSACTION_SIGNING
    )
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

package rdx.works.profile.data.model.extensions

import rdx.works.core.InstantGenerator
import rdx.works.core.mapWhen
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Transaction
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceFlag
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.RequestedNumber
import rdx.works.profile.data.model.pernetwork.Shared

fun Profile.updateLastUsed(id: FactorSource.FactorSourceID): Profile {
    return copy(
        factorSources = this.factorSources.mapWhen(predicate = { it.id == id }) { factorSource ->
            factorSource.common.lastUsedOn = InstantGenerator()
            factorSource
        }.toIdentifiedArrayList()
    )
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

fun Profile.addMainBabylonDeviceFactorSource(
    mainBabylonFactorSource: DeviceFactorSource
): Profile {
    val existingBabylonDeviceFactorSources = factorSources
        .mapWhen(
            predicate = { factorSource -> factorSource is DeviceFactorSource && factorSource.supportsBabylon }
        ) { deviceBabylonFactorSource ->
            (deviceBabylonFactorSource as DeviceFactorSource).copy(
                common = deviceBabylonFactorSource.common.copy(
                    flags = deviceBabylonFactorSource.common.flags.filterNot { it == FactorSourceFlag.Main }
                )
            )
        }

    return copy(
        factorSources = (listOf(mainBabylonFactorSource) + existingBabylonDeviceFactorSources).toIdentifiedArrayList()
    )
}

fun Profile.mainBabylonFactorSource(): DeviceFactorSource? {
    val deviceFactorSources = factorSources.filterIsInstance<DeviceFactorSource>()
    val babylonFactorSources = deviceFactorSources.filter { it.supportsBabylon }
    return if (babylonFactorSources.size == 1) {
        babylonFactorSources.first()
    } else {
        babylonFactorSources.firstOrNull { it.common.flags.contains(FactorSourceFlag.Main) }
    }
}

fun Profile.isCurrentNetworkMainnet(): Boolean {
    return currentNetwork.networkID == Radix.Network.mainnet.id
}

package rdx.works.core.sargon

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSourceFlag
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileNetwork
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.profileToDebugString
import rdx.works.core.annotations.DebugOnly

val Profile.currentGateway: Gateway
    get() = appPreferences.gateways.current

val Profile.currentNetwork: ProfileNetwork?
    get() {
        val currentGateway = currentGateway
        return networks.find { it.id == currentGateway.network.id }
    }

val Profile.isCurrentNetworkMainnet: Boolean
    get() = currentNetwork?.id == NetworkId.MAINNET

val Profile.activeAccountsOnCurrentNetwork: List<Account>
    get() = currentNetwork?.accounts()?.notHiddenAccounts().orEmpty()

fun Profile.activeAccountOnCurrentNetwork(withAddress: AccountAddress): Account? =
    activeAccountsOnCurrentNetwork.firstOrNull { account ->
        account.address == withAddress
    }

val Profile.hiddenAccountsOnCurrentNetwork: List<Account>
    get() = currentNetwork?.accounts()?.hiddenAccounts().orEmpty()

val Profile.activePersonasOnCurrentNetwork: List<Persona>
    get() = currentNetwork?.personas()?.notHiddenPersonas().orEmpty()

val Profile.hiddenPersonasOnCurrentNetwork: List<Persona>
    get() = currentNetwork?.personas()?.hiddenPersonas().orEmpty()

val Profile.allPersonasOnCurrentNetwork: List<Persona>
    get() = currentNetwork?.personas().orEmpty()

fun Profile.activePersonaOnCurrentNetwork(withAddress: IdentityAddress): Persona? =
    activePersonasOnCurrentNetwork.firstOrNull { persona ->
        persona.address == withAddress
    }

val Profile.deviceFactorSources: List<DeviceFactorSource>
    get() = factorSources().filterIsInstance<DeviceFactorSource>()

val Profile.ledgerFactorSources: List<LedgerHardwareWalletFactorSource>
    get() = factorSources().filterIsInstance<LedgerHardwareWalletFactorSource>()

fun Profile.factorSourceById(id: FactorSourceId) = factorSources.getById(id = id)

fun Profile.deviceFactorSourceById(id: FactorSourceId) = deviceFactorSources.getById(id = id)

fun Profile.factorSourceByIdValue(value: Exactly32Bytes) = factorSources.getByIdValue(value = value)

val Profile.deviceFactorSourcesWithAccounts: Map<DeviceFactorSource, List<Account>>
    get() {
        val allAccountsOnNetwork = activeAccountsOnCurrentNetwork
        return deviceFactorSources.associateWith { deviceFactorSource ->
            allAccountsOnNetwork.filter { it.address.string == deviceFactorSource.id.body.hex } // TODO integration check
        }
    }

val Profile.mainBabylonFactorSource: DeviceFactorSource?
    get() {
        val babylonFactorSources = deviceFactorSources.filter { it.isBabylonDeviceFactorSource }
        return if (babylonFactorSources.size == 1) {
            babylonFactorSources.first()
        } else {
            babylonFactorSources.firstOrNull { it.common.flags.contains(FactorSourceFlag.MAIN) }
        }
    }

val Profile.babylonFactorSourcesWithAccounts: Map<DeviceFactorSource, List<Account>>
    get() = deviceFactorSourcesWithAccounts.filter { entry ->
        entry.key.isBabylonDeviceFactorSource
    }.mapValues { entry ->
        entry.value.filter { account -> account.usesEd25519 }
    }

val Profile.olympiaFactorSourcesWithAccounts: Map<DeviceFactorSource, List<Account>>
    get() = deviceFactorSourcesWithAccounts.filter { entry ->
        entry.key.supportsOlympia
    }.mapValues { entry ->
        entry.value.filter { account -> account.usesSECP256k1 }
    }


/**
 * Used only by debug features, like inspect profile, where it is the only place
 * in UI where the profile's json needs to be previewed.
 */
@DebugOnly
fun Profile.prettyPrinted(): String = profileToDebugString(profile = this)

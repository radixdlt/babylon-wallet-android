package rdx.works.profile.data.model

import rdx.works.core.IdentifiedArrayList
import rdx.works.core.emptyIdentifiedArrayList
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.apppreferences.Transaction
import rdx.works.profile.data.model.extensions.isHidden
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceFlag
import rdx.works.profile.data.model.pernetwork.Network
import java.time.Instant

data class Profile(
    val header: Header,

    /**
     * Settings for this profile in the app, contains default security configs as well as display settings.
     */
    val appPreferences: AppPreferences,

    /**
     * The known sources of factors, used for authorization such as spending funds.
     * Always contains at least one DeviceFactorSource.
     */
    val factorSources: IdentifiedArrayList<FactorSource>,

    /**
     * Effectively **per network**: a list of accounts, personas and connected dApps.
     */
    val networks: List<Network>,
) {

    internal fun snapshot(): ProfileSnapshot {
        return ProfileSnapshot(
            header = header,
            appPreferences = appPreferences,
            factorSources = factorSources,
            networks = networks
        )
    }

    internal fun withUpdatedContentHint() = copy(
        header = header.copy(
            contentHint = Header.ContentHint(
                numberOfNetworks = networks.size,
                numberOfAccountsOnAllNetworksInTotal = networks.sumOf { network -> network.accounts.count { it.isHidden().not() } },
                numberOfPersonasOnAllNetworksInTotal = networks.sumOf { network -> network.personas.count { it.isHidden().not() } }
            )
        )
    )

    /**
     * Temporarily the only factor source that the user can use to create accounts/personas.
     * When new UI is added that allows the user to import other factor sources
     * (like an Olympia device factor source), we will need to revisit this.
     *
     * NOTE that this factor source will always be used when creating the first account.
     */
    val babylonMainDeviceFactorSource: DeviceFactorSource
        get() = factorSources
            .filterIsInstance<DeviceFactorSource>()
            .firstOrNull { deviceFactorSource ->
                deviceFactorSource.isBabylon && deviceFactorSource.common.flags.any { it == FactorSourceFlag.Main }
            } ?: factorSources
            .filterIsInstance<DeviceFactorSource>()
            .first { deviceFactorSource ->
                deviceFactorSource.isBabylon
            }

    val babylonDeviceFactorSourceExist: Boolean
        get() = factorSources
            .filterIsInstance<DeviceFactorSource>()
            .any {
                it.isBabylon
            }

    companion object {
        fun init(
            id: String,
            deviceInfo: DeviceInfo,
            creationDate: Instant,
            gateways: Gateways = Gateways.preset
        ): Profile {
            val networks = listOf(
                Network(
                    accounts = listOf(),
                    authorizedDapps = listOf(),
                    networkID = gateways.current().network.id,
                    personas = listOf()
                )
            )

            val appPreferences = AppPreferences(
                transaction = Transaction.default,
                display = Display.default,
                security = Security.default,
                gateways = gateways,
                p2pLinks = listOf()
            )

            return Profile(
                header = Header.init(
                    id = id,
                    deviceInfo = deviceInfo,
                    creationDate = creationDate,
                    numberOfNetworks = networks.size
                ),
                appPreferences = appPreferences,
                factorSources = emptyIdentifiedArrayList(),
                networks = networks
            )
        }
    }
}

val Profile.currentGateway: Radix.Gateway
    get() = appPreferences.gateways.current()

val Profile.currentNetwork: Network
    get() {
        val currentGateway = currentGateway

        return networks.find { it.networkID == currentGateway.network.id } ?: error(
            "No per-network found for gateway: $currentGateway. This should not happen $networks"
        )
    }

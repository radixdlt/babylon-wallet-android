package rdx.works.profile.data.model

import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.apppreferences.Transaction
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.utils.isNotHidden
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
    val factorSources: List<FactorSource>,

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
                numberOfAccountsOnAllNetworksInTotal = networks.sumOf { network -> network.accounts.count { it.isNotHidden() } },
                numberOfPersonasOnAllNetworksInTotal = networks.sumOf { network -> network.personas.count { it.isNotHidden() } }
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
    val babylonDeviceFactorSource: DeviceFactorSource
        get() = factorSources
            .filterIsInstance<DeviceFactorSource>()
            .first {
                it.common.cryptoParameters == FactorSource.Common.CryptoParameters.babylon
            }

    val babylonDeviceFactorSourceExist: Boolean
        get() = factorSources
            .filterIsInstance<DeviceFactorSource>()
            .any {
                it.common.cryptoParameters == FactorSource.Common.CryptoParameters.babylon
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
                factorSources = listOf(),
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

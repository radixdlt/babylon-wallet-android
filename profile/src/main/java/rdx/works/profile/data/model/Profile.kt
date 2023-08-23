package rdx.works.profile.data.model

import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.apppreferences.Transaction
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.factorsources.Slip10Curve.CURVE_25519
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
                numberOfAccountsOnAllNetworksInTotal = networks.map { it.accounts }.flatten().size,
                numberOfPersonasOnAllNetworksInTotal = networks.map { it.personas }.flatten().size
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
                it.common.cryptoParameters.supportedCurves.contains(CURVE_25519)
            }

    val olympiaDeviceFactorSource: FactorSource
        get() = factorSources
            .filterIsInstance<DeviceFactorSource>()
            .first {
                it.common.cryptoParameters.supportedCurves.contains(Slip10Curve.SECP_256K1)
            }

    companion object {
        @Suppress("LongParameterList")
        fun init(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            id: String,
            deviceModel: String,
            deviceName: String,
            creationDate: Instant,
            gateway: Radix.Gateway = Radix.Gateway.default
        ): Profile {
            val factorSource = DeviceFactorSource.babylon(
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                model = deviceModel,
                name = deviceName,
                createdAt = creationDate
            )

            val networks = listOf(
                Network(
                    accounts = listOf(),
                    authorizedDapps = listOf(),
                    networkID = gateway.network.id,
                    personas = listOf()
                )
            )

            val appPreferences = AppPreferences(
                transaction = Transaction.default,
                display = Display.default,
                security = Security.default,
                gateways = Gateways.fromCurrent(current = gateway),
                p2pLinks = listOf()
            )

            return Profile(
                header = Header.init(
                    id = id,
                    deviceName = deviceName,
                    creationDate = creationDate,
                    numberOfNetworks = networks.size
                ),
                appPreferences = appPreferences,
                factorSources = listOf(factorSource),
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
            "No per-network found for gateway: $currentGateway. This should not happen"
        )
    }

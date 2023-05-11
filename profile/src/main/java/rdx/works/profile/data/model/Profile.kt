package rdx.works.profile.data.model

import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.Slip10Curve.CURVE_25519
import rdx.works.profile.data.model.factorsources.Slip10Curve.SECP_256K1
import rdx.works.profile.data.model.pernetwork.Network

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

    /**
     * Temporarily the only factor source that the user can use to create accounts/personas.
     * When new UI is added that allows the user to import other factor sources
     * (like an Olympia device factor source), we will need to revisit this.
     *
     * NOTE that this factor source will always be used when creating the first account.
     */
    val babylonDeviceFactorSource: FactorSource
        get() = factorSources.first {
            it.kind == FactorSourceKind.DEVICE && it.parameters.supportedCurves.contains(CURVE_25519)
        }

    val olympiaDeviceFactorSource: FactorSource
        get() = factorSources.first {
            it.kind == FactorSourceKind.DEVICE && it.parameters.supportedCurves.contains(SECP_256K1)
        }

    companion object {
        fun init(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            header: Header,
            gateway: Radix.Gateway = Radix.Gateway.default
        ): Profile {
            val factorSource = FactorSource.babylon(
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                label = header.creatingDevice
            )

            val mainNetwork = Network(
                accounts = listOf(),
                authorizedDapps = listOf(),
                networkID = gateway.network.id,
                personas = listOf()
            )

            val appPreferences = AppPreferences(
                display = Display.default,
                security = Security.default,
                gateways = Gateways.fromCurrent(current = gateway),
                p2pLinks = listOf()
            )

            return Profile(
                header = header,
                appPreferences = appPreferences,
                factorSources = listOf(factorSource),
                networks = listOf(mainNetwork)
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

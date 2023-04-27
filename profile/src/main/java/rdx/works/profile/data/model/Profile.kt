package rdx.works.profile.data.model

import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.hex.extensions.toHexString
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.Slip10Curve.CURVE_25519
import rdx.works.profile.data.model.factorsources.Slip10Curve.SECP_256K1
import rdx.works.profile.data.model.pernetwork.AccountSigner
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import timber.log.Timber

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
     * Returns the account signers, currently only for accounts that their factor instances derive
     * from [FactorSourceKind.DEVICE] factor sources. Note that those instances also have
     * a non-null derivation path.
     */
    inline fun getAccountSigners(
        addresses: List<String>,
        networkId: Int,
        getMnemonic: (FactorSource) -> MnemonicWithPassphrase
    ): List<AccountSigner> {
        val network = networks.firstOrNull { network ->
            network.networkID == networkId
        } ?: return emptyList()

        val accounts = if (addresses.isEmpty()) {
            listOf(network.accounts.first())
        } else {
            addresses.mapNotNull { address ->
                network.accounts.find { it.address == address }
            }
        }

        return accounts.map { account ->
            when (val securityState = account.securityState) {
                is SecurityState.Unsecured -> {
                    val factorInstance = securityState.unsecuredEntityControl.genesisFactorInstance

                    val factorSource = factorSources.find {
                        it.id == factorInstance.factorSourceId
                    }

                    if (factorSource == null) {
                        Timber.w("No FactorSource found with id ${factorInstance.factorSourceId}")
                        return@map null
                    }

                    if (factorSource.kind != FactorSourceKind.DEVICE) {
                        Timber.w("No FactorSource with DEVICE kind was found, but the account requested for a non-DEVICE factor source")
                        return@map null
                    }

                    if (!factorSource.supportsCurve(factorInstance.publicKey.curve)) {
                        Timber.w("The curve ${factorInstance.publicKey.curve} is not supported by the selected FactorSource")
                        return@map null
                    }

                    val mnemonicWithPassphrase = getMnemonic(factorSource)
                    val extendedKey = mnemonicWithPassphrase.deriveExtendedKey(
                        factorInstance = factorInstance
                    )
                    if (
                        extendedKey.keyPair
                            .getCompressedPublicKey()
                            .removeLeadingZero()
                            .toHexString() != factorInstance.publicKey.compressedData
                    ) {
                        Timber.w("FactorSource's public key does not match with the derived public key")
                        return@map null
                    }

                    AccountSigner(
                        account = account,
                        privateKey = extendedKey.keyPair.privateKey
                    )
                }
            }
        }.filterNotNull()
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

internal val Profile.currentNetwork: Network
    get() {
        val currentGateway = currentGateway

        return networks.find { it.networkID == currentGateway.network.id } ?: error(
            "No per-network found for gateway: $currentGateway. This should not happen"
        )
    }

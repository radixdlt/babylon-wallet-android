package rdx.works.profile.data.model

import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.hex.extensions.toHexString
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.Profile.Companion.equals
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.Slip10Curve.CURVE_25519
import rdx.works.profile.data.model.pernetwork.AccountSigner
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.incrementFactorSourceNextAccountIndex
import timber.log.Timber

data class Profile(
    /**
     * A locally generated stable identifier of this Profile. Useful for checking if
     * two [Profile]s which are unequal based on [equals] (content) might be
     * semantically the same, based on the ID.
     */
    val id: String,

    /**
     * A description of the device the Profile was first generated on,
     * typically the wallet app reads a human provided device name
     * if present and able, and/or a model description of the device e.g:
     * `"Galaxy A53 5G (Samsung SM-A536B)"`
     * This string can be presented to the user during a recovery flow,
     * when the profile is restored from backup.
     *
     * This string is as constructed from [DeviceInfo] will be formed firt by the user's generated
     * device name followed by the device's manufacturer and the device's factory model.
     */
    val creatingDevice: String,

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
    val networks: List<OnNetwork>,

    /**
     * A version of the Profile Snapshot data format used for compatibility checks.
     */
    val version: Int
) {

    internal fun snapshot(): ProfileSnapshot {
        return ProfileSnapshot(
            id = id,
            creatingDevice = creatingDevice,
            appPreferences = appPreferences,
            factorSources = factorSources,
            networks = networks,
            version = version
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

    companion object {
        const val LATEST_PROFILE_VERSION = 24
        private const val GENERIC_ANDROID_DEVICE_PLACEHOLDER = "Android Phone"

        fun init(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            firstAccountDisplayName: String,
            creatingDevice: String = GENERIC_ANDROID_DEVICE_PLACEHOLDER
        ): Profile {
            val gateway = Gateway.default

            val factorSource = FactorSource.babylon(
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                hint = creatingDevice
            )

            val initialAccount = OnNetwork.Account.init(
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                factorSource = factorSource,
                networkId = gateway.network.networkId(),
                displayName = firstAccountDisplayName
            )

            val mainNetwork = OnNetwork(
                accounts = listOf(initialAccount),
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
                id = UUIDGenerator.uuid().toString(),
                creatingDevice = creatingDevice,
                appPreferences = appPreferences,
                factorSources = listOf(factorSource),
                networks = listOf(mainNetwork),
                version = LATEST_PROFILE_VERSION
            ).incrementFactorSourceNextAccountIndex(
                forNetwork = gateway.network.networkId(),
                factorSourceId = factorSource.id
            )
        }
    }
}

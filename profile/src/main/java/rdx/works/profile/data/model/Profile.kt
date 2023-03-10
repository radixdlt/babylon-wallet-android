package rdx.works.profile.data.model

import com.radixdlt.bip39.model.MnemonicWords
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.OnNetwork

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
    val factorSources: FactorSources,

    /**
     * Effectively **per network**: a list of accounts, personas and connected dApps.
     */
    val onNetwork: List<OnNetwork>,

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
            onNetwork = onNetwork,
            version = version
        )
    }

    fun notaryFactorSource():
        FactorSources.Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource {
        return factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.first()
    }

    companion object {
        const val LATEST_PROFILE_VERSION = 19
        private const val GENERIC_ANDROID_DEVICE_PLACEHOLDER = "Android Phone"

        fun init(
            gateway: Gateway,
            mnemonic: MnemonicWords,
            firstAccountDisplayName: String,
            creatingDevice: String = GENERIC_ANDROID_DEVICE_PLACEHOLDER
        ): Profile {
            val curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource =
                FactorSources.Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource
                    .deviceFactorSource(
                        mnemonic = mnemonic,
                        label = firstAccountDisplayName
                    )

            val network = gateway.network

            val factorSources = FactorSources(
                curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources = listOf(
                    curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource
                ),
                secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources = listOf()
            )

            val initialAccount = OnNetwork.Account.initial(
                mnemonic = mnemonic,
                factorSources = factorSources,
                networkId = network.networkId(),
                displayName = firstAccountDisplayName
            )

            val mainNetwork = OnNetwork(
                accounts = listOf(initialAccount),
                authorizedDapps = listOf(),
                networkID = network.id,
                personas = listOf()
            )

            val appPreferences = AppPreferences(
                display = Display.default,
                gateways = Gateways(gateway.url, listOf(gateway)),
                p2pClients = emptyList()
            )

            return Profile(
                id = UUIDGenerator.uuid().toString(),
                creatingDevice = creatingDevice,
                appPreferences = appPreferences,
                factorSources = factorSources,
                onNetwork = listOf(mainNetwork),
                version = LATEST_PROFILE_VERSION
            )
        }
    }
}

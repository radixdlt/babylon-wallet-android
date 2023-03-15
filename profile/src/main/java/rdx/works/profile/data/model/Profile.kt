package rdx.works.profile.data.model

import com.radixdlt.bip39.model.MnemonicWords
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.extensions.incrementFactorSourceNextAccountIndex
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.factorsources.FactorSource
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
    val factorSources: List<FactorSource>,

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

    // TODO(ABW-1023)
    fun notaryFactorSource() = factorSources.firstOrNull()

    companion object {
        const val LATEST_PROFILE_VERSION = 20
        private const val GENERIC_ANDROID_DEVICE_PLACEHOLDER = "Android Phone"

        fun init(
            mnemonic: MnemonicWords,
            firstAccountDisplayName: String,
            creatingDevice: String = GENERIC_ANDROID_DEVICE_PLACEHOLDER
        ): Profile {
            val gateway = Gateway.default

            val factorSource = FactorSource.babylon(
                mnemonic = mnemonic,
                hint = creatingDevice
            )

            val initialAccount = OnNetwork.Account.initial(
                mnemonic = mnemonic,
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
                gateways = Gateways.fromCurrent(current = gateway),
                p2pClients = listOf()
            )

            return Profile(
                id = UUIDGenerator.uuid().toString(),
                creatingDevice = creatingDevice,
                appPreferences = appPreferences,
                factorSources = listOf(factorSource),
                onNetwork = listOf(mainNetwork),
                version = LATEST_PROFILE_VERSION
            ).incrementFactorSourceNextAccountIndex(
                forNetwork = gateway.network.networkId(),
                factorSourceId = factorSource.id
            )
        }
    }
}

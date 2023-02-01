package rdx.works.profile.data.model

import com.radixdlt.bip39.model.MnemonicWords
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.OnNetwork

data class Profile(
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
     * Incrementing from 1
     */
    val version: Int
) {

    internal fun snapshot(): ProfileSnapshot {
        return ProfileSnapshot(
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
        private const val INITIAL_VERSION = 1
        fun init(
            networkAndGateway: NetworkAndGateway,
            mnemonic: MnemonicWords,
            firstAccountDisplayName: String
        ): Profile {
            val curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource =
                FactorSources.Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource
                    .deviceFactorSource(
                        mnemonic = mnemonic,
                        label = firstAccountDisplayName
                    )

            val network = networkAndGateway.network

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
                connectedDapps = listOf(),
                networkID = network.id,
                personas = listOf()
            )

            val appPreferences = AppPreferences(
                display = Display.default,
                networkAndGateway = networkAndGateway,
                p2pClients = emptyList()
            )

            return Profile(
                appPreferences = appPreferences,
                factorSources = factorSources,
                onNetwork = listOf(mainNetwork),
                version = INITIAL_VERSION
            )
        }
    }
}

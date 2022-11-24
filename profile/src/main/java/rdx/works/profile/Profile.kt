package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.model.apppreferences.AppPreferences
import rdx.works.profile.model.apppreferences.Display
import rdx.works.profile.model.apppreferences.NetworkAndGateway
import rdx.works.profile.model.apppreferences.P2PClients
import rdx.works.profile.model.factorsources.FactorSources
import rdx.works.profile.model.pernetwork.Account
import rdx.works.profile.model.pernetwork.PerNetwork

@Serializable
data class Profile(
    /**
     * Settings for this profile in the app, contains default security configs as well as display settings.
     */
    @SerialName("appPreferences")
    val appPreferences: AppPreferences,

    /**
     * The known sources of factors, used for authorization such as spending funds.
     * Always contains at least one DeviceFactorSource.
     */
    @SerialName("factorSources")
    val factorSources: FactorSources,

    /**
     * Effectively **per network**: a list of accounts, personas and connected dApps.
     */
    @SerialName("perNetwork")
    val perNetwork: List<PerNetwork>
) {

    companion object {
        fun init(
            networkAndGateway: NetworkAndGateway,
            mnemonic: MnemonicWords,
            firstAccountDisplayName: String
        ): Profile {

            val curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource =
                FactorSources.Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource.deviceFactorSource(
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

            val initialAccount = Account.initial(
                mnemonic = mnemonic,
                factorSources = factorSources,
                perNetwork = emptyList(),
                networkId = network.networkId()
            )

            val mainNetwork = PerNetwork(
                accounts = listOf(initialAccount),
                connectedDapps = listOf(),
                networkID = network.id,
                personas = listOf()
            )

            val appPreferences = AppPreferences(
                display = Display.default,
                networkAndGateway = networkAndGateway,
                p2pClients = P2PClients(
                    connections = emptyList()
                )
            )

            return Profile(
                appPreferences = appPreferences,
                factorSources = factorSources,
                perNetwork = listOf(mainNetwork)
            )
        }
    }
}

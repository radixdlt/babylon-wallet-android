package rdx.works.profile.model.pernetwork

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.AccountDerivationPath
import rdx.works.profile.CompressedPublicKey
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.enginetoolkit.EngineToolkitImpl
import rdx.works.profile.model.factorsources.FactorSources

@Serializable
data class Account(
    /**
     * The globally unique and identifiable Radix component address of this account. Can be used as
     * a stable ID. Cryptographically derived from a seeding public key which typically was created by
     * the `DeviceFactorSource` (and typically the same public key as an instance of the device factor
     * typically used in the primary role of this account).
     */
    @SerialName("address")
    val address: EntityAddress,

    /**
     * An identifier for the gradient for this account, to be displayed in wallet
     * and possibly by dApps.
     */
    @SerialName("appearanceID")
    val appearanceID: Int,

    /**
     * The SLIP10 compatible Hierarchical Deterministic derivation path which is used to derive
     * the public keys of the factors of the different roles, if the factor source kind of said factor
     * instance supports Hierarchical Deterministic derivation.
     */
    @SerialName("derivationPath")
    val derivationPath: String,

    /**
     * An optional displayName or label, used by presentation layer only.
     */
    @SerialName("displayName")
    val displayName: String?,

    /**
     * The index of this account, in the list of accounts for a certain network. This means that
     * profile on network `mainnet` will have an account with `accountIndex = 0`, but so can an
     * account on network `testnet` too! However, their `address`es will differ!
     */
    @SerialName("index")
    val index: Int,

    /**
     * Security of this account
     */
    @SerialName("securityState")
    val securityState: SecurityState
) {
    companion object {
        private const val INITIAL_ACCOUNT_NAME = "Main"
        private const val INITIAL_ACCOUNT_INDEX = 0
        /**
         * Creates initial account upon new profile creation
         */
        fun initial(
            mnemonic: MnemonicWords,
            factorSources: FactorSources,
            perNetwork: List<PerNetwork>,
            networkId: NetworkId
        ): Account {
            val derivationPath = AccountDerivationPath(
                perNetwork = perNetwork,
                networkId = networkId
            ).path()
            val compressedPublicKey = CompressedPublicKey(
                mnemonic = mnemonic
            ).derive(derivationPath)

            return Account(
                address = EngineToolkitImpl().deriveAddress(compressedPublicKey),
                appearanceID = 0,
                derivationPath = derivationPath,
                displayName = INITIAL_ACCOUNT_NAME,
                index = INITIAL_ACCOUNT_INDEX,
                securityState = SecurityState.unsecuredSecurityState(
                    compressedPublicKey = compressedPublicKey,
                    derivationPath =  DerivationPath.accountDerivationPath(
                        derivationPath = derivationPath
                    ),
                    factorSources = factorSources
                )
            )
        }
    }
}
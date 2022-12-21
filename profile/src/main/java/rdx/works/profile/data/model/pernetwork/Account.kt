package rdx.works.profile.data.model.pernetwork

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.extensions.removeLeadingZero
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import models.crypto.PublicKey
import rdx.works.profile.data.extensions.compressedPublicKey
import rdx.works.profile.data.extensions.deriveAddress
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.SecurityState.Unsecured.Companion.unsecuredSecurityState
import rdx.works.profile.data.repository.AccountDerivationPath
import rdx.works.profile.data.repository.EntityDerivationPath
import rdx.works.profile.derivation.model.NetworkId

@Serializable
data class Account(
    /**
     * The globally unique and identifiable Radix component address of this account. Can be used as
     * a stable ID. Cryptographically derived from a seeding public key which typically was created by
     * the `DeviceFactorSource` (and typically the same public key as an instance of the device factor
     * typically used in the primary role of this account).
     */
    @SerialName("address")
    val entityAddress: EntityAddress,

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
    val securityState: SecurityState.Unsecured
) {
    enum class AppearanceIdGradient {
        Gradient1,
        Gradient2,
        Gradient3,
        Gradient4,
        Gradient5,
        Gradient6,
        Gradient7,
        Gradient8,
        Gradient9,
        Gradient10,
        Gradient11,
        Gradient12,
    }

    companion object {
        /**
         * Creates initial account upon new profile creation
         */
        fun initial(
            mnemonic: MnemonicWords,
            factorSources: FactorSources,
            networkId: NetworkId,
            displayName: String
        ): Account {
            return createNewVirtualAccount(
                displayName = displayName,
                entityDerivationPath = AccountDerivationPath(
                    perNetwork = emptyList(),
                    networkId = networkId
                ),
                entityIndex = 0,
                mnemonic = mnemonic,
                factorSources = factorSources,
                networkId = networkId
            )
        }
    }
}

fun createNewVirtualAccount(
    displayName: String,
    entityDerivationPath: EntityDerivationPath,
    entityIndex: Int,
    mnemonic: MnemonicWords,
    factorSources: FactorSources,
    networkId: NetworkId
): Account {
    val derivationPath = entityDerivationPath.path()

    val compressedPublicKey = mnemonic.compressedPublicKey(
        derivationPath = derivationPath
    )
    val publicKey = PublicKey.EddsaEd25519(
        compressedPublicKey.removeLeadingZero()
    )
    val address = deriveAddress(
        networkID = networkId,
        publicKey = publicKey
    )

    val unsecuredSecurityState = unsecuredSecurityState(
        compressedPublicKey = compressedPublicKey,
        derivationPath = DerivationPath.accountDerivationPath(
            derivationPath = derivationPath
        ),
        factorSources = factorSources
    )

    return Account(
        entityAddress = address,
        appearanceID = entityIndex % Account.AppearanceIdGradient.values().count(),
        derivationPath = derivationPath,
        displayName = displayName,
        index = entityIndex,
        securityState = unsecuredSecurityState
    )
}

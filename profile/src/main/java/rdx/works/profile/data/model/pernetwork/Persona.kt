package rdx.works.profile.data.model.pernetwork

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.toolkit.models.crypto.PublicKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.extensions.compressedPublicKey
import rdx.works.profile.data.extensions.deriveAddress
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.SecurityState.Unsecured.Companion.unsecuredSecurityState
import rdx.works.profile.data.repository.IdentityDerivationPath
import rdx.works.profile.derivation.model.NetworkId

/**
 * Persona is very similar to Account but it adds fields
 */
@Serializable
data class Persona(
    /**
     * The globally unique and identifiable Radix component address of this persona. Can be used as
     * a stable ID. Cryptographically derived from a seeding public key which typically was created by
     * the `DeviceFactorSource` (and typically the same public key as an instance of the device factor
     * typically used in the primary role of this persona).
     */
    @SerialName("address")
    val entityAddress: EntityAddress,

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

    @SerialName("fields")
    val fields: List<PersonaField>,

    /**
     * The index of this persona, in the list of personas for a certain network. This means that
     * profile on network `mainnet` will have a persona with `accountIndex = 0`, but so can person
     * on network `testnet` too! However, their `address`es will differ!
     */
    @SerialName("index")
    val index: Int,

    /**
     * The ID of the network that has been used to generate the accounts, to which personas
     * have been added and dApps connected.
     */
    @SerialName("networkID")
    val networkID: Int,

    /**
     * Security of this persona
     */
    @SerialName("securityState")
    val securityState: SecurityState.Unsecured
)

@Suppress("LongParameterList") // TODO refine this later on
fun createNewPersona(
    displayName: String,
    fields: List<PersonaField>,
    entityIndex: Int,
    mnemonicWords: MnemonicWords,
    factorSources: FactorSources,
    networkId: NetworkId
): Persona {
    val derivationPath = IdentityDerivationPath(
        entityIndex = entityIndex,
        networkId = networkId
    ).path()

    val compressedPublicKey = mnemonicWords.compressedPublicKey(
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
        derivationPath = DerivationPath.identityDerivationPath(
            derivationPath = derivationPath
        ),
        factorSources = factorSources
    )

    return Persona(
        entityAddress = address,
        derivationPath = derivationPath,
        displayName = displayName,
        fields = fields,
        index = entityIndex,
        networkID = networkId.value,
        securityState = unsecuredSecurityState
    )
}

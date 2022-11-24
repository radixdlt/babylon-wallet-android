package rdx.works.profile.derivation

import com.radixdlt.bip44.BIP44_PREFIX
import rdx.works.profile.derivation.model.CoinType
import rdx.works.profile.derivation.model.EntityType
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId

/**
 * The **default** derivation path used to derive `Identity` (Persona) keys for signing authentication,
 * at a certain (Persona) index (`ENTITY_INDEX`) and **unique per network** (`NETWORK_ID`) as per [CAP-26][cap26].
 *
 * Note that users can choose to use custom derivation path instead of this default one
 * when deriving keys for identities (personas).
 *
 * The format is:
 *          `m/44'/1022'/<NETWORK_ID>'/618'/<ENTITY_INDEX>'/<KEY_TYPE>'`
 *
 */
data class IdentityHDDerivationPath(
    private val networkId: NetworkId,
    private val identityIndex: Int,
    private val keyType: KeyType
) {
    private val coinType: CoinType = CoinType.RadixDlt
    private val entityType: EntityType = EntityType.Identity

    val path: String
        get() = "$BIP44_PREFIX/44'/${coinType.value}'/${networkId.value}'/${entityType.value}'/${identityIndex}'/${keyType.value}'"

}
package rdx.works.profile.data.repository

import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.EntityKind
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.HDPathValue
import com.radixdlt.sargon.extensions.account
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.nextAccountIndex
import rdx.works.core.sargon.nextPersonaIndex
import javax.inject.Inject

class PublicKeyProvider @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository
) {

    /**
     * CAP26 derivation path scheme
     */
    suspend fun getNextDerivationPathForFactorSource(
        forNetworkId: NetworkId,
        factorSource: FactorSource,
        entityKind: EntityKind = EntityKind.ACCOUNT
    ): DerivationPath {
        val profile = profileRepository.profile.first()
        val accountIndex = when (entityKind) {
            EntityKind.ACCOUNT -> profile.nextAccountIndex(
                forNetworkId = forNetworkId,
                factorSourceId = factorSource.id,
                derivationPathScheme = DerivationPathScheme.CAP26
            )

            EntityKind.PERSONA -> profile.nextPersonaIndex(
                forNetworkId = forNetworkId,
                derivationPathScheme = DerivationPathScheme.CAP26,
                factorSourceID = factorSource.id
            )
        }
        return DerivationPath.Cap26.account(
            networkId = forNetworkId,
            keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
            index = accountIndex
        )
    }

    fun getDerivationPathsForIndices(
        forNetworkId: NetworkId,
        indices: Set<HDPathValue>,
        isForLegacyOlympia: Boolean = false
    ): List<DerivationPath> {
        return indices.map { accountIndex ->
            if (isForLegacyOlympia) {
                DerivationPath.Bip44Like.init(index = accountIndex)
            } else {
                DerivationPath.Cap26.account(
                    networkId = forNetworkId,
                    keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
                    index = accountIndex,
                )
            }
        }
    }

    suspend fun deriveHDPublicKeyForDeviceFactorSource(
        deviceFactorSource: FactorSource.Device,
        derivationPath: DerivationPath
    ): Result<HierarchicalDeterministicPublicKey> = mnemonicRepository.readMnemonic(deviceFactorSource.value.id.asGeneral())
        .mapCatching { mnemonicWithPassphrase ->
            mnemonicWithPassphrase.derivePublicKey(path = derivationPath)
        }

    suspend fun derivePublicKeysDeviceFactorSource(
        deviceFactorSource: FactorSource.Device,
        derivationPaths: List<DerivationPath>
    ): Result<List<HierarchicalDeterministicPublicKey>> {
        return mnemonicRepository.readMnemonic(deviceFactorSource.value.id.asGeneral()).mapCatching { mnemonicWithPassphrase ->
            derivationPaths.map { mnemonicWithPassphrase.derivePublicKey(path = it) }
        }
    }
}

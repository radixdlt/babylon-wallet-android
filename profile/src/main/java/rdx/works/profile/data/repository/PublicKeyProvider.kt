package rdx.works.profile.data.repository

import com.radixdlt.sargon.AccountPath
import com.radixdlt.sargon.Bip44LikePath
import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.EntityKind
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.IdentityPath
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.asHardened
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
        entityKind: EntityKind = EntityKind.ACCOUNT,
        keyKind: Cap26KeyKind = Cap26KeyKind.TRANSACTION_SIGNING,
    ): DerivationPath {
        val profile = profileRepository.profile.first()
        return when (entityKind) {
            EntityKind.ACCOUNT -> {
                val accountIndex = profile.nextAccountIndex(
                    forNetworkId = forNetworkId,
                    factorSourceId = factorSource.id,
                    derivationPathScheme = DerivationPathScheme.CAP26
                )

                AccountPath.init(
                    networkId = forNetworkId,
                    keyKind = keyKind,
                    index = accountIndex.asHardened()
                ).asGeneral()
            }

            EntityKind.PERSONA -> {
                val identityIndex = profile.nextPersonaIndex(
                    forNetworkId = forNetworkId,
                    derivationPathScheme = DerivationPathScheme.CAP26,
                    factorSourceId = factorSource.id
                )

                IdentityPath.init(
                    networkId = forNetworkId,
                    keyKind = keyKind,
                    index = identityIndex.asHardened()
                ).asGeneral()
            }
        }
    }

    fun getDerivationPathsForIndices(
        forNetworkId: NetworkId,
        indices: LinkedHashSet<HdPathComponent>,
        isForLegacyOlympia: Boolean = false
    ): List<DerivationPath> {
        return indices.map { accountIndex ->
            if (isForLegacyOlympia) {
                Bip44LikePath.init(accountIndex).asGeneral()
            } else {
                AccountPath.init(
                    networkId = forNetworkId,
                    keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
                    index = accountIndex.asHardened()
                ).asGeneral()
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

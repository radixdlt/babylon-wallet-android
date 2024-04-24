package rdx.works.profile.data.repository

import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.extensions.HDPathValue
import com.radixdlt.sargon.extensions.account
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.derivePublicKey
import rdx.works.core.sargon.nextAccountIndex
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
        factorSource: FactorSource
    ): DerivationPath {
        val profile = profileRepository.profile.first()
        val accountIndex = profile.nextAccountIndex(
            forNetworkId = forNetworkId,
            factorSourceId = factorSource.id,
            derivationPathScheme = DerivationPathScheme.CAP26
        )
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

    suspend fun derivePublicKeyForDeviceFactorSource(
        deviceFactorSource: FactorSource.Device,
        derivationPath: DerivationPath
    ): Result<PublicKey> = mnemonicRepository.readMnemonic(deviceFactorSource.value.id.asGeneral()).mapCatching { mnemonicWithPassphrase ->
        mnemonicWithPassphrase.derivePublicKey(derivationPath = derivationPath)
    }

    suspend fun derivePublicKeysDeviceFactorSource(
        deviceFactorSource: FactorSource.Device,
        derivationPaths: List<DerivationPath>,
        isForLegacyOlympia: Boolean = false
    ): Result<Map<DerivationPath, PublicKey>> {
        return mnemonicRepository.readMnemonic(deviceFactorSource.value.id.asGeneral()).mapCatching { mnemonicWithPassphrase ->
            if (isForLegacyOlympia) {
                derivationPaths.associateWith { derivationPath ->
                    mnemonicWithPassphrase.derivePublicKey(
                        derivationPath = derivationPath,
                        curve = Slip10Curve.CURVE25519
                    )
                }
            } else {
                derivationPaths.associateWith { derivationPath -> mnemonicWithPassphrase.derivePublicKey(derivationPath = derivationPath) }
            }
        }
    }
}

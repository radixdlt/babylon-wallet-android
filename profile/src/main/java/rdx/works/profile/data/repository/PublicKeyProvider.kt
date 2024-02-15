package rdx.works.profile.data.repository

import com.radixdlt.extensions.removeLeadingZero
import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.extensions.nextAccountIndex
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId
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
        val accountIndex = profile.nextAccountIndex(factorSource, DerivationPathScheme.CAP_26, forNetworkId)
        return DerivationPath.forAccount(
            networkId = forNetworkId,
            accountIndex = accountIndex,
            keyType = KeyType.TRANSACTION_SIGNING
        )
    }

    fun getDerivationPathsForIndices(
        forNetworkId: NetworkId,
        indices: Set<Int>,
        isForLegacyOlympia: Boolean = false
    ): List<DerivationPath> {
        return indices.map { accountIndex ->
            if (isForLegacyOlympia) {
                DerivationPath.forLegacyOlympia(accountIndex)
            } else {
                DerivationPath.forAccount(
                    networkId = forNetworkId,
                    accountIndex = accountIndex,
                    keyType = KeyType.TRANSACTION_SIGNING
                )
            }
        }
    }

    suspend fun derivePublicKeyForDeviceFactorSource(
        deviceFactorSource: DeviceFactorSource,
        derivationPath: DerivationPath
    ): ByteArray {
        val mnemonicWithPassphrase = requireNotNull(mnemonicRepository.readMnemonic(deviceFactorSource.id).getOrNull())
        return mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath).removeLeadingZero()
    }

    suspend fun derivePublicKeysDeviceFactorSource(
        deviceFactorSource: DeviceFactorSource,
        derivationPaths: List<DerivationPath>,
        isForLegacyOlympia: Boolean = false
    ): Map<DerivationPath, ByteArray> {
        return if (isForLegacyOlympia) {
            derivationPaths.associateWith { derivationPath ->
                val mnemonicWithPassphrase = requireNotNull(mnemonicRepository.readMnemonic(deviceFactorSource.id).getOrNull())
                mnemonicWithPassphrase.compressedPublicKey(Slip10Curve.SECP_256K1, derivationPath)
            }
        } else {
            derivationPaths.associateWith { derivationPath ->
                val mnemonicWithPassphrase = requireNotNull(mnemonicRepository.readMnemonic(deviceFactorSource.id).getOrNull())
                mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath).removeLeadingZero()
            }
        }
    }
}

package rdx.works.profile.domain.account

import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.hex
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.derivePublicKey
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.core.sargon.supportsOlympia
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.di.coroutines.DefaultDispatcher
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import javax.inject.Inject

class GetFactorSourceIdForOlympiaAccountsUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(olympiaAccounts: List<OlympiaAccountDetails>): FactorSourceId.Hash? {
        return withContext(defaultDispatcher) {
            getProfileUseCase().deviceFactorSources
                .filter { deviceFactorSource ->
                    deviceFactorSource.value.common.cryptoParameters.supportsOlympia
                }
                .map { deviceFactorSource ->
                    deviceFactorSource.value.id.asGeneral()
                }
                .forEach { fromHashId ->
                    val mnemonic = mnemonicRepository.readMnemonic(fromHashId).getOrNull()
                    if (mnemonic?.validatePublicKeysOf(olympiaAccounts) == true) {
                        return@withContext fromHashId
                    }
                }

            return@withContext null
        }
    }

    private fun MnemonicWithPassphrase.validatePublicKeysOf(accounts: List<OlympiaAccountDetails>): Boolean = accounts.all {
        derivePublicKey(derivationPath = it.derivationPath, curve = Slip10Curve.SECP256K1).hex == it.publicKey.hex
    }
}

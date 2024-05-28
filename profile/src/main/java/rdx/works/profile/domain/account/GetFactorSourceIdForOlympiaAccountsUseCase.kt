package rdx.works.profile.domain.account

import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.validate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
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
    suspend operator fun invoke(olympiaAccounts: List<OlympiaAccountDetails>): Result<FactorSourceId.Hash?> {
        return withContext(defaultDispatcher) {
            val existingId = getProfileUseCase().deviceFactorSources
                .filter { deviceFactorSource ->
                    deviceFactorSource.value.common.cryptoParameters.supportsOlympia
                }
                .map { deviceFactorSource ->
                    deviceFactorSource.value.id.asGeneral()
                }
                .find { fromHashId ->
                    val readMnemonicResult = mnemonicRepository.readMnemonic(fromHashId)
                    readMnemonicResult.getOrNull()?.validatePublicKeysOf(olympiaAccounts) == true
                }?.value?.asGeneral()
            Result.success(existingId)
        }
    }

    private fun MnemonicWithPassphrase.validatePublicKeysOf(accounts: List<OlympiaAccountDetails>): Boolean {
        val hdPublicKeys = accounts.map {
            HierarchicalDeterministicPublicKey(
                publicKey = it.publicKey,
                derivationPath = it.derivationPath
            )
        }

        return validate(hdPublicKeys = hdPublicKeys)
    }
}

package rdx.works.profile.domain.account

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.validatePublicKeysOf
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.di.coroutines.DefaultDispatcher
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.deviceFactorSources
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import javax.inject.Inject

class GetFactorSourceIdForOlympiaAccountsUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(olympiaAccounts: List<OlympiaAccountDetails>): FactorSource.FactorSourceID.FromHash? {
        return withContext(defaultDispatcher) {
            getProfileUseCase.deviceFactorSources
                .first()
                .filter { deviceFactorSource ->
                    deviceFactorSource.common.cryptoParameters.supportsOlympia
                }
                .map { deviceFactorSource ->
                    deviceFactorSource.id
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
}

package rdx.works.profile.domain.account

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.validatePublicKeysOf
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import javax.inject.Inject

class CheckOlympiaFactorSourceForAccountsExistUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        olympiaAccounts: List<OlympiaAccountDetails>
    ): FactorSource.ID? {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val factorSourceIDs = profile.factorSources.filter {
                it.kind == FactorSourceKind.DEVICE && it.parameters.supportsOlympia
            }.map { it.id }
            factorSourceIDs.forEach { id ->
                val mnemonic = requireNotNull(mnemonicRepository.readMnemonic(id))
                if (mnemonic.validatePublicKeysOf(olympiaAccounts)) {
                    return@withContext id
                }
            }
            return@withContext null
        }
    }
}

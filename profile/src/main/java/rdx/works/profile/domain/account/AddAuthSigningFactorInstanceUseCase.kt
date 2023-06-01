package rdx.works.profile.domain.account

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SigningEntity
import rdx.works.profile.data.model.pernetwork.addAuthSigningFactorInstanceForEntity
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class AddAuthSigningFactorInstanceUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        signingEntity: SigningEntity,
        authSigningFactorInstance: FactorInstance,
    ) {
        return withContext(defaultDispatcher) {
            profileRepository.updateProfile { profile ->
                profile.addAuthSigningFactorInstanceForEntity(signingEntity, authSigningFactorInstance)
            }
        }
    }
}

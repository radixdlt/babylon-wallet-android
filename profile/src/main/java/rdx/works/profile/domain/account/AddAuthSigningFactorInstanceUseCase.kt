package rdx.works.profile.domain.account

import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.ProfileEntity
import rdx.works.core.sargon.addAuthSigningFactorInstanceForEntity
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class AddAuthSigningFactorInstanceUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        entity: ProfileEntity,
        authSigningFactorInstance: HierarchicalDeterministicFactorInstance,
    ) {
        return withContext(defaultDispatcher) {
            profileRepository.updateProfile { profile ->
                profile.addAuthSigningFactorInstanceForEntity(entity, authSigningFactorInstance)
            }
        }
    }
}

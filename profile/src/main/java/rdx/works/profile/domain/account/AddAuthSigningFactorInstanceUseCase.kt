package rdx.works.profile.domain.account

import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.extensions.ProfileEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.addAuthSigningFactorInstanceForEntity
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import rdx.works.core.di.DefaultDispatcher
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

package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.extensions.createOrUpdatePersonaOnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

interface UpdatePersonaUseCase {
    suspend operator fun invoke(
        updatedPersona: OnNetwork.Persona,
    )
}

class UpdatePersonaUseCaseImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : UpdatePersonaUseCase {

    override suspend operator fun invoke(
        updatedPersona: OnNetwork.Persona,
    ) {
        return withContext(defaultDispatcher) {
            val profile = profileDataSource.readProfile()
            checkNotNull(profile) {
                "Profile does not exist"
            }
            val updatedProfile = profile.createOrUpdatePersonaOnNetwork(updatedPersona)
            profileDataSource.saveProfile(updatedProfile)
        }
    }
}

package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.extensions.updatePersona
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class UpdatePersonaUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        updatedPersona: OnNetwork.Persona,
    ) {
        return withContext(defaultDispatcher) {
            val profile = profileDataSource.readProfile()
            checkNotNull(profile) {
                "Profile does not exist"
            }
            val updatedProfile = profile.updatePersona(updatedPersona)
            profileDataSource.saveProfile(updatedProfile)
        }
    }
}

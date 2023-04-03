package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.updatePersona
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class UpdatePersonaUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource,
    private val dAppConnectionRepository: DAppConnectionRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        updatedPersona: Network.Persona,
    ) {
        return withContext(defaultDispatcher) {
            val profile = profileDataSource.profile.first()

            val updatedProfile = profile.updatePersona(updatedPersona)
            profileDataSource.saveProfile(updatedProfile)
            dAppConnectionRepository.ensureAuthorizedPersonasFieldsExist(updatedPersona.address, updatedPersona.fields.map { it.id })
        }
    }
}

package rdx.works.profile.domain.persona

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.updatePersona
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class UpdatePersonaUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dAppConnectionRepository: DAppConnectionRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        updatedPersona: Network.Persona,
    ) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()

            val updatedProfile = profile.updatePersona(updatedPersona)
            profileRepository.saveProfile(updatedProfile)
            dAppConnectionRepository.ensureAuthorizedPersonasFieldsExist(updatedPersona.address, updatedPersona.fields.map { it.id })
        }
    }
}

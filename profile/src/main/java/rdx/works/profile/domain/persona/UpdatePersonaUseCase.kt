package rdx.works.profile.domain.persona

import com.radixdlt.sargon.Persona
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.di.DefaultDispatcher
import rdx.works.core.sargon.fieldIds
import rdx.works.core.sargon.updatePersona
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class UpdatePersonaUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dAppConnectionRepository: DAppConnectionRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        updatedPersona: Persona,
    ) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()

            val updatedProfile = profile.updatePersona(updatedPersona)
            profileRepository.saveProfile(updatedProfile)
            dAppConnectionRepository.ensureAuthorizedPersonasFieldsExist(
                personaAddress = updatedPersona.address,
                existingFieldIds = updatedPersona.personaData.fieldIds
            )
        }
    }
}

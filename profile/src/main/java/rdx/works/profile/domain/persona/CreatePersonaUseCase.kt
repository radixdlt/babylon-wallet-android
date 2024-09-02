package rdx.works.profile.domain.persona

import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.addPersona
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.init
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

class CreatePersonaUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        displayName: DisplayName,
        personaData: PersonaData,
        hdPublicKey: HierarchicalDeterministicPublicKey,
        factorSourceId: FactorSourceId.Hash
    ): Result<Persona> {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val networkId = profile.currentGateway.network.id
            // Construct new persona
            val newPersona = Persona.init(
                displayName = displayName,
                factorSourceId = factorSourceId,
                networkId = networkId,
                personaData = personaData,
                hdPublicKey = hdPublicKey
            )

            // Add persona to the profile
            val updatedProfile = profile.addPersona(
                persona = newPersona,
                onNetwork = networkId
            )
            profileRepository.saveProfile(updatedProfile)
            preferencesManager.markFirstPersonaCreated()
            Result.success(newPersona)
        }
    }
}

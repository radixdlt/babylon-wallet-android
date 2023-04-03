package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Persona.Companion.init
import rdx.works.profile.data.model.pernetwork.addPersona
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreatePersonaUseCase @Inject constructor(
    private val getMnemonicUseCase: GetMnemonicUseCase,
    private val profileDataSource: ProfileDataSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        displayName: String,
        fields: List<Network.Persona.Field>
    ): Network.Persona {
        return withContext(defaultDispatcher) {
            val profile = profileDataSource.profile.first()

            val networkID = profile.appPreferences.gateways.current().network.networkId()

            val factorSource = profile.babylonDeviceFactorSource

            // Construct new persona
            val newPersona = init(
                displayName = displayName,
                fields = fields,
                mnemonicWithPassphrase = getMnemonicUseCase(mnemonicKey = factorSource.id),
                factorSource = factorSource,
                networkId = networkID
            )

            // Add persona to the profile
            val updatedProfile = profile.addPersona(
                persona = newPersona,
                withFactorSourceId = factorSource.id,
                onNetwork = networkID
            )

            // Save updated profile
            profileDataSource.saveProfile(updatedProfile)

            // Return new persona
            newPersona
        }
    }
}

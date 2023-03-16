package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.extensions.createPersona
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.Persona.Companion.createNewPersona
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreatePersonaUseCase @Inject constructor(
    private val generateMnemonicUseCase: GetMnemonicUseCase,
    private val profileDataSource: ProfileDataSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        displayName: String,
        fields: List<OnNetwork.Persona.Field>
    ): OnNetwork.Persona {
        return withContext(defaultDispatcher) {
            val profile = profileDataSource.readProfile()
            checkNotNull(profile) {
                "Profile does not exist"
            }

            val networkID = profile.appPreferences.gateways.current().network.networkId()

            val factorSource = profile.babylonDeviceFactorSource

            // Construct new persona
            val newPersona = createNewPersona(
                displayName = displayName,
                fields = fields,
                mnemonicWithPassphrase = generateMnemonicUseCase(mnemonicKey = factorSource.id),
                factorSource = factorSource,
                networkId = networkID
            )

            // Add persona to the profile
            val updatedProfile = profile.createPersona(
                persona = newPersona,
                factorSourceId = factorSource.id,
                networkId = networkID
            )

            // Save updated profile
            profileDataSource.saveProfile(updatedProfile)

            // Return new persona
            newPersona
        }
    }
}

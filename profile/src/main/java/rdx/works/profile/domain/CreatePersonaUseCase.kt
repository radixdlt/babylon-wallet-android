package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.extensions.addPersonaOnNetwork
import rdx.works.profile.data.model.pernetwork.Persona
import rdx.works.profile.data.model.pernetwork.PersonaField
import rdx.works.profile.data.model.pernetwork.createNewPersona
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.utils.personasPerNetworkCount
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreatePersonaUseCase @Inject constructor(
    private val generateMnemonicUseCase: GetMnemonicUseCase,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        displayName: String,
        fields: List<PersonaField>
    ): Persona {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.readProfile()
            checkNotNull(profile) {
                "Profile does not exist"
            }

            val networkID = profile.appPreferences.networkAndGateway.network.networkId()
            // Construct new persona
            val newPersona = createNewPersona(
                displayName = displayName,
                fields = fields,
                entityIndex = profile.perNetwork.personasPerNetworkCount(networkID),
                mnemonicWords = MnemonicWords(
                    phrase = generateMnemonicUseCase(
                        mnemonicKey = profile.factorSources
                            .curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                            .first().factorSourceID
                    )
                ),
                factorSources = profile.factorSources,
                networkId = networkID
            )

            // Add persona to the profile
            val updatedProfile = profile.addPersonaOnNetwork(
                newPersona,
                networkID = NetworkId.Hammunet
            )

            // Save updated profile
            profileRepository.saveProfile(updatedProfile)

            // Return new persona
            newPersona
        }
    }
}

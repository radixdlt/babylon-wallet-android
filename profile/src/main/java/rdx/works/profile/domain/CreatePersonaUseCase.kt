package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.extensions.createOrUpdatePersonaOnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.Persona.Companion.createNewPersona
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.utils.personasPerNetworkCount
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

            val networkID = profile.appPreferences.networkAndGateway.network.networkId()
            // Construct new persona
            val newPersona = createNewPersona(
                displayName = displayName,
                fields = fields,
                entityIndex = profile.onNetwork.personasPerNetworkCount(networkID),
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
            val updatedProfile = profile.createOrUpdatePersonaOnNetwork(newPersona)

            // Save updated profile
            profileDataSource.saveProfile(updatedProfile)

            // Return new persona
            newPersona
        }
    }
}

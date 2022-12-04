package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import rdx.works.profile.data.repository.AccountDerivationPath
import rdx.works.profile.data.extensions.addPersonaOnNetwork
import rdx.works.profile.data.model.pernetwork.Persona
import rdx.works.profile.data.model.pernetwork.PersonaField
import rdx.works.profile.data.model.pernetwork.createNewPersona
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.utils.personasPerNetworkCount
import rdx.works.profile.derivation.model.NetworkId
import javax.inject.Inject

class CreatePersonaUseCase @Inject constructor(
    private val generateMnemonicUseCase: GetMnemonicUseCase,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        displayName: String,
        fields: List<PersonaField>
    ): Persona {

        // Read profile first as its needed to create account
        profileRepository.readProfileSnapshot()?.let { profileSnapshot ->

            val profile = profileSnapshot.toProfile()

            val networkID = profile.appPreferences.networkAndGateway.network.networkId()
            // Construct new persona
            val newPersona = createNewPersona(
                displayName = displayName,
                fields = fields,
                entityDerivationPath = AccountDerivationPath(
                    perNetwork = profile.perNetwork,
                    networkId = networkID
                ),
                entityIndex = profile.perNetwork.personasPerNetworkCount(networkID),
                mnemonicWords = MnemonicWords(
                    phrase = generateMnemonicUseCase(
                        mnemonicKey = profile.factorSources
                            .curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                            .first().factorSourceID
                    )
                ),
                factorSources = profile.factorSources
            )

            // Add persona to the profile
            val updatedProfile = profile.addPersonaOnNetwork(
                newPersona,
                networkID = NetworkId.Hammunet
            )

            // Save updated profile
            profileRepository.saveProfileSnapshot(updatedProfile.snapshot())

            // Return new persona
            return newPersona
        }

        // We should never reach here
        throw IllegalStateException("Profile does not exist")
    }
}

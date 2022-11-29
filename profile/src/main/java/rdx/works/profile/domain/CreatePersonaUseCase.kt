package rdx.works.profile.domain

import rdx.works.profile.data.repository.AccountDerivationPath
import rdx.works.profile.data.repository.CompressedPublicKey
import rdx.works.profile.data.repository.UnsecuredSecurityState
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
        profileRepository.readProfile()?.let { profile ->

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
                derivePublicKey = CompressedPublicKey(
                    mnemonic = generateMnemonicUseCase()
                ),
                createSecurityState = UnsecuredSecurityState(
                    factorSources = profile.factorSources
                )
            )

            // Add persona to the profile
            val updatedProfile = profile.addPersonaOnNetwork(
                newPersona,
                networkID = NetworkId.Hammunet
            )

            // Save updated profile
            profileRepository.saveProfile(updatedProfile)

            // Return new persona
            return newPersona
        }

        // We should never reach here
        throw IllegalStateException("Profile does not exist")
    }
}

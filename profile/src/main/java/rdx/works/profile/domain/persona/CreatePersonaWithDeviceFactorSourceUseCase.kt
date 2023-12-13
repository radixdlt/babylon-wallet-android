package rdx.works.profile.domain.persona

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.model.extensions.mainBabylonFactorSource
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Persona.Companion.init
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.addPersona
import rdx.works.profile.data.model.pernetwork.nextPersonaIndex
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.di.coroutines.DefaultDispatcher
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import javax.inject.Inject

class CreatePersonaWithDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        displayName: String,
        personaData: PersonaData
    ): Network.Persona {
        return withContext(defaultDispatcher) {
            val profile = ensureBabylonFactorSourceExistUseCase()

            val networkID = profile.currentGateway.network.networkId()
            val factorSource = profile.mainBabylonFactorSource()
                ?: throw IllegalStateException("Babylon factor source is not present")
            val mnemonicWithPassphrase = requireNotNull(mnemonicRepository.readMnemonic(factorSource.id).getOrNull())
            // Construct new persona
            val newPersona = init(
                entityIndex = profile.nextPersonaIndex(DerivationPathScheme.CAP_26, networkID),
                displayName = displayName,
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                factorSource = factorSource,
                networkId = networkID,
                personaData = personaData
            )

            // Add persona to the profile
            val updatedProfile = profile.addPersona(
                persona = newPersona,
                onNetwork = networkID
            )
            profileRepository.saveProfile(updatedProfile)
            newPersona
        }
    }
}

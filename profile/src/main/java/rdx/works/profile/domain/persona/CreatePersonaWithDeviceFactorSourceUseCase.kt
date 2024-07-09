package rdx.works.profile.domain.persona

import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.extensions.asGeneral
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.addPersona
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.init
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.core.sargon.nextPersonaIndex
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.di.coroutines.DefaultDispatcher
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import javax.inject.Inject

class CreatePersonaWithDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager,
    private val profileRepository: ProfileRepository,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        displayName: DisplayName,
        personaData: PersonaData
    ): Result<Persona> {
        return withContext(defaultDispatcher) {
            ensureBabylonFactorSourceExistUseCase().mapCatching { profile ->
                val networkId = profile.currentGateway.network.id
                val factorSource = profile.mainBabylonFactorSource
                    ?: error("Babylon factor source is not present")
                val mnemonicWithPassphrase = mnemonicRepository.readMnemonic(factorSource.value.id.asGeneral()).getOrThrow()
                // Construct new persona
                val newPersona = Persona.init(
                    entityIndex = profile.nextPersonaIndex(
                        forNetworkId = networkId,
                        derivationPathScheme = DerivationPathScheme.CAP26
                    ),
                    displayName = displayName,
                    mnemonicWithPassphrase = mnemonicWithPassphrase,
                    factorSourceId = factorSource.value.id.asGeneral(),
                    networkId = networkId,
                    personaData = personaData
                )

                // Add persona to the profile
                val updatedProfile = profile.addPersona(
                    persona = newPersona,
                    onNetwork = networkId
                )
                profileRepository.saveProfile(updatedProfile)
                preferencesManager.markFirstPersonaCreated()
                newPersona
            }
        }
    }
}

package rdx.works.profile

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.AddP2PClientUseCase

class AddP2PClientUseCaseTest {

    @Test
    fun `diw fe`() = runBlocking {
        val profileRepository = mock(ProfileRepository::class.java)
        val addP2PClientUseCase = AddP2PClientUseCase(profileRepository)

        val initialProfile = Profile(
            appPreferences = AppPreferences(
                display = Display.default,
                networkAndGateway = NetworkAndGateway.hammunet,
                p2pClients = emptyList()
            ),
            factorSources = FactorSources(
                curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources = emptyList(),
                secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources = emptyList()
            ),
            perNetwork = emptyList(),
            version = "0.0.1"
        )
        whenever(profileRepository.readProfile()).thenReturn(initialProfile)

        val p2pClient = addP2PClientUseCase(
            displayName = "Mac browser",
            connectionPassword = "pass1234"
        )

        val updatedProfile = initialProfile.copy(
            appPreferences = AppPreferences(
                display = initialProfile.appPreferences.display,
                networkAndGateway = initialProfile.appPreferences.networkAndGateway,
                p2pClients = listOf(p2pClient)
            ),
            factorSources = initialProfile.factorSources,
            perNetwork = initialProfile.perNetwork,
            version = initialProfile.version
        )
        verify(profileRepository).saveProfile(updatedProfile)
    }

    @Test(expected = IllegalStateException::class)
    fun `diw fer3r3g534`(): Unit = runBlocking {
        val profileRepository = mock(ProfileRepository::class.java)
        val addP2PClientUseCase = AddP2PClientUseCase(profileRepository)

        whenever(profileRepository.readProfile()).thenReturn(null)

        addP2PClientUseCase(
            displayName = "Mac browser",
            connectionPassword = "pass1234"
        )
    }
}
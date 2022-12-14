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
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.AddP2PClientUseCase
import kotlin.test.Ignore

class AddP2PClientUseCaseTest {

    @Ignore("P2PClient data class or this unit test needs refactor")
    @Test
    fun `given profile exists, when adding p2p client, verify it is added properly`() = runBlocking {
        val profileRepository = mock(ProfileRepository::class.java)
        val addP2PClientUseCase = AddP2PClientUseCase(profileRepository)
        val expectedP2pClient = P2PClient.init(
            connectionPassword = "pass1234",
            displayName = "Mac browser"
        )

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
        whenever(profileRepository.readProfileSnapshot()).thenReturn(initialProfile.snapshot())

        addP2PClientUseCase(
            displayName = "Mac browser",
            connectionPassword = "pass1234"
        )

        val updatedProfile = initialProfile.copy(
            appPreferences = AppPreferences(
                display = initialProfile.appPreferences.display,
                networkAndGateway = initialProfile.appPreferences.networkAndGateway,
                p2pClients = listOf(expectedP2pClient)
            ),
            factorSources = initialProfile.factorSources,
            perNetwork = initialProfile.perNetwork,
            version = initialProfile.version
        )
        verify(profileRepository).saveProfileSnapshot(updatedProfile.snapshot())
    }

    @Test(expected = IllegalStateException::class)
    fun `when profile does not exist, verify exception thrown when adding p2pclient`(): Unit = runBlocking {
        val profileRepository = mock(ProfileRepository::class.java)
        val addP2PClientUseCase = AddP2PClientUseCase(profileRepository)

        whenever(profileRepository.readProfileSnapshot()).thenReturn(null)

        addP2PClientUseCase(
            displayName = "Mac browser",
            connectionPassword = "pass1234"
        )
    }
}
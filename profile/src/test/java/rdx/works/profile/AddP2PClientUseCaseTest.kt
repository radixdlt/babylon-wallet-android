package rdx.works.profile

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.domain.AddP2PClientUseCase
import kotlin.test.Ignore

class AddP2PClientUseCaseTest {

    @Ignore("P2PClient data class or this unit test needs refactor")
    @Test
    fun `given profile exists, when adding p2p client, verify it is added properly`() = runBlocking {
        val profileDataSource = mock(ProfileDataSource::class.java)
        val addP2PClientUseCase = AddP2PClientUseCase(profileDataSource)
        val expectedP2pClient = P2PClient.init(
            connectionPassword = "pass1234",
            displayName = "Mac browser"
        )

        val initialProfile = Profile(
            id = "9958f568-8c9b-476a-beeb-017d1f843266",
            appPreferences = AppPreferences(
                display = Display.default,
                gateways = Gateways(Gateway.hammunet.url, listOf(Gateway.hammunet)),
                p2pClients = emptyList()
            ),
            factorSources = FactorSources(
                curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources = emptyList(),
                secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources = emptyList()
            ),
            onNetwork = emptyList(),
            version = 1
        )
        whenever(profileDataSource.readProfile()).thenReturn(initialProfile)

        addP2PClientUseCase(
            displayName = "Mac browser",
            connectionPassword = "pass1234"
        )

        val updatedProfile = initialProfile.copy(
            appPreferences = AppPreferences(
                display = initialProfile.appPreferences.display,
                gateways = initialProfile.appPreferences.gateways,
                p2pClients = listOf(expectedP2pClient)
            ),
            factorSources = initialProfile.factorSources,
            onNetwork = initialProfile.onNetwork,
            version = initialProfile.version
        )
        verify(profileDataSource).saveProfile(updatedProfile)
    }

    @Test(expected = IllegalStateException::class)
    fun `when profile does not exist, verify exception thrown when adding p2pclient`(): Unit = runBlocking {
        val profileDataSource = mock(ProfileDataSource::class.java)
        val addP2PClientUseCase = AddP2PClientUseCase(profileDataSource)

        whenever(profileDataSource.readProfile()).thenReturn(null)

        addP2PClientUseCase(
            displayName = "Mac browser",
            connectionPassword = "pass1234"
        )
    }
}

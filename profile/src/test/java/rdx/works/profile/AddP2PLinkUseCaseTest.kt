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
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.domain.AddP2PLinkUseCase
import kotlin.test.Ignore
import rdx.works.profile.data.model.apppreferences.Security

class AddP2PLinkUseCaseTest {

    @Ignore("P2PLink data class or this unit test needs refactor")
    @Test
    fun `given profile exists, when adding p2p client, verify it is added properly`() = runBlocking {
        val profileDataSource = mock(ProfileDataSource::class.java)
        val addP2PLinkUseCase = AddP2PLinkUseCase(profileDataSource)
        val expectedP2PLink = P2PLink.init(
            connectionPassword = "pass1234",
            displayName = "Mac browser"
        )

        val initialProfile = Profile(
            id = "9958f568-8c9b-476a-beeb-017d1f843266",
            creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)",
            appPreferences = AppPreferences(
                display = Display.default,
                security = Security.default,
                gateways = Gateways(Gateway.hammunet.url, listOf(Gateway.hammunet)),
                p2pLinks = emptyList()
            ),
            factorSources = listOf(),
            networks = emptyList(),
            version = 1
        )
        whenever(profileDataSource.readProfile()).thenReturn(initialProfile)

        addP2PLinkUseCase(
            displayName = "Mac browser",
            connectionPassword = "pass1234"
        )

        val updatedProfile = initialProfile.copy(
            appPreferences = AppPreferences(
                display = initialProfile.appPreferences.display,
                security = initialProfile.appPreferences.security,
                gateways = initialProfile.appPreferences.gateways,
                p2pLinks = listOf(expectedP2PLink)
            ),
            factorSources = initialProfile.factorSources,
            networks = initialProfile.networks,
            version = initialProfile.version
        )
        verify(profileDataSource).saveProfile(updatedProfile)
    }

    @Test(expected = IllegalStateException::class)
    fun `when profile does not exist, verify exception thrown when adding p2pLink`(): Unit = runBlocking {
        val profileDataSource = mock(ProfileDataSource::class.java)
        val addP2PLinkUseCase = AddP2PLinkUseCase(profileDataSource)

        whenever(profileDataSource.readProfile()).thenReturn(null)

        addP2PLinkUseCase(
            displayName = "Mac browser",
            connectionPassword = "pass1234"
        )
    }
}

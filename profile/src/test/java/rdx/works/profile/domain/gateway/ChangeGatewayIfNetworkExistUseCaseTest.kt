package rdx.works.profile.domain.gateway

import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import rdx.works.core.sargon.addGateway
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.FakeProfileRepository
import rdx.works.profile.data.repository.profile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChangeGatewayIfNetworkExistUseCaseTest {

    @Test
    fun `switches to exact gateway when multiple gateways share network id`() = runTest {
        val firstMainnetGateway = Gateway.init("https://first-mainnet-gateway.radixdlt.com/", NetworkId.MAINNET)
        val selectedMainnetGateway = Gateway.init("https://selected-mainnet-gateway.radixdlt.com/", NetworkId.MAINNET)
        val profileRepository = FakeProfileRepository(
            Profile.sample()
                .addGateway(firstMainnetGateway)
                .addGateway(selectedMainnetGateway)
        )
        val useCase = ChangeGatewayIfNetworkExistUseCase(profileRepository)

        val changed = useCase(selectedMainnetGateway)

        assertTrue(changed)
        assertEquals(
            selectedMainnetGateway.string,
            profileRepository.profile.first().currentGateway.string
        )
    }
}

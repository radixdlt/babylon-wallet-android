package rdx.works.profile.domain.account

import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.Url
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import rdx.works.core.sargon.addGateway
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.init
import rdx.works.profile.FakeProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.repository.updateProfile
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SwitchNetworkUseCaseTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
        phrase = "noodle question hungry sail type offer grocery clay nation hello mixture forum"
    )
    private val profileRepository = FakeProfileRepository(
        Profile.init(
            deviceFactorSource = DeviceFactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase, isMain = true).asGeneral(),
            creatingDeviceName = "Unit Test"
        )
    )

    private val useCase = SwitchNetworkUseCase(profileRepository, testDispatcher)

    @Test
    fun `switching network changes profile current network`() = testScope.runTest {
        val networkId = NetworkId.HAMMUNET
        val urlToSwitch = "https://hammunet-network.radixdlt.com/"
        profileRepository.updateProfile { it.addGateway(Gateway.init(urlToSwitch, networkId)) }

        useCase(Url(urlToSwitch))
        assertEquals(urlToSwitch, profileRepository.profile.first().currentGateway.string)
    }
}
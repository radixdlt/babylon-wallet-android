package rdx.works.profile.domain.account

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import rdx.works.core.domain.DeviceInfo
import rdx.works.core.sargon.addGateway
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.toUrl
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
    private val profileRepository = with(DeviceInfo.sample()) {
        FakeProfileRepository(
            Profile.init(
                deviceFactorSource = FactorSource.Device.babylon(
                    mnemonicWithPassphrase = mnemonicWithPassphrase,
                    deviceInfo = this,
                    isMain = true
                ),
                deviceInfo = toSargonDeviceInfo()
            )
        )
    }

    private val useCase = SwitchNetworkUseCase(profileRepository, testDispatcher)

    @Test
    fun `switching network changes profile current network`() = testScope.runTest {
        val networkId = NetworkId.HAMMUNET
        val urlToSwitch = "https://hammunet-network.radixdlt.com/"
        profileRepository.updateProfile { it.addGateway(Gateway.init(urlToSwitch, networkId)) }

        useCase(urlToSwitch.toUrl())
        assertEquals(urlToSwitch, profileRepository.profile.first().currentGateway.string)
    }
}
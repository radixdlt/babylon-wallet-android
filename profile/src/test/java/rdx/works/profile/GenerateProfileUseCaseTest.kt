package rdx.works.profile

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import rdx.works.core.domain.DeviceInfo
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.init
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GenerateProfileUseCase
import kotlin.test.Test

class GenerateProfileUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeDeviceInfoRepository = mockk<DeviceInfoRepository>().apply {
        every { getDeviceInfo() } returns DeviceInfo(
            name = "Unit",
            manufacturer = "Test",
            model = ""
        )
    }
    private val mnemonicRepository = mockk<MnemonicRepository>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val profileRepository = FakeProfileRepository()
    private val testScope = TestScope(testDispatcher)
    val generateProfileUseCase = GenerateProfileUseCase(
        profileRepository = profileRepository,
        deviceInfoRepository = fakeDeviceInfoRepository,
        defaultDispatcher = testDispatcher,
        mnemonicRepository = mnemonicRepository,
        preferencesManager = preferencesManager
    )

    @Before
    fun setUp() {
        coEvery { mnemonicRepository.saveMnemonic(any(), any()) } just Runs
        coEvery { preferencesManager.markFactorSourceBackedUp(any()) } just Runs
    }

    @Test
    fun `when generating profile, verify correct data generated from mnemonic`() {
        testScope.runTest {
            val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
                phrase = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                        "humble limb repeat video sudden possible story mask neutral prize goose mandate"
            )
            val babylonFactorSource = FactorSource.Device.babylon(
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                isMain = true
            )

            val profile = generateProfileUseCase(mnemonicWithPassphrase)

            assertEquals(babylonFactorSource.id, profile.mainBabylonFactorSource!!.id)
        }
    }
}

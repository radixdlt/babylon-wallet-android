package rdx.works.profile

import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.asGeneral
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
import org.junit.Ignore
import rdx.works.core.domain.DeviceInfo
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.init
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
    fun `given profile already exists, when generate profile called, return existing profile`() = testScope.runTest {
        // given
        val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
            phrase = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate"
        )

        val profile = Profile.init(
            deviceFactorSource = DeviceFactorSource.babylon(
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                isMain = true
            ).asGeneral(),
            creatingDeviceName = "Unit Test"
        )
        profileRepository.saveProfile(profile)

        // then
        assertEquals(generateProfileUseCase(), profile)
    }

    @Ignore("TODO Integration")
    @Test
    fun `given profile does not exist, when generating one, verify correct data generated from mnemonic`() {
        testScope.runTest {
            val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
                phrase = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                        "humble limb repeat video sudden possible story mask neutral prize goose mandate"
            )
            val babylonFactorSource = DeviceFactorSource.babylon(
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                isMain = true
            )
            // TODO integration this should change
            val profile = generateProfileUseCase()

//            Assert.assertEquals(
//                "Factor Source ID",
//                expectedFactorSourceId,
//                profile.mainBabylonFactorSource()!!.id.body.value
//            )
        }
    }
}

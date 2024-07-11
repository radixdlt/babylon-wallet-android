package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import rdx.works.core.TimestampGenerator
import rdx.works.core.domain.DeviceInfo
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.core.sargon.olympia
import rdx.works.profile.FakeProfileRepository
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EnsureBabylonFactorSourceExistUseCaseTest {
    private val profileRepository = FakeProfileRepository()
    private val mnemonicRepository = mockk<MnemonicRepository>()
    private val deviceInfoRepository = mockk<DeviceInfoRepository>()
    private val preferenceManager = mockk<PreferencesManager>()

    private val ensureBabylonFactorSourceExistUseCase =
        EnsureBabylonFactorSourceExistUseCase(mnemonicRepository, profileRepository, deviceInfoRepository, preferenceManager)

    private val deviceInfo = DeviceInfo(
        id = UUID.randomUUID(),
        date = TimestampGenerator(),
        name = "device1",
        manufacturer = "manufacturer1",
        model = "model1"
    )
    private val mnemonic = MnemonicWithPassphrase.init(
        phrase = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate"
    )
    // Sargon does not allow init without main, so the only possible test case here is to create a new profile,
    // remove main factor source, serialize, deserialize and provide it to repository
    private val deviceFactorSource = FactorSource.Device.babylon(
        mnemonicWithPassphrase = mnemonic,
        deviceInfo = deviceInfo,
        isMain = true
    )
    private val profileWithoutMain = Profile.init(
        deviceFactorSource = deviceFactorSource,
        deviceInfo = deviceInfo.toSargonDeviceInfo(),
    ).let {
        it.copy(
            factorSources = it.factorSources.asIdentifiable().append(
                element = FactorSource.Device.olympia(
                    mnemonicWithPassphrase = MnemonicWithPassphrase.sample.other(),
                    deviceInfo = deviceInfo
                )
            ).removeBy(deviceFactorSource.id).asList()
        )
    }

    @Test
    fun `babylon factor source is added to profile if it does not exist`() = runTest {
        profileRepository.saveProfile(profileWithoutMain)
        coEvery { preferenceManager.markFactorSourceBackedUp(any()) } just Runs
        every { deviceInfoRepository.getDeviceInfo() } returns deviceInfo
        coEvery { mnemonicRepository.createNew() } returns Result.success(mnemonic)

        val profile = ensureBabylonFactorSourceExistUseCase().getOrThrow()
        assertEquals(2, profile.factorSources.size)
        assertEquals(deviceFactorSource.id, profile.mainBabylonFactorSource?.id)

        // Run use case again, factor source is in place so no need to add it again
        val profileAgain = ensureBabylonFactorSourceExistUseCase().getOrThrow()
        assertEquals(2, profileAgain.factorSources.size)
    }
}

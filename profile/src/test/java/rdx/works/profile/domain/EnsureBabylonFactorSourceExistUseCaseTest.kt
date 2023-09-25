package rdx.works.profile.domain

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.DeviceInfo
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import java.time.Instant

internal class EnsureBabylonFactorSourceExistUseCaseTest {
    private val profileRepository = mockk<ProfileRepository>()
    private val mnemonicRepository = mockk<MnemonicRepository>()
    private val deviceInfoRepository = mockk<DeviceInfoRepository>()

    private val ensureBabylonFactorSourceExistUseCase =
        EnsureBabylonFactorSourceExistUseCase(mnemonicRepository, profileRepository, deviceInfoRepository)

    @Before
    fun setUp() {
        every { deviceInfoRepository.getDeviceInfo() } returns DeviceInfo("device1", "manufacturer1", "model1")
        coEvery { mnemonicRepository() } returns MnemonicWithPassphrase(
            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
            bip39Passphrase = ""
        )
        val profile = Profile.init(
            id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
            deviceInfo = DeviceInfo(
                name = "unit",
                manufacturer = "",
                model = "test"
            ),
            creationDate = Instant.EPOCH,
            gateway = Radix.Gateway.default
        )
        every { profileRepository.profileState } returns flowOf(ProfileState.Restored(profile))
        coEvery { profileRepository.saveProfile(any()) } just Runs
    }

    @Test
    fun `babylon factor source is added to profile if it does not exist`() = runTest {
        val profile = ensureBabylonFactorSourceExistUseCase()
        every { profileRepository.profileState } returns flowOf(ProfileState.Restored(profile))
        ensureBabylonFactorSourceExistUseCase()
        assert(profile.factorSources.size == 1)
    }
}

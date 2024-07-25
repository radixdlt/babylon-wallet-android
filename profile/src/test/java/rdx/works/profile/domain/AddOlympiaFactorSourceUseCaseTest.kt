package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HostId
import com.radixdlt.sargon.HostInfo
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.babylon
import rdx.works.profile.FakeProfileRepository
import rdx.works.profile.data.repository.HostInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import kotlin.test.Test

internal class AddOlympiaFactorSourceUseCaseTest {

    private val profileRepository = FakeProfileRepository()
    private val getProfileUseCase = GetProfileUseCase(profileRepository)
    private val mnemonicRepository = mockk<MnemonicRepository>()
    private val hostInfoRepository = mockk<HostInfoRepository>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val usecase = AddOlympiaFactorSourceUseCase(
        getProfileUseCase = getProfileUseCase,
        profileRepository = profileRepository,
        mnemonicRepository = mnemonicRepository,
        preferencesManager = preferencesManager,
        hostInfoRepository = hostInfoRepository
    )

    @Test
    fun `new factor source is added to a profile, if it does not already exist`() = runTest {
        val olympiaMnemonic = MnemonicWithPassphrase.init(
            phrase = "noodle question hungry sail type offer grocery clay nation hello mixture forum"
        )
        val babylonMnemonic = MnemonicWithPassphrase.init(
            phrase = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote"
        )

        val hostId = HostId.sample()
        val hostInfo = HostInfo.sample.other()
        val profile = Profile.init(
            deviceFactorSource = FactorSource.Device.babylon(
                mnemonicWithPassphrase = babylonMnemonic,
                hostInfo = hostInfo,
                isMain = true
            ),
            hostId = hostId,
            hostInfo = hostInfo
        )
        profileRepository.saveProfile(profile)

        coEvery { mnemonicRepository.mnemonicExist(any()) } returns false
        coEvery { mnemonicRepository.saveMnemonic(any(), any()) } returns Result.success(Unit)
        coEvery { preferencesManager.markFactorSourceBackedUp(any()) } just Runs
        coEvery { hostInfoRepository.getHostId() } returns hostId
        coEvery { hostInfoRepository.getHostInfo() } returns hostInfo

        usecase(olympiaMnemonic)
        assertEquals(2, profileRepository.inMemoryProfileOrNull?.factorSources?.size)

        usecase(olympiaMnemonic)
        assertEquals(2, profileRepository.inMemoryProfileOrNull?.factorSources?.size)
    }
}

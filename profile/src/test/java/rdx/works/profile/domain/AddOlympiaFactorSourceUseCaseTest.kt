package rdx.works.profile.domain

import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.size
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.init
import rdx.works.profile.FakeProfileRepository
import rdx.works.profile.data.repository.MnemonicRepository
import kotlin.test.Test

internal class AddOlympiaFactorSourceUseCaseTest {

    private val profileRepository = FakeProfileRepository()
    private val getProfileUseCase = GetProfileUseCase(profileRepository)
    private val mnemonicRepository = mockk<MnemonicRepository>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val usecase = AddOlympiaFactorSourceUseCase(getProfileUseCase, profileRepository, mnemonicRepository, preferencesManager)

    @Test
    fun `new factor source is added to a profile, if it does not already exist`() = runTest {
        val olympiaMnemonic = MnemonicWithPassphrase.init(
            phrase = "noodle question hungry sail type offer grocery clay nation hello mixture forum"
        )
        val babylonMnemonic = MnemonicWithPassphrase.init(
            phrase = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote"
        )


        val profile = Profile.init(
            deviceFactorSource = FactorSource.Device.babylon(mnemonicWithPassphrase = babylonMnemonic, isMain = true),
            creatingDeviceName = "Unit Test"
        )
        profileRepository.saveProfile(profile)

        coEvery { mnemonicRepository.mnemonicExist(any()) } returns false
        coEvery { mnemonicRepository.saveMnemonic(any(), any()) } just Runs
        coEvery { preferencesManager.markFactorSourceBackedUp(any()) } just Runs


        usecase(olympiaMnemonic)
        assertEquals(2, profileRepository.inMemoryProfileOrNull?.factorSources?.size)

        usecase(olympiaMnemonic)
        assertEquals(2, profileRepository.inMemoryProfileOrNull?.factorSources?.size)
    }
}

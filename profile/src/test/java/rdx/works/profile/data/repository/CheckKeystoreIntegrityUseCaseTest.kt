package rdx.works.profile.data.repository

import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.core.logNonFatalException

class CheckKeystoreIntegrityUseCaseTest {
    private val getProfile = mockk<GetProfileUseCase>()
    private val repository = mockk<MnemonicRepository> {
        coEvery { clearInvalidatedMnemonics() } returns Result.success(Unit)
    }
    private val sut = CheckKeystoreIntegrityUseCase(getProfile, repository)

    @Test fun migratedStorageIsHealthyWithoutResettingTheRetiredLegacyAlias() = runTest {
        coEvery { getProfile.finishedOnboardingProfile() } returns Profile.sample()
        coEvery { repository.mnemonicExist(any()) } returns true
        sut()
        assertFalse(sut.didMnemonicIntegrityChange.first())
        coVerifyOrder {
            repository.clearInvalidatedMnemonics()
            repository.mnemonicExist(any())
        }
        coVerify(exactly = 0) { repository.deleteMnemonic(any()) }
    }

    @Test fun resetFailurePreservesTheLastIntegrityStatus() = runTest {
        mockkStatic("rdx.works.core.CrashlyticsExtensionsKt")
        every { logNonFatalException(any()) } returns Unit
        try {
            coEvery { getProfile.finishedOnboardingProfile() } returns Profile.sample()
            coEvery { repository.mnemonicExist(any()) } returns false
            sut()
            assertTrue(sut.didMnemonicIntegrityChange.first())
            coEvery { repository.clearInvalidatedMnemonics() } returns Result.failure(java.io.IOException("Key reset failed"))
            sut()
            assertTrue(sut.didMnemonicIntegrityChange.first())
            coVerify(exactly = 0) { repository.deleteMnemonic(any()) }
            coVerify(exactly = 1) { repository.mnemonicExist(any()) }
        } finally {
            unmockkStatic("rdx.works.core.CrashlyticsExtensionsKt")
        }
    }

    @Test fun missingRecordsAreReportedWithoutDeletingAnything() = runTest {
        coEvery { getProfile.finishedOnboardingProfile() } returns Profile.sample()
        coEvery { repository.mnemonicExist(any()) } returns false
        sut()
        assertTrue(sut.didMnemonicIntegrityChange.first())
        coVerify(exactly = 0) { repository.deleteMnemonic(any()) }
    }

    @Test fun onboardingDoesNotRunForegroundRecordChecks() = runTest {
        coEvery { getProfile.finishedOnboardingProfile() } returns null
        sut()
        coVerify(exactly = 0) { repository.mnemonicExist(any()) }
        coVerify(exactly = 0) { repository.clearInvalidatedMnemonics() }
    }

    @Test fun recoveryRefreshesWalletIntegrityWithoutDeletingOtherSeedRecords() = runTest {
        coEvery { getProfile.finishedOnboardingProfile() } returns Profile.sample()
        coEvery { repository.mnemonicExist(any()) } returnsMany listOf(false, true)
        sut()
        assertTrue(sut.didMnemonicIntegrityChange.first())
        sut()
        assertFalse(sut.didMnemonicIntegrityChange.first())
        coVerify(exactly = 0) { repository.deleteMnemonic(any()) }
    }

    @Test fun eachSuccessfulCheckRefreshesPerMnemonicRecoveryStatus() = runTest {
        coEvery { getProfile.finishedOnboardingProfile() } returns Profile.sample()
        // One record stays unavailable while another record becomes unavailable or is recovered.
        // The aggregate flag stays true, but listeners must recheck each mnemonic.
        coEvery { repository.mnemonicExist(any()) } returns false
        val events = mutableListOf<Boolean>()
        val collector = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
            sut.didMnemonicIntegrityChange.take(3).toList(events)
        }
        sut()
        yield()
        sut()
        yield()
        assertEquals(listOf(false, true, true), events)
        collector.cancel()
    }
}

package rdx.works.profile.domain

import com.radixdlt.sargon.HostInfo
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.extensions.Accounts
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import java.io.IOException

class DeriveProfileRecoveryTest {
    private val state = MutableStateFlow<ProfileState>(ProfileState.None)
    private val profiles = mockk<ProfileRepository> { every { profileState } returns state }
    private val mnemonics = mockk<MnemonicRepository>()
    private val preferences = mockk<PreferencesManager>(relaxed = true)
    private val os = mockk<SargonOs>()
    private val manager = mockk<SargonOsManager> { every { sargonOs } returns os }
    private var recovered = false

    @Test fun derivationRecoversWithoutAnExistingProfileBeforePersistingTheImportedMnemonic() = runTest {
        coEvery { mnemonics.clearInvalidatedMnemonics() } coAnswers { recovered = true; Result.success(Unit) }
        coEvery { os.resolveHostInfo() } returns HostInfo.sample()
        coEvery { os.newWalletWithDerivedBdfs(any(), any()) } coAnswers {
            check(recovered) { "Retained invalidated key prevents imported mnemonic persistence" }
        }
        val sut = DeriveProfileUseCase(profiles, preferences, manager, mnemonics, StandardTestDispatcher(testScheduler))
        assertTrue(sut(MnemonicWithPassphrase.sample(), Accounts(emptyList())).isSuccess)
        coVerify(exactly = 1) { preferences.markFactorSourceBackedUp(any()) }
    }

    @Test fun failedRecoveryPreventsDerivationAndDoesNotMarkTheSeedBackedUp() = runTest {
        coEvery { mnemonics.clearInvalidatedMnemonics() } returns Result.failure(IOException("Reset failed"))
        val sut = DeriveProfileUseCase(profiles, preferences, manager, mnemonics, StandardTestDispatcher(testScheduler))
        assertTrue(sut(MnemonicWithPassphrase.sample(), Accounts(emptyList())).isFailure)
        coVerify(exactly = 0) { os.newWalletWithDerivedBdfs(any(), any()) }
        coVerify(exactly = 0) { preferences.markFactorSourceBackedUp(any()) }
    }

    @Test fun anExistingProfileDoesNotRunDerivationCleanup() = runTest {
        state.value = ProfileState.Loaded(Profile.sample())
        val sut = DeriveProfileUseCase(profiles, preferences, manager, mnemonics, StandardTestDispatcher(testScheduler))
        assertTrue(sut(MnemonicWithPassphrase.sample(), Accounts(emptyList())).isSuccess)
        coVerify(exactly = 0) { mnemonics.clearInvalidatedMnemonics() }
        coVerify(exactly = 0) { os.newWalletWithDerivedBdfs(any(), any()) }
    }
}

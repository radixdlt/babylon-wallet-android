package com.babylon.wallet.android.presentation.account.createaccount

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.selectfactorsource.SelectFactorSourceProxy
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.DiscardTemporaryRestoredFileForBackupUseCase
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountRecoveryTest {
    @get:Rule val dispatcherRule = TestDispatcherRule()
    private val profile = mockk<GetProfileUseCase>()
    private val mnemonics = mockk<MnemonicRepository>()
    private val os = mockk<SargonOs>()
    private val manager = mockk<SargonOsManager> { every { sargonOs } returns os }
    private val discardBackup = mockk<DiscardTemporaryRestoredFileForBackupUseCase>(relaxed = true)
    private val selectFactor = mockk<SelectFactorSourceProxy> {
        coEvery { selectFactorSource(any()) } returns null
    }
    private var recovered = false

    private fun viewModel() = CreateAccountViewModel(
        savedStateHandle = mockk<SavedStateHandle> { every { get<Any?>(any()) } returns null },
        firstAccountCreationStatusManager = mockk(relaxed = true),
        syncAccountThirdPartyDepositsWithLedger = mockk(),
        getProfileUseCase = profile,
        deleteWalletUseCase = mockk(relaxed = true),
        discardTemporaryRestoredFileForBackupUseCase = discardBackup,
        changeGatewayIfNetworkExistUseCase = mockk(),
        changeBackupSettingUseCase = mockk(),
        appEventBus = mockk(),
        homeCardsRepository = mockk(),
        sargonOsManager = manager,
        selectFactorSourceProxy = selectFactor,
        mnemonicRepository = mnemonics
    )

    @Test fun creationRecoversBeforeTheSdkPersistsItsDefaultMnemonic() = runTest {
        coEvery { profile.finishedOnboardingProfile() } returns null
        coEvery { mnemonics.clearInvalidatedMnemonics() } coAnswers { recovered = true; Result.success(Unit) }
        coEvery { os.newWallet() } coAnswers { check(recovered) { "Retained invalidated key prevents mnemonic persistence" } }
        val vm = viewModel()
        vm.onAccountCreateClick()
        advanceUntilIdle()
        coVerify(exactly = 1) { discardBackup(BackupType.DeprecatedCloud) }
        assertNull(vm.state.value.uiMessage)
        assertFalse(vm.state.value.isCreatingAccount)
    }

    @Test fun failedRecoveryStopsCreationBeforeSdkCanContinueWithoutSavingTheMnemonic() = runTest {
        coEvery { profile.finishedOnboardingProfile() } returns null
        coEvery { mnemonics.clearInvalidatedMnemonics() } returns Result.failure(IOException("Reset failed"))
        val vm = viewModel()
        vm.onAccountCreateClick()
        advanceUntilIdle()
        coVerify(exactly = 0) { os.newWallet() }
        coVerify(exactly = 0) { discardBackup(any()) }
        assertNotNull(vm.state.value.uiMessage)
        assertFalse(vm.state.value.isCreatingAccount)
    }

    @Test fun existingWalletDoesNotRunCreationCleanup() = runTest {
        coEvery { profile.finishedOnboardingProfile() } returns Profile.sample()
        viewModel().onAccountCreateClick()
        advanceUntilIdle()
        coVerify(exactly = 0) { mnemonics.clearInvalidatedMnemonics() }
        coVerify(exactly = 0) { os.newWallet() }
    }
}

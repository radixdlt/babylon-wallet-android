package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.presentation.onboarding.OnboardingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import rdx.works.profile.domain.backup.DiscardTemporaryRestoredFileForBackupUseCase

@ExperimentalCoroutinesApi
class OnboardingViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    //private val deviceSecurityHelper = Mockito.mock(DeviceSecurityHelper::class.java)
    private val discardTemporaryRestoredFileForBackupUseCase = Mockito.mock(DiscardTemporaryRestoredFileForBackupUseCase::class.java)

    @Ignore("Test is ignored until on-boarding feature is completed")
    @Test
    fun `given device is not secure, when proceed button clicked, show warning`() = runTest {
        // given
        //whenever(deviceSecurityHelper.isDeviceSecure()).thenReturn(false)
        val event = mutableListOf<OnboardingViewModel.OnBoardingUiState>()
        val viewModel = OnboardingViewModel()

        // when
        viewModel.onCreateNewWalletClick()
        viewModel.state
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.last(), OnboardingViewModel.OnBoardingUiState(showWarning = true))
    }
}

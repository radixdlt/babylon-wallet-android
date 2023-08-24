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
import rdx.works.profile.domain.backup.DiscardRestoredProfileFromBackupUseCase
import rdx.works.profile.domain.backup.IsProfileFromBackupExistsUseCase
import rdx.works.profile.domain.backup.RestoreProfileFromBackupUseCase

@ExperimentalCoroutinesApi
class OnboardingViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    //private val deviceSecurityHelper = Mockito.mock(DeviceSecurityHelper::class.java)
    private val isProfileFromBackupExistsUseCase = Mockito.mock(IsProfileFromBackupExistsUseCase::class.java)
    private val restoreProfileFromBackupUseCase = Mockito.mock(RestoreProfileFromBackupUseCase::class.java)
    private val discardRestoredProfileFromBackupUseCase = Mockito.mock(DiscardRestoredProfileFromBackupUseCase::class.java)

    @Test
    fun `when alert not accepted, do not show external warning`() = runTest {
        // given
        val event = mutableListOf<OnboardingViewModel.OnBoardingUiState>()
        val viewModel = OnboardingViewModel(
            //deviceSecurityHelper = deviceSecurityHelper,
            isProfileFromBackupExistsUseCase = isProfileFromBackupExistsUseCase,
            discardRestoredProfileFromBackupUseCase = discardRestoredProfileFromBackupUseCase
        )

        // when
        viewModel.onAlertClicked(false)
        viewModel.state
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first(), OnboardingViewModel.OnBoardingUiState(showWarning = false))
    }

    @Test
    fun `when user not authenticated successfully, do not go next and dismiss`() = runTest {
        // given
        val event = mutableListOf<OnboardingViewModel.OnBoardingUiState>()
        val viewModel = OnboardingViewModel(
            //deviceSecurityHelper = deviceSecurityHelper,
            isProfileFromBackupExistsUseCase = isProfileFromBackupExistsUseCase,
            discardRestoredProfileFromBackupUseCase = discardRestoredProfileFromBackupUseCase
        )

        // when
        viewModel.onUserAuthenticated(false)
        viewModel.state
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first(), OnboardingViewModel.OnBoardingUiState(authenticateWithBiometric = false))
    }

    @Ignore("Test is ignored until on-boarding feature is completed")
    @Test
    fun `given device is secure, when proceed button clicked, verify authentication prompt shown`() = runTest {
        // given
        //whenever(deviceSecurityHelper.isDeviceSecure()).thenReturn(true)
        val event = mutableListOf<OnboardingViewModel.OnBoardingUiState>()
        val viewModel = OnboardingViewModel(
            //deviceSecurityHelper = deviceSecurityHelper,
            isProfileFromBackupExistsUseCase = isProfileFromBackupExistsUseCase,
            discardRestoredProfileFromBackupUseCase = discardRestoredProfileFromBackupUseCase
        )

        // when
        viewModel.onProceedClick()
        viewModel.state
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.last(), OnboardingViewModel.OnBoardingUiState(authenticateWithBiometric = true))
    }

    @Ignore("Test is ignored until on-boarding feature is completed")
    @Test
    fun `given device is not secure, when proceed button clicked, show warning`() = runTest {
        // given
        //whenever(deviceSecurityHelper.isDeviceSecure()).thenReturn(false)
        val event = mutableListOf<OnboardingViewModel.OnBoardingUiState>()
        val viewModel = OnboardingViewModel(
            //deviceSecurityHelper = deviceSecurityHelper,
            isProfileFromBackupExistsUseCase = isProfileFromBackupExistsUseCase,
            discardRestoredProfileFromBackupUseCase = discardRestoredProfileFromBackupUseCase
        )

        // when
        viewModel.onProceedClick()
        viewModel.state
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.last(), OnboardingViewModel.OnBoardingUiState(showWarning = true))
    }
}

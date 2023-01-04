package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.presentation.onboarding.OnboardingViewModel
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class OnboardingViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val preferencesManager = Mockito.mock(PreferencesManager::class.java)

    private val deviceSecurityHelper = Mockito.mock(DeviceSecurityHelper::class.java)

    @Test
    fun `when alert accepted, go next`() = runTest {
        // given
        val viewModel = OnboardingViewModel(preferencesManager, deviceSecurityHelper)

        // when
        viewModel.onAlertClicked(true)

        advanceUntilIdle()

        // then
        verify(preferencesManager).setShowOnboarding(false)
    }

    @Test
    fun `when alert not accepted, do not show external warning`() = runTest {
        // given
        val event = mutableListOf<OnboardingViewModel.OnboardingUiState>()
        val viewModel = OnboardingViewModel(preferencesManager, deviceSecurityHelper)

        // when
        viewModel.onAlertClicked(false)
        viewModel.onboardingUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first(), OnboardingViewModel.OnboardingUiState(showWarning = false))
    }

    @Test
    fun `when user authenticated successfully, go next`() = runTest {
        // given
        val viewModel = OnboardingViewModel(preferencesManager, deviceSecurityHelper)

        // when
        viewModel.onUserAuthenticated(true)

        advanceUntilIdle()

        // then
        verify(preferencesManager).setShowOnboarding(false)
    }

    @Test
    fun `when user not authenticated successfully, do not go next and dismiss`() = runTest {
        // given
        val event = mutableListOf<OnboardingViewModel.OnboardingUiState>()
        val viewModel = OnboardingViewModel(preferencesManager, deviceSecurityHelper)

        // when
        viewModel.onUserAuthenticated(false)
        viewModel.onboardingUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first(), OnboardingViewModel.OnboardingUiState(authenticateWithBiometric = false))
    }

    @Test
    fun `given device is secure, when proceed button clicked, verify authentication prompt shown`() = runTest {
        // given
        whenever(deviceSecurityHelper.isDeviceSecure()).thenReturn(true)
        val event = mutableListOf<OnboardingViewModel.OnboardingUiState>()
        val viewModel = OnboardingViewModel(preferencesManager, deviceSecurityHelper)

        // when
        viewModel.onProceedClick()
        viewModel.onboardingUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.last(), OnboardingViewModel.OnboardingUiState(authenticateWithBiometric = true))
    }

    @Test
    fun `given device is not secure, when proceed button clicked, show warning`() = runTest {
        // given
        whenever(deviceSecurityHelper.isDeviceSecure()).thenReturn(false)
        val event = mutableListOf<OnboardingViewModel.OnboardingUiState>()
        val viewModel = OnboardingViewModel(preferencesManager, deviceSecurityHelper)

        // when
        viewModel.onProceedClick()
        viewModel.onboardingUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.last(), OnboardingViewModel.OnboardingUiState(showWarning = true))
    }
}
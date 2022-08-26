package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.data.DataStoreManager
import com.babylon.wallet.android.presentation.onboarding.OnboardingViewModel
import com.babylon.wallet.android.utils.SecurityHelper
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

    private val dataStoreManager = Mockito.mock(DataStoreManager::class.java)

    private val securityHelper = Mockito.mock(SecurityHelper::class.java)

    @Test
    fun `given device security is setup, when proceeding next, verify biometric auth happens`() = runTest {
        // given
        whenever(securityHelper.isDeviceSecure()).thenReturn(true)
        val event = mutableListOf<OnboardingViewModel.OnboardingUiAction>()
        val viewModel = OnboardingViewModel(dataStoreManager, securityHelper)

        // when
        viewModel.onboardingUiAction
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        viewModel.onProceedClick()

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first(), OnboardingViewModel.OnboardingUiAction.AuthenticateWithBiometric)
    }

    @Test
    fun `given device security is not setup, when proceeding next, verify security warning shown`() = runTest {
        // given
        whenever(securityHelper.isDeviceSecure()).thenReturn(false)
        val event = mutableListOf<OnboardingViewModel.OnboardingUiAction>()
        val viewModel = OnboardingViewModel(dataStoreManager, securityHelper)

        // when
        viewModel.onboardingUiAction
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        viewModel.onProceedClick()

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first(), OnboardingViewModel.OnboardingUiAction.ShowSecurityWarning)
    }

    @Test
    fun `when user authenticated, verify onboarding not shown anymore`() = runTest {
        // given
        val viewModel = OnboardingViewModel(dataStoreManager, securityHelper)

        // when
        viewModel.onUserAuthenticated()
        advanceUntilIdle()

        // then
        verify(dataStoreManager).setShowOnboarding(false)
    }
}
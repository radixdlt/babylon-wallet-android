package com.babylon.wallet.android.presentation.onboarding

import app.cash.turbine.test
import com.babylon.wallet.android.presentation.StateViewModelTest
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.cloudbackup.data.GoogleSignInManager

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest : StateViewModelTest<OnboardingViewModel>() {

    private val preferencesManager = mockk<PreferencesManager>()
    private val googleSignInManager = mockk<GoogleSignInManager>().apply {
        coEvery { signOut() } just Runs
    }

    override fun initVM(): OnboardingViewModel {
        return OnboardingViewModel(googleSignInManager, preferencesManager)
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { preferencesManager.isEulaAccepted } returns flowOf(true)
        every { googleSignInManager.isSignedIn() } returns false
    }

    @Test
    fun `if eula accepted revoke GDrive access on init`() = runTest {
        vm.value
        advanceUntilIdle()
        coVerify(exactly = 1) { googleSignInManager.signOut() }
    }

    @Test
    fun `if eula not accepted don't revoke GDrive access on init`() = runTest {
        every { preferencesManager.isEulaAccepted } returns flowOf(false)
        vm.value
        advanceUntilIdle()
        coVerify(exactly = 0) { googleSignInManager.signOut() }
    }

    @Test
    fun `onCreateNewWalletClick eula accepted`() = runTest {
        // Given
        coEvery { preferencesManager.isEulaAccepted } returns flowOf(true)
        // When
        vm.value.onCreateNewWalletClick()
        advanceUntilIdle()
        // Then
        vm.value.oneOffEvent.test {
            val event = expectMostRecentItem()
            assert(event is OnboardingViewModel.OnboardingEvent.NavigateToCreateNewWallet)
        }
    }

    @Test
    fun `onCreateNewWalletClick eula not accepted`() = runTest {
        // Given
        coEvery { preferencesManager.isEulaAccepted } returns flowOf(false)
        // When
        vm.value.onCreateNewWalletClick()
        advanceUntilIdle()
        // Then
        vm.value.oneOffEvent.test {
            val event = expectMostRecentItem()
            assert(event is OnboardingViewModel.OnboardingEvent.NavigateToEula)
        }
    }
}
package com.babylon.wallet.android.presentation.onboarding

import app.cash.turbine.test
import com.babylon.wallet.android.presentation.TestDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.cloudbackup.data.GoogleSignInManager

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private lateinit var vm: OnboardingViewModel

    private val preferencesManager = mockk<PreferencesManager>()
    private val googleSignInManager = mockk<GoogleSignInManager>()

    @Before
    fun setUp() {
        every { googleSignInManager.isSignedIn() } returns false
        vm = OnboardingViewModel(
            preferencesManager = preferencesManager,
            googleSignInManager = googleSignInManager
        )
    }

    @Test
    fun `onCreateNewWalletClick eula accepted`() = runTest {
        // Given
        coEvery { preferencesManager.isEulaAccepted } returns flowOf(true)
        // When
        vm.onCreateNewWalletClick()
        advanceUntilIdle()
        // Then
        vm.oneOffEvent.test {
            val event = expectMostRecentItem()
            assert(event is OnboardingViewModel.OnboardingEvent.NavigateToCreateNewWallet)
        }
    }

    @Test
    fun `onCreateNewWalletClick eula not accepted`() = runTest {
        // Given
        coEvery { preferencesManager.isEulaAccepted } returns flowOf(false)
        // When
        vm.onCreateNewWalletClick()
        advanceUntilIdle()
        // Then
        vm.oneOffEvent.test {
            val event = expectMostRecentItem()
            assert(event is OnboardingViewModel.OnboardingEvent.NavigateToEula)
        }
    }
}
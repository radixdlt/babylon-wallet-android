package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.MainViewModel
import com.babylon.wallet.android.DataStoreManager
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MainViewModelTest {

    private val preferencesManager = Mockito.mock(DataStoreManager::class.java)

    @Test
    fun `given show onboarding, when view model asked, verify onboarding shown`() {
        // given
        whenever(preferencesManager.showOnboarding).thenReturn(true)
        val viewModel = MainViewModel(preferencesManager)

        // when
        val showOnboarding = viewModel.showOnboarding()

        // then
        verify(preferencesManager).showOnboarding
        Assert.assertEquals(showOnboarding, true)
    }

    @Test
    fun `given do not show onboarding, when view model asked, verify onboarding not shown`() {
        // given
        whenever(preferencesManager.showOnboarding).thenReturn(false)
        val viewModel = MainViewModel(preferencesManager)

        // when
        val showOnboarding = viewModel.showOnboarding()

        // then
        verify(preferencesManager).showOnboarding
        Assert.assertEquals(showOnboarding, false)
    }

}
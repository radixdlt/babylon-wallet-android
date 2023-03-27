@file:OptIn(ExperimentalCoroutinesApi::class)
package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.TestDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.profile.data.repository.PersonaRepository

internal class PersonaDataOngoingViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()

    private val samplePersona = SampleDataProvider().samplePersona()

    fun initVM(): PersonaDataOngoingViewModel {
        return PersonaDataOngoingViewModel(
            savedStateHandle,
            personaRepository
        )
    }

    @Before
    fun setUp() {
        every { savedStateHandle.get<String>(ARG_PERSONA_ID) } returns samplePersona.address
        every { savedStateHandle.get<String>(ARG_REQUIRED_FIELDS) } returns "GivenName"
        coEvery { personaRepository.getPersonaByAddressFlow(any()) } returns flow {
            emit(samplePersona)
        }
    }

    @Test
    fun `initial state is set up properly when fields are not missing`() = runTest {
        val vm = initVM()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.persona?.persona?.address == samplePersona.address)
            assert(item.continueButtonEnabled)
        }
    }

    @Test
    fun `initial state is set up properly when fields are missing`() = runTest {
        every { savedStateHandle.get<String>(ARG_REQUIRED_FIELDS) } returns "PhoneNumber"
        val vm = initVM()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.persona?.persona?.address == samplePersona.address)
            assert(!item.continueButtonEnabled)
        }
    }

}
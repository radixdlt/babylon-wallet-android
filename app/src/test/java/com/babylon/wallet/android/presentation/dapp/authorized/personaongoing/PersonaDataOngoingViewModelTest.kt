@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.RequiredPersonaField
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.TestDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.core.identifiedArrayListOf
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.domain.GetProfileUseCase

internal class PersonaDataOngoingViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()

    private val samplePersona = SampleDataProvider().samplePersona()

    fun initVM(): PersonaDataOngoingViewModel {
        return PersonaDataOngoingViewModel(
            savedStateHandle,
            getProfileUseCase
        )
    }

    @Before
    fun setUp() {
        every { savedStateHandle.get<String>(ARG_PERSONA_ID) } returns samplePersona.address
        every { savedStateHandle.get<RequiredPersonaFields>(ARG_REQUIRED_FIELDS) } returns RequiredPersonaFields(
            fields = listOf(
                RequiredPersonaField(
                    PersonaData.PersonaDataField.Kind.Name,
                    MessageFromDataChannel.IncomingRequest.NumberOfValues(
                        1,
                        MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
                    )
                )
            )
        )
        every { getProfileUseCase() } returns flowOf(profile(personas = identifiedArrayListOf(samplePersona)))
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
        every { savedStateHandle.get<RequiredPersonaFields>(ARG_REQUIRED_FIELDS) } returns RequiredPersonaFields(
            fields = listOf(
                RequiredPersonaField(
                    PersonaData.PersonaDataField.Kind.PhoneNumber,
                    MessageFromDataChannel.IncomingRequest.NumberOfValues(
                        1,
                        MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
                    )
                )
            )
        )
        val vm = initVM()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.persona?.persona?.address == samplePersona.address)
            assert(!item.continueButtonEnabled)
        }
    }

}

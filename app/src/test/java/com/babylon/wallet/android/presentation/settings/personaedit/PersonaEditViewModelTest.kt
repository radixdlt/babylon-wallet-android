package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.RequiredPersonaField
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.utils.isValidEmail
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.persona.UpdatePersonaUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaEditViewModelTest : StateViewModelTest<PersonaEditViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val updatePersonaUseCase = mockk<UpdatePersonaUseCase>()

    private val nameFieldId = "1"
    private val emailFieldId = "2"

    override fun initVM(): PersonaEditViewModel {
        return PersonaEditViewModel(getProfileUseCase, updatePersonaUseCase, savedStateHandle)
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_PERSONA_ADDRESS) } returns "1"
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
        mockkStatic("com.babylon.wallet.android.utils.StringExtensionsKt")
        every { any<String>().isValidEmail() } returns true
        coEvery { updatePersonaUseCase(any()) } just Runs
        every { getProfileUseCase() } returns flowOf(
            profile(
                personas = listOf(
                    SampleDataProvider().samplePersona("1")
                )
            )
        )
    }

    private fun <T> StateFlow<T>.whileCollecting(action: suspend () -> Unit) = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { collect {} }
        action()
        collectJob.cancel()
    }

    @Test
    fun `init load persona and it's fields`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.persona?.address == "1")
                assert(item.currentFields.size == 2)
                assert(item.fieldsToAdd.size == 1)
            }
        }
    }

    @Test
    fun `save triggers proper useCase`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onSave()
            advanceUntilIdle()
            val persona = slot<Network.Persona>()
            coVerify(exactly = 1) { updatePersonaUseCase(capture(persona)) }
            assert(persona.captured.personaData.name != null)
        }
    }

    @Test
    fun `delete field updates state`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onDeleteField(emailFieldId)
            advanceUntilIdle()
            vm.onSave()
            advanceUntilIdle()
            val persona = slot<Network.Persona>()
            coVerify(exactly = 1) { updatePersonaUseCase(capture(persona)) }
            assert(persona.captured.personaData.name != null)
            assert(persona.captured.personaData.emailAddresses.isEmpty())
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.size == 1)
                assert(item.fieldsToAdd.size == 2)
            }

        }
    }

    @Test
    fun `field value change update state`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onFieldValueChanged(emailFieldId, PersonaData.PersonaDataField.Email("jakub@jakub.pl"))
            advanceUntilIdle()
            vm.onFieldValueChanged(
                nameFieldId,
                PersonaData.PersonaDataField.Name(
                    variant = PersonaData.PersonaDataField.Name.Variant.Western,
                    given = "jakub",
                    family = "",
                    nickname = ""
                )
            )
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(
                    item.currentFields.map {
                        it.entry
                    }.filter {
                        it.value.kind == PersonaData.PersonaDataField.Kind.EmailAddress
                    }.map { it.value as PersonaData.PersonaDataField.Email }.first().value == "jakub@jakub.pl"
                )
                assert(
                    item.currentFields.map {
                        it.entry
                    }.filter {
                        it.value.kind == PersonaData.PersonaDataField.Kind.Name
                    }.map { it.value as PersonaData.PersonaDataField.Name }.first().given == "jakub"
                )
            }
        }
    }

}
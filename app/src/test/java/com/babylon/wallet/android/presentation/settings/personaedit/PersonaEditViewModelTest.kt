package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.BaseViewModelTest
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.domain.UpdatePersonaUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaEditViewModelTest : BaseViewModelTest<PersonaEditViewModel>() {

    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val updatePersonaUseCase = mockk<UpdatePersonaUseCase>()

    override fun initVM(): PersonaEditViewModel {
        return PersonaEditViewModel(personaRepository, updatePersonaUseCase, savedStateHandle)
    }

    @Before
    override fun setUp() {
        super.setUp()
        val addressSlot = slot<String>()
        every { savedStateHandle.get<String>(com.babylon.wallet.android.presentation.settings.personadetail.ARG_PERSONA_ADDRESS) } returns "1"
        mockkStatic("com.babylon.wallet.android.utils.StringExtensionsKt")
        every { any<String>().isValidEmail() } returns true
        coEvery { updatePersonaUseCase(any()) } just Runs
        coEvery { personaRepository.getPersonaByAddressFlow(capture(addressSlot)) } answers {
            flow {
                emit(SampleDataProvider().samplePersona(addressSlot.captured))
            }
        }
    }

    fun <T> StateFlow<T>.whileCollecting(action: suspend () -> Unit) = runTest {
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
                assert(item.fieldsToAdd.size == OnNetwork.Persona.Field.Kind.values().size - 2)
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
            val persona = slot<OnNetwork.Persona>()
            coVerify(exactly = 1) { updatePersonaUseCase(capture(persona)) }
            assert(persona.captured.fields.size == 2)
        }
    }

    @Test
    fun `delete field updates state`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onDeleteField(OnNetwork.Persona.Field.Kind.Email)
            advanceUntilIdle()
            vm.onSave()
            advanceUntilIdle()
            val persona = slot<OnNetwork.Persona>()
            coVerify(exactly = 1) { updatePersonaUseCase(capture(persona)) }
            assert(persona.captured.fields.size == 1)
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.size == 1)
                assert(item.fieldsToAdd.size == OnNetwork.Persona.Field.Kind.values().size - 1)
            }

        }
    }

    @Test
    fun `field value change update state`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.Email, "jakub@jakub.pl")
            advanceUntilIdle()
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.FirstName, "jakub")
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.firstOrNull { it.kind == OnNetwork.Persona.Field.Kind.Email }?.value == "jakub@jakub.pl")
                assert(item.currentFields.firstOrNull { it.kind == OnNetwork.Persona.Field.Kind.FirstName }?.value == "jakub")
            }
        }
    }

    @Test
    fun `field selection enables Add button`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onSelectionChanged(OnNetwork.Persona.Field.Kind.LastName, true)
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.addButtonEnabled)
            }
            vm.onSelectionChanged(OnNetwork.Persona.Field.Kind.LastName, false)
            vm.state.test {
                val item = expectMostRecentItem()
                assert(!item.addButtonEnabled)
            }
        }
    }

    @Test
    fun `adding field changes current fields`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onSelectionChanged(OnNetwork.Persona.Field.Kind.LastName, true)
            vm.onAddFields()
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.size == 3)
            }
        }
    }

    @Test
    fun `field validation works`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onSelectionChanged(OnNetwork.Persona.Field.Kind.LastName, true)
            vm.onSelectionChanged(OnNetwork.Persona.Field.Kind.PersonalIdentificationNumber, true)
            vm.onSelectionChanged(OnNetwork.Persona.Field.Kind.ZipCode, true)
            vm.onAddFields()
            advanceUntilIdle()
            vm.onDisplayNameChanged("jakub")
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.FirstName, "66666")
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.LastName, "66666")
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.Email, "jakub@jakub.pl")
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.PersonalIdentificationNumber, "66666")
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.ZipCode, "66666")
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.size == 5)
                assert(item.currentFields.all { it.valid == true })
            }
            vm.onDisplayNameChanged("")
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(!item.saveButtonEnabled)
            }
            every { any<String>().isValidEmail() } returns false
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.FirstName, "")
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.LastName, "")
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.Email, "jakubjakub.pl")
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.PersonalIdentificationNumber, "")
            vm.onValueChanged(OnNetwork.Persona.Field.Kind.ZipCode, "")
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.all { it.valid == false })
            }
        }
    }
}
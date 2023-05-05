package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.model.encodeToString
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
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.persona.UpdatePersonaUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaEditViewModelTest : StateViewModelTest<PersonaEditViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val updatePersonaUseCase = mockk<UpdatePersonaUseCase>()

    override fun initVM(): PersonaEditViewModel {
        return PersonaEditViewModel(getProfileUseCase, updatePersonaUseCase, savedStateHandle)
    }

    @Before
    override fun setUp() {
        super.setUp()

        every { savedStateHandle.get<String>(ARG_PERSONA_ADDRESS) } returns "1"
        every { savedStateHandle.get<String>(ARG_REQUIRED_FIELDS) } returns listOf(Network.Persona.Field.ID.GivenName).encodeToString()
        mockkStatic("com.babylon.wallet.android.utils.StringExtensionsKt")
        every { any<String>().isValidEmail() } returns true
        coEvery { updatePersonaUseCase(any()) } just Runs
        every { getProfileUseCase() } returns flowOf(profile(personas = listOf(
            SampleDataProvider().samplePersona("1")
        )))
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
                assert(item.fieldsToAdd.size == Network.Persona.Field.ID.values().size - 2)
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
            assert(persona.captured.fields.size == 2)
        }
    }

    @Test
    fun `delete field updates state`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onDeleteField(Network.Persona.Field.ID.EmailAddress)
            advanceUntilIdle()
            vm.onSave()
            advanceUntilIdle()
            val persona = slot<Network.Persona>()
            coVerify(exactly = 1) { updatePersonaUseCase(capture(persona)) }
            assert(persona.captured.fields.size == 1)
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.size == 1)
                assert(item.fieldsToAdd.size == Network.Persona.Field.ID.values().size - 1)
            }

        }
    }

    @Test
    fun `field value change update state`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onFieldValueChanged(Network.Persona.Field.ID.EmailAddress, "jakub@jakub.pl")
            advanceUntilIdle()
            vm.onFieldValueChanged(Network.Persona.Field.ID.GivenName, "jakub")
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.firstOrNull { it.id == Network.Persona.Field.ID.EmailAddress }?.value == "jakub@jakub.pl")
                assert(item.currentFields.firstOrNull { it.id == Network.Persona.Field.ID.GivenName }?.value == "jakub")
            }
        }
    }

    @Test
    fun `adding field changes current fields`() = runTest {
        val vm = vm.value
        vm.state.whileCollecting {
            advanceUntilIdle()
            vm.onSelectionChanged(Network.Persona.Field.ID.FamilyName, true)
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
            vm.onSelectionChanged(Network.Persona.Field.ID.FamilyName, true)
            vm.onSelectionChanged(Network.Persona.Field.ID.GivenName, true)
            vm.onSelectionChanged(Network.Persona.Field.ID.EmailAddress, true)
            vm.onAddFields()
            advanceUntilIdle()
            vm.onDisplayNameChanged("jakub")
            vm.onFieldValueChanged(Network.Persona.Field.ID.GivenName, "66666")
            vm.onFieldValueChanged(Network.Persona.Field.ID.FamilyName, "66666")
            vm.onFieldValueChanged(Network.Persona.Field.ID.EmailAddress, "jakub@jakub.pl")
            vm.onFieldValueChanged(Network.Persona.Field.ID.PhoneNumber, "123456789")
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.size == 3)
                assert(item.currentFields.all { it.valid == true })
            }
            vm.onDisplayNameChanged("")
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(!item.saveButtonEnabled)
            }
            every { any<String>().isValidEmail() } returns false
            vm.onFieldValueChanged(Network.Persona.Field.ID.GivenName, "")
            vm.onFieldValueChanged(Network.Persona.Field.ID.FamilyName, "")
            vm.onFieldValueChanged(Network.Persona.Field.ID.EmailAddress, "jakubjakub.pl")
            vm.onFieldValueChanged(Network.Persona.Field.ID.PhoneNumber, "")
            advanceUntilIdle()
            vm.state.test {
                val item = expectMostRecentItem()
                assert(item.currentFields.all { it.valid == false })
            }
        }
    }
}

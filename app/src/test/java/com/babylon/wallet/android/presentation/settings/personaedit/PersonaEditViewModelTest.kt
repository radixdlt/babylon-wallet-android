package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.fakes.UpdatePersonaUseCaseFake
import com.babylon.wallet.android.presentation.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.domain.UpdatePersonaUseCase

internal class PersonaEditViewModelTest : BaseViewModelTest<PersonaEditViewModel>() {

    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val updatePersonaUseCase = spyk<UpdatePersonaUseCase> {
        UpdatePersonaUseCaseFake()
    }

    override fun initVM(): PersonaEditViewModel {
        return PersonaEditViewModel(personaRepository, updatePersonaUseCase, savedStateHandle)
    }

    @Before
    override fun setUp() {
        super.setUp()
        val addressSlot = slot<String>()
        every { savedStateHandle.get<String>(com.babylon.wallet.android.presentation.settings.personadetail.ARG_PERSONA_ADDRESS) } returns "1"
        coEvery { personaRepository.getPersonaByAddressFlow(capture(addressSlot)) } answers {
            flow {
                emit(SampleDataProvider().samplePersona(addressSlot.captured))
            }
        }
    }

    @Test
    fun `init load persona and dapps`() = runTest {
    }
}
package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.BaseViewModelTest
import com.babylon.wallet.android.presentation.model.encodeToString
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.PersonaRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaDataOnetimeViewModelTest : BaseViewModelTest<PersonaDataOnetimeViewModel>() {

    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()

    private val samplePersona = SampleDataProvider().samplePersona()

    override fun initVM(): PersonaDataOnetimeViewModel {
        return PersonaDataOnetimeViewModel(
            savedStateHandle,
            personaRepository
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        mockkStatic("com.babylon.wallet.android.presentation.model.PersonaExtensionsKt")
        every { any<List<Network.Persona.Field.Kind>>().encodeToString() } returns "GivenName"
        every { savedStateHandle.get<String>(ARG_REQUIRED_FIELDS) } returns "GivenName"
        coEvery { personaRepository.personas } returns flow {
            emit(listOf(samplePersona))
        }
    }

    @Test
    fun `initial state is set up properly`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.personaListToDisplay.size == 1)
            assert(item.requiredFields.size == 1 && item.requiredFields.first() == Network.Persona.Field.Kind.GivenName)
        }
    }

    @Test
    fun `selecting persona enables continue button`() = runTest {
        val vm = vm.value
        vm.onSelectPersona(samplePersona)
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.continueButtonEnabled)
        }
    }

    @Test
    fun `edit click triggers edit action with proper required fields`() = runTest {
        val vm = vm.value
        vm.onEditClick(samplePersona.address)
        advanceUntilIdle()
        val item = vm.oneOffEvent.first()
        assert(item is PersonaDataOnetimeEvent.OnEditPersona && item.requiredFieldsEncoded == "GivenName")
    }

}
package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.BaseViewModelTest
import com.babylon.wallet.android.presentation.model.encodeToString
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaDataOnetimeViewModelTest : BaseViewModelTest<PersonaDataOnetimeViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val samplePersona = sampleDataProvider.samplePersona()

    override fun initVM(): PersonaDataOnetimeViewModel {
        return PersonaDataOnetimeViewModel(
            savedStateHandle,
            getProfileUseCase,
            preferencesManager
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_REQUIRED_FIELDS) } returns listOf(Network.Persona.Field.Kind.GivenName).encodeToString()
        coEvery { preferencesManager.firstPersonaCreated } returns flow {
            emit(true)
        }
        coEvery { getProfileUseCase() } returns flowOf(
            profile(personas = listOf(samplePersona))
        )
    }

    @Test
    fun `initial state is set up properly`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.personaListToDisplay.size == 1)
            assert(item.personaListToDisplay.first().missingFieldKinds().size == 0)
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

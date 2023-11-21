package com.babylon.wallet.android.presentation.dapp.selectpersona

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.ARG_DAPP_DEFINITION_ADDRESS
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.SelectPersonaViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.core.identifiedArrayListOf
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class SelectPersonaViewModelTest : StateViewModelTest<SelectPersonaViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val dAppConnectionRepository = DAppConnectionRepositoryFake()

    override fun initVM(): SelectPersonaViewModel {
        return SelectPersonaViewModel(
            savedStateHandle,
            dAppConnectionRepository,
            getProfileUseCase,
            preferencesManager
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        coEvery { preferencesManager.firstPersonaCreated } returns flow {
            emit(true)
        }
        every { savedStateHandle.get<String>(ARG_DAPP_DEFINITION_ADDRESS) } returns "address1"
        every { getProfileUseCase() } returns flowOf(
            profile(
                personas = identifiedArrayListOf(
                    SampleDataProvider().samplePersona("address1"),
                    SampleDataProvider().samplePersona("address2")
                )
            )
        )
    }

    @Test
    fun `connected dapp exist and has authorized persona`() = runTest {
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.continueButtonEnabled)
            assert(item.personaListToDisplay.size == 2)
            val onePersonaAuthorized = item.personaListToDisplay.count { it.lastUsedOn != null } == 1
            assert(onePersonaAuthorized)
        }
    }

    @Test
    fun `connected dapp does not exist`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(!item.continueButtonEnabled)
            assert(item.personaListToDisplay.size == 2)
            val noPersonaAuthorized = item.personaListToDisplay.all { it.lastUsedOn == null }
            assert(noPersonaAuthorized)
        }
    }

}

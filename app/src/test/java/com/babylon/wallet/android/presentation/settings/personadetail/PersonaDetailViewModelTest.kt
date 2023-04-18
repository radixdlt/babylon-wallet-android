package com.babylon.wallet.android.presentation.settings.personadetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaDetailViewModelTest : StateViewModelTest<PersonaDetailViewModel>() {

    private val dAppConnectionRepository = DAppConnectionRepositoryFake()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()

    override fun initVM(): PersonaDetailViewModel {
        return PersonaDetailViewModel(dAppConnectionRepository, getProfileUseCase, savedStateHandle)
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_PERSONA_ADDRESS) } returns "1"
        every { getProfileUseCase() } returns flowOf(profile(personas = listOf(
            SampleDataProvider().samplePersona("1")
        )))
    }

    @Test
    fun `init load persona and dapps`() = runTest {
        val vm = vm.value
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.persona?.address == "1")
            assert(item.authorizedDapps.size == 2)
        }
        collectJob.cancel()
    }
}

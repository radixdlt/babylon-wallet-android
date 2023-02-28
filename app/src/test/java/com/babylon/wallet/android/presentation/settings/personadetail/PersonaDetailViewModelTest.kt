package com.babylon.wallet.android.presentation.settings.personadetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.presentation.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.repository.PersonaRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaDetailViewModelTest : BaseViewModelTest<PersonaDetailViewModel>() {

    private val dAppConnectionRepository = DAppConnectionRepositoryFake()
    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()

    override fun initVM(): PersonaDetailViewModel {
        return PersonaDetailViewModel(dAppConnectionRepository, personaRepository, savedStateHandle)
    }

    @Before
    override fun setUp() {
        super.setUp()
        val addressSlot = slot<String>()
        every { savedStateHandle.get<String>(ARG_PERSONA_ADDRESS) } returns "1"
        coEvery { personaRepository.getPersonaByAddressFlow(capture(addressSlot)) } answers {
            flow {
                emit(SampleDataProvider().samplePersona(addressSlot.captured))
            }
        }
    }

    @Test
    fun `init load persona and dapps`() = runTest {
        val vm = vm.value
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.persona?.address == "1")
            assert(item.connectedDapps.size == 2)
        }
        collectJob.cancel()
    }
}
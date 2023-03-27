package com.babylon.wallet.android.presentation.settings.dappdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.fakes.AccountRepositoryFake
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.fakes.DappMetadataRepositoryFake
import com.babylon.wallet.android.presentation.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.repository.PersonaRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class DappDetailViewModelTest : BaseViewModelTest<DappDetailViewModel>() {

    private val dAppConnectionRepository = DAppConnectionRepositoryFake().apply {
        state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
    }
    private val dappMetadataRepository = DappMetadataRepositoryFake()
    private val accountRepository = AccountRepositoryFake()
    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val samplePersonas = listOf(
        sampleDataProvider.samplePersona("address1"),
        sampleDataProvider.samplePersona(sampleDataProvider.randomAddress())
    )

    override fun initVM(): DappDetailViewModel {
        return DappDetailViewModel(
            dAppConnectionRepository,
            dappMetadataRepository,
            personaRepository,
            accountRepository,
            savedStateHandle
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        val addressSlot = slot<String>()
        every { savedStateHandle.get<String>(ARG_DAPP_ADDRESS) } returns "address1"
        coEvery { personaRepository.getPersonaByAddress(capture(addressSlot)) } answers {
            sampleDataProvider.samplePersona(addressSlot.captured)
        }
        coEvery { personaRepository.personas } returns flow {
            emit(samplePersonas)
        }
    }

    @Test
    fun `init load dapp data into state`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.dappMetadata != null)
            assert(item.dapp != null)
            assert(item.personas.size == 1)
        }
    }

    @Test
    fun `null dapp emission after delete closes screen`() = runTest {
        val vm = vm.value
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.NoDapp
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is DappDetailEvent.LastPersonaDeleted)
        }
    }

    @Test
    fun `persona click sets persona detail data`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onPersonaClick(samplePersonas[0])
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.selectedPersona?.persona == samplePersonas[0])
            assert(item.sharedPersonaAccounts.size == 1)
        }
    }

    @Test
    fun `persona details closed clear selected persona state`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onPersonaClick(samplePersonas[0])
        advanceUntilIdle()
        vm.onPersonaDetailsClosed()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.selectedPersona == null)
            assert(item.sharedPersonaAccounts.size == 0)
        }
    }

    @Test
    fun `dapp deletion call repo and trigger proper one off event`() = runTest {
        val vm = vm.value
        vm.onDeleteDapp()
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is DappDetailEvent.DappDeleted)
        }
    }

}
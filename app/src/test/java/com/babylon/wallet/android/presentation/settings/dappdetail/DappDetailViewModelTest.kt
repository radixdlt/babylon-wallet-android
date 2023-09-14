package com.babylon.wallet.android.presentation.settings.dappdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.usecases.GetDAppWithMetadataAndAssociatedResourcesUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.ARG_DAPP_ADDRESS
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.DappDetailEvent
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.DappDetailViewModel
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.SelectedSheetState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.RequestedNumber
import rdx.works.profile.data.model.pernetwork.Shared
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class DappDetailViewModelTest : StateViewModelTest<DappDetailViewModel>() {

    private val incomingRequestRepository = IncomingRequestRepositoryImpl()
    private val dAppConnectionRepository = DAppConnectionRepositoryFake().apply {
        state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
    }
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getDAppWithAssociatedResourcesUseCase = mockk<GetDAppWithMetadataAndAssociatedResourcesUseCase>()
    private val samplePersonas = listOf(
        sampleDataProvider.samplePersona("address1"),
        sampleDataProvider.samplePersona(sampleDataProvider.randomAddress())
    )

    override fun initVM(): DappDetailViewModel {
        return DappDetailViewModel(
            dAppConnectionRepository,
            getDAppWithAssociatedResourcesUseCase,
            getProfileUseCase,
            incomingRequestRepository,
            savedStateHandle
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_DAPP_ADDRESS) } returns "address1"
        every { getProfileUseCase() } returns flowOf(
            profile(
                personas = samplePersonas, dApps = listOf(
                    Network.AuthorizedDapp(
                        11, "1", "dApp", listOf(
                            Network.AuthorizedDapp.AuthorizedPersonaSimple(
                                identityAddress = "address1",
                                sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData(),
                                lastLogin = "2023-01-31T10:28:14Z",
                                sharedAccounts = Shared(
                                    listOf("address-acc-1"),
                                    RequestedNumber(
                                        RequestedNumber.Quantifier.AtLeast,
                                        1
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        coEvery { getDAppWithAssociatedResourcesUseCase("address1", false) } returns
                Result.success(
                    SampleDataProvider().sampleDAppWithResources()
                )
    }

    @Test
    fun `init load dapp data into state`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.dappWithMetadata != null)
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
        val expectedPersona = samplePersonas[0].toUiModel()
        val vm = vm.value
        advanceUntilIdle()
        vm.onPersonaClick(samplePersonas[0])
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert((item.selectedSheetState as SelectedSheetState.SelectedPersona).persona == expectedPersona)
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
            assert((item.selectedSheetState as SelectedSheetState.SelectedPersona).persona == null)
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

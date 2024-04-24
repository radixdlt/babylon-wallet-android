package com.babylon.wallet.android.presentation.settings.dappdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.usecases.GetDAppWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatedDAppWebsiteUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.ARG_DAPP_ADDRESS
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DappDetailEvent
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DappDetailViewModel
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.SelectedSheetState
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.extensions.discriminant
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.core.domain.DApp
import rdx.works.core.identifiedArrayListOf
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.RequestedNumber
import rdx.works.profile.data.model.pernetwork.Shared
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class ApprovedDappsViewModelTest : StateViewModelTest<DappDetailViewModel>() {

    private val incomingRequestRepository = IncomingRequestRepositoryImpl()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getDAppWithAssociatedResourcesUseCase = mockk<GetDAppWithResourcesUseCase>()
    private val getValidatedDAppWebsiteUseCase = mockk<GetValidatedDAppWebsiteUseCase>()
    private val samplePersonas = identifiedArrayListOf(
        sampleDataProvider.samplePersona(IdentityAddress.sampleMainnet().string),
        sampleDataProvider.samplePersona(IdentityAddress.sampleMainnet.random().string)
    )
    private val dApp = DApp.sampleMainnet()
    private val authorizedDapp = Network.AuthorizedDapp(
        networkID = dApp.dAppAddress.networkId.discriminant.toInt(),
        dAppDefinitionAddress = dApp.dAppAddress.string,
        displayName = dApp.name,
        referencesToAuthorizedPersonas = listOf(
            Network.AuthorizedDapp.AuthorizedPersonaSimple(
                identityAddress = samplePersonas[0].address,
                sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData(),
                lastLogin = "2023-01-31T10:28:14Z",
                sharedAccounts = Shared(
                    listOf(AccountAddress.sampleMainnet().string),
                    RequestedNumber(
                        RequestedNumber.Quantifier.AtLeast,
                        1
                    )
                )
            )
        )
    )
    private val dAppConnectionRepository = DAppConnectionRepositoryFake().apply {
        this.savedDApp = authorizedDapp
        state = DAppConnectionRepositoryFake.InitialState.SavedDapp
    }

    override fun initVM(): DappDetailViewModel {
        return DappDetailViewModel(
            dAppConnectionRepository,
            getDAppWithAssociatedResourcesUseCase,
            getValidatedDAppWebsiteUseCase,
            getProfileUseCase,
            incomingRequestRepository,
            savedStateHandle
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        val dApp = DApp.sampleMainnet()
        every { savedStateHandle.get<String>(ARG_DAPP_ADDRESS) } returns dApp.dAppAddress.string
        every { getProfileUseCase() } returns flowOf(
            profile(
                accounts = identifiedArrayListOf(
                    account(address = AccountAddress.sampleMainnet()),
                    account()
                ),
                personas = samplePersonas,
                dApps = listOf(

                )
            )
        )
        coEvery { getDAppWithAssociatedResourcesUseCase(dApp.dAppAddress, false) } returns
                Result.success(DAppWithResources(dApp = dApp))
    }

    @Test
    fun `init load dapp data into state`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.dAppWithResources != null)
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
            assert(item.selectedSheetState == null)
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
package com.babylon.wallet.android.presentation.settings.authorizeddapps

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.usecases.GetDAppWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatedDAppWebsiteUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.ARG_DAPP_ADDRESS
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DappDetailEvent
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DappDetailViewModel
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.SelectedSheetState
import com.babylon.wallet.android.utils.AppEventBusImpl
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedPersonaSimple
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.RequestedQuantity
import com.radixdlt.sargon.SharedPersonaData
import com.radixdlt.sargon.SharedToDappWithPersonaAccountAddresses
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.AuthorizedDapps
import com.radixdlt.sargon.extensions.ReferencesToAuthorizedPersonas
import com.radixdlt.sargon.extensions.atLeast
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
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
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class ApprovedDappsViewModelTest : StateViewModelTest<DappDetailViewModel>() {

    private val incomingRequestRepository = IncomingRequestRepositoryImpl(AppEventBusImpl())
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getDAppWithAssociatedResourcesUseCase = mockk<GetDAppWithResourcesUseCase>()
    private val getValidatedDAppWebsiteUseCase = mockk<GetValidatedDAppWebsiteUseCase>()

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities().let {
        val mainnet = it.networks.asIdentifiable().getBy(NetworkId.MAINNET)!!
        it.copy(
            networks = it.networks.asIdentifiable().updateOrAppend(
                mainnet.copy(authorizedDapps = AuthorizedDapps().asList())
            ).asList()
        )
    }
    private val samplePersonas = profile.currentNetwork!!.personas
    private val dApp = DApp.sampleMainnet()
    private val authorizedDapp = AuthorizedDapp(
        networkId = dApp.dAppAddress.networkId,
        dappDefinitionAddress = dApp.dAppAddress,
        displayName = dApp.name,
        referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas(
            AuthorizedPersonaSimple(
                identityAddress = samplePersonas[0].address,
                sharedPersonaData = SharedPersonaData(null, null, null),
                lastLogin = Timestamp.parse("2023-01-31T10:28:14Z"),
                sharedAccounts = SharedToDappWithPersonaAccountAddresses(
                    request = RequestedQuantity.atLeast(1),
                    ids = listOf(profile.currentNetwork!!.accounts.first().address)
                )
            )
        ).asList()
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
        every { getProfileUseCase.flow } returns flowOf(profile)
        coEvery { getProfileUseCase() } returns profile
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

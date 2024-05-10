package com.babylon.wallet.android.presentation.settings.personadetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.settings.personas.personadetail.ARG_PERSONA_ADDRESS
import com.babylon.wallet.android.presentation.settings.personas.personadetail.PersonaDetailViewModel
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.core.domain.DApp
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonaDetailViewModelTest : StateViewModelTest<PersonaDetailViewModel>() {

    private val dAppConnectionRepository = DAppConnectionRepositoryFake()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getDAppsUseCase = mockk<GetDAppsUseCase>()
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val changeEntityVisibilityUseCase = mockk<ChangeEntityVisibilityUseCase>()
    private val addAuthSigningFactorInstanceUseCase = mockk<AddAuthSigningFactorInstanceUseCase>()
    private val rolaClient = mockk<ROLAClient>()
    private val eventBus = mockk<AppEventBus>()

    val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
    val persona = profile.networks.asIdentifiable().getBy(NetworkId.MAINNET)!!.personas.first()

    override fun initVM(): PersonaDetailViewModel {
        return PersonaDetailViewModel(
            dAppConnectionRepository,
            getProfileUseCase,
            eventBus,
            addAuthSigningFactorInstanceUseCase,
            rolaClient,
            incomingRequestRepository,
            getDAppsUseCase,
            savedStateHandle,
            changeEntityVisibilityUseCase
        )
    }

    @Before
    override fun setUp() {
        super.setUp()

        every { savedStateHandle.get<String>(ARG_PERSONA_ADDRESS) } returns persona.address.string
        every { getProfileUseCase.flow } returns flowOf(profile)
        val dApp = DApp.sampleMainnet()
        val dAppOther = DApp.sampleMainnet.other()
        coEvery { getDAppsUseCase(dApp.dAppAddress, false) } returns Result.success(dApp)
        coEvery { getDAppsUseCase(dAppOther.dAppAddress, false) } returns Result.success(dAppOther)
    }

    @Test
    fun `init load persona and dapps`() = runTest {
        every { eventBus.events } returns MutableSharedFlow()
        val vm = vm.value
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.persona?.address == persona.address)
            assert(item.authorizedDapps.size == 2)
        }
        collectJob.cancel()
    }
}

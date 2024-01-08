package com.babylon.wallet.android.presentation.settings.personadetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.settings.personas.personadetail.ARG_PERSONA_ADDRESS
import com.babylon.wallet.android.presentation.settings.personas.personadetail.PersonaDetailViewModel
import com.babylon.wallet.android.utils.AppEventBus
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
import rdx.works.core.identifiedArrayListOf
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
        every { savedStateHandle.get<String>(ARG_PERSONA_ADDRESS) } returns "1"
        every { getProfileUseCase() } returns flowOf(
            profile(
                personas = identifiedArrayListOf(
                    SampleDataProvider().samplePersona("1")
                )
            )
        )
        coEvery { getDAppsUseCase("address1", false) } returns Result.success(DApp("address1"))
        coEvery { getDAppsUseCase("address2", false) } returns Result.success(DApp("address2"))
    }

    @Test
    fun `init load persona and dapps`() = runTest {
        every { eventBus.events } returns MutableSharedFlow()
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

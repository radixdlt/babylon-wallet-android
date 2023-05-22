package com.babylon.wallet.android.presentation.dapp.selectpersona

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import rdx.works.core.preferences.PreferencesManager
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.SelectPersonaViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ARG_REQUEST_ID
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
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class SelectPersonaViewModelTest : StateViewModelTest<SelectPersonaViewModel>() {

    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val dAppConnectionRepository = DAppConnectionRepositoryFake()

    private val requestWithNonExistingDappAddress = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        dappId = "1",
        interactionId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address"
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge,
        oneTimeAccountsRequestItem = null,
        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            true, false, 1,
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
        )
    )

    override fun initVM(): SelectPersonaViewModel {
        return SelectPersonaViewModel(
            savedStateHandle,
            dAppConnectionRepository,
            getProfileUseCase,
            preferencesManager,
            incomingRequestRepository
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        coEvery { preferencesManager.firstPersonaCreated } returns flow {
            emit(true)
        }
        every { savedStateHandle.get<String>(ARG_REQUEST_ID) } returns "1"
        every { getProfileUseCase() } returns flowOf(
            profile(personas = listOf(
                SampleDataProvider().samplePersona("address1"),
                SampleDataProvider().samplePersona("address2")
            ))
        )
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns requestWithNonExistingDappAddress
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

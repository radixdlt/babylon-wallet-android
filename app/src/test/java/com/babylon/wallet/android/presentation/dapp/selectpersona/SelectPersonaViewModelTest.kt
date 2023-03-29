package com.babylon.wallet.android.presentation.dapp.selectpersona

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.presentation.BaseViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.SelectPersonaViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ARG_REQUEST_ID
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
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.data.repository.ProfileDataSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class SelectPersonaViewModelTest : BaseViewModelTest<SelectPersonaViewModel>() {

    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val dAppConnectionRepository = DAppConnectionRepositoryFake()

    private val requestWithNonExistingDappAddress = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        dappId = "1",
        requestId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address"
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest(),
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
            personaRepository,
            preferencesManager,
            incomingRequestRepository
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        val addressSlot = slot<String>()
        coEvery { preferencesManager.firstPersonaCreated } returns flow {
            emit(true)
        }
        every { savedStateHandle.get<String>(ARG_REQUEST_ID) } returns "1"
        coEvery { personaRepository.getPersonaByAddress(capture(addressSlot)) } answers {
            SampleDataProvider().samplePersona(addressSlot.captured)
        }
        coEvery { personaRepository.personas } returns flow {
            emit(
                listOf(
                    SampleDataProvider().samplePersona("address1"),
                    SampleDataProvider().samplePersona("address2")
                )
            )
        }
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

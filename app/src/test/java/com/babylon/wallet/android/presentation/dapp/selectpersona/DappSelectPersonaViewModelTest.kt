package com.babylon.wallet.android.presentation.dapp.selectpersona

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
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
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.data.repository.ProfileDataSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class DappSelectPersonaViewModelTest : BaseViewModelTest<DappSelectPersonaViewModel>() {

    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val profileDataSource = mockk<ProfileDataSource>()
    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val dAppConnectionRepository = DAppConnectionRepositoryFake()

    private val samplePersona = SampleDataProvider().samplePersona()

    private val requestWithNonExistingDappAddress = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        "1",
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

    override fun initVM(): DappSelectPersonaViewModel {
        return DappSelectPersonaViewModel(
            savedStateHandle,
            dAppConnectionRepository,
            personaRepository,
            incomingRequestRepository
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        val addressSlot = slot<String>()
        every { savedStateHandle.get<String>(com.babylon.wallet.android.presentation.dapp.login.ARG_REQUEST_ID) } returns "1"
        coEvery { profileDataSource.getCurrentNetwork() } returns Network.nebunet
        coEvery { personaRepository.getPersonaByAddress(capture(addressSlot)) } answers {
            SampleDataProvider().samplePersona(addressSlot.captured)
        }
        coEvery { personaRepository.personas } returns flow {
            emit(listOf(samplePersona))
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
        }
    }

    @Test
    fun `connected dapp does not exist`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(!item.continueButtonEnabled)
            assert(item.personaListToDisplay.size == 1)
        }
    }

}
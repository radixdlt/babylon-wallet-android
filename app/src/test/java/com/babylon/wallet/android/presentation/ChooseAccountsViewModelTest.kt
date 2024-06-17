package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.ARG_EXACT_ACCOUNT_COUNT
import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.ARG_NUMBER_OF_ACCOUNTS
import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.OneTimeChooseAccountsViewModel
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChooseAccountsViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val profileRepository = FakeProfileRepository(
        profile = Profile.sample()
            .changeGateway(Gateway.forNetwork(NetworkId.MAINNET))
            .unHideAllEntities() // Default profile sample has hidden entities.
    )
    private val getProfileUseCase = GetProfileUseCase(profileRepository)

    private val incomingRequestRepository = IncomingRequestRepositoryImpl()

    private lateinit var viewModel: OneTimeChooseAccountsViewModel

    private val accountsRequestExact = IncomingMessage.IncomingRequest.UnauthorizedRequest(
        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata(NetworkId.MAINNET, "", "", false),
        oneTimeAccountsRequestItem = IncomingMessage.IncomingRequest.AccountsRequestItem(
            isOngoing = false,
            numberOfValues = IncomingMessage.IncomingRequest.NumberOfValues(
                1,
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly
            ),
            challenge = null
        )
    )
    private val accountsTwoRequestExact = IncomingMessage.IncomingRequest.UnauthorizedRequest(
        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata(NetworkId.MAINNET, "", "", false),
        oneTimeAccountsRequestItem = IncomingMessage.IncomingRequest.AccountsRequestItem(
            isOngoing = false,
            numberOfValues = IncomingMessage.IncomingRequest.NumberOfValues(
                2,
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly
            ),
            challenge = null
        )
    )

    private val accountsRequestAtLeast = IncomingMessage.IncomingRequest.UnauthorizedRequest(
        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata(NetworkId.MAINNET, "", "", false),
        oneTimeAccountsRequestItem = IncomingMessage.IncomingRequest.AccountsRequestItem(
            isOngoing = false,
            numberOfValues = IncomingMessage.IncomingRequest.NumberOfValues(
                2,
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            ),
            challenge = null
        )
    )

    @Before
    fun setup() = runTest {
        incomingRequestRepository.add(accountsRequestAtLeast)

        viewModel = OneTimeChooseAccountsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    ARG_NUMBER_OF_ACCOUNTS to accountsRequestAtLeast.oneTimeAccountsRequestItem!!.numberOfValues.quantity,
                    ARG_EXACT_ACCOUNT_COUNT to true
                )
            ),
            getProfileUseCase = getProfileUseCase
        )
    }

    @Test
    fun `given a profile with 2 accounts, when Choose Accounts screen is launched, then it shows two profiles to select`() =
        runTest {
            advanceUntilIdle()
            val state = viewModel.state.first()
            assertTrue(state.availableAccountItems.size == 2)
        }

    @Test
    fun `given a request for at least 2 accounts, when Choose Accounts screen is launched, then continue button is disabled`() =
        runTest {
            advanceUntilIdle()
            val state = viewModel.state.first()
            assertFalse(state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for at least 2 accounts, when user selects one, then continue button is disabled`() =
        runTest {
            advanceUntilIdle()
            viewModel.onAccountSelect(0)
            val state = viewModel.state.first()
            assertFalse(state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for at least 2 accounts, when user selects two, then continue button is enabled`() =
        runTest {
            advanceUntilIdle()
            viewModel.onAccountSelect(0)
            viewModel.onAccountSelect(1)
            val state = viewModel.state.first()
            assertTrue(state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for at least 2 accounts, when user selects two and unselect the last selected, then continue button is disabled`() =
        runTest {
            advanceUntilIdle()
            viewModel.onAccountSelect(0)
            viewModel.onAccountSelect(1)
            viewModel.onAccountSelect(1)
            val state = viewModel.state.first()
            assertFalse(state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for exactly 1 account, when selected one, then continue button is enabled`() =
        runTest {
            // given
            incomingRequestRepository.add(accountsRequestExact)

            viewModel = OneTimeChooseAccountsViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        ARG_NUMBER_OF_ACCOUNTS to accountsRequestExact.oneTimeAccountsRequestItem!!.numberOfValues.quantity,
                        ARG_EXACT_ACCOUNT_COUNT to true
                    )
                ),
                getProfileUseCase = getProfileUseCase
            )

            advanceUntilIdle()

            // when
            viewModel.onAccountSelect(0)

            // then
            val state = viewModel.state.first()
            assertTrue(state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for exactly 2 account, when selecting one, then continue button is disabled`() =
        runTest {
            // given
            incomingRequestRepository.add(accountsTwoRequestExact)

            viewModel = OneTimeChooseAccountsViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        ARG_NUMBER_OF_ACCOUNTS to accountsTwoRequestExact.oneTimeAccountsRequestItem!!.numberOfValues.quantity,
                        ARG_EXACT_ACCOUNT_COUNT to true
                    )
                ),
                getProfileUseCase = getProfileUseCase
            )

            advanceUntilIdle()

            // when
            viewModel.onAccountSelect(0)

            // then
            val state = viewModel.state.first()
            assertFalse(state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for exactly 2 account, when selecting two, then continue button is enabled`() =
        runTest {
            // given
            incomingRequestRepository.add(accountsTwoRequestExact)
            viewModel = OneTimeChooseAccountsViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        ARG_NUMBER_OF_ACCOUNTS to accountsTwoRequestExact.oneTimeAccountsRequestItem!!.numberOfValues.quantity,
                        ARG_EXACT_ACCOUNT_COUNT to true
                    )
                ),
                getProfileUseCase = getProfileUseCase
            )
            advanceUntilIdle()

            // when
            viewModel.onAccountSelect(0)
            viewModel.onAccountSelect(1)

            // then
            val state = viewModel.state.first()
            assertTrue(state.isContinueButtonEnabled)
        }

}

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.dapp.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.domain.model.resources.metadata.PublicKeyHash
import com.babylon.wallet.android.domain.usecases.BuildAuthorizedDappResponseUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.fakes.DappMessengerFake
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.ARG_INTERACTION_ID
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.utils.AppEventBus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import rdx.works.core.identifiedArrayListOf
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.math.BigDecimal

class DAppAuthorizedLoginViewModelTest : StateViewModelTest<DAppAuthorizedLoginViewModel>() {

    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val appEventBus = mockk<AppEventBus>()
    private val stateRepository = StateRepositoryFake()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val buildAuthorizedDappResponseUseCase = mockk<BuildAuthorizedDappResponseUseCase>()
    private val dAppMessenger = DappMessengerFake()
    private val dAppConnectionRepository = spyk<DAppConnectionRepositoryFake> { DAppConnectionRepositoryFake() }

    private val samplePersona = SampleDataProvider().samplePersona(personaAddress = "address1")

    private val requestWithNonExistingDappAddress = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        remoteConnectorId = "remoteConnectorId",
        interactionId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address",
            false
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge,
        oneTimeAccountsRequestItem = null,
        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            true,
            MessageFromDataChannel.IncomingRequest.NumberOfValues(
                1,
                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            ),
            null
        )
    )

    private val usePersonaRequestOngoing = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        remoteConnectorId = "remoteConnectorId",
        interactionId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address",
            false
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            true, MessageFromDataChannel.IncomingRequest.NumberOfValues(
                1,
                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            ),
            null
        )
    )

    private val usePersonaRequestOngoingPlusOngoingData = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        remoteConnectorId = "1",
        interactionId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address",
            false
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            true, MessageFromDataChannel.IncomingRequest.NumberOfValues(
                1,
                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            ),
            null
        ),
        ongoingPersonaDataRequestItem = MessageFromDataChannel.IncomingRequest.PersonaRequestItem(
            isRequestingName = true,
            isOngoing = true
        )
    )

    private val usePersonaRequestOngoingDataOnly = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        remoteConnectorId = "1",
        interactionId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address",
            false
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        ongoingPersonaDataRequestItem = MessageFromDataChannel.IncomingRequest.PersonaRequestItem(
            isRequestingName = true,
            isOngoing = true
        )
    )

    private val usePersonaRequestOneTimeAccounts = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        remoteConnectorId = "1",
        interactionId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address",
            false
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            false, MessageFromDataChannel.IncomingRequest.NumberOfValues(
                1,
                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            ),
            null
        )
    )

    private val usePersonaRequestOneTimeAccountsAndData = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        remoteConnectorId = "1",
        interactionId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address",
            false
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            false, MessageFromDataChannel.IncomingRequest.NumberOfValues(
                1,
                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            ),
            null
        ),
        oneTimePersonaDataRequestItem = MessageFromDataChannel.IncomingRequest.PersonaRequestItem(
            isRequestingName = true,
            isOngoing = false
        )
    )

    override fun initVM(): DAppAuthorizedLoginViewModel {
        return DAppAuthorizedLoginViewModel(
            savedStateHandle,
            appEventBus,
            dAppMessenger,
            dAppConnectionRepository,
            getProfileUseCase,
            getCurrentGatewayUseCase,
            stateRepository,
            incomingRequestRepository,
            buildAuthorizedDappResponseUseCase
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_INTERACTION_ID) } returns "1"
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.nebunet
        every { buildAuthorizedDappResponseUseCase.signingState } returns emptyFlow()
        coEvery { buildAuthorizedDappResponseUseCase.invoke(any(), any(), any(), any(), any(), any(),) } returns Result.success(any())
        every { getProfileUseCase() } returns flowOf(
            profile(
                personas = identifiedArrayListOf(samplePersona),
                dApps = listOf(
                    Network.AuthorizedDapp(
                        networkID = Radix.Gateway.nebunet.network.id,
                        dAppDefinitionAddress = "dapp_address",
                        displayName = "1",
                        referencesToAuthorizedPersonas = emptyList()
                    )
                )
            )
        )
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns requestWithNonExistingDappAddress
    }

    @Test
    fun `init sets correct state for login request`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.SelectPersona)
        }
    }

    @Test
    fun `handle ongoing request flow first time`() = runTest {
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.SavedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.onSelectPersona(samplePersona)
        advanceUntilIdle()
        vm.personaSelectionConfirmed()
        advanceUntilIdle()
        vm.oneOffEvent.test {
            assert(expectMostRecentItem() is Event.DisplayPermission)
        }
        vm.onAccountsSelected(listOf(AccountItemUiModel("random address", "account 1", 0)), false)
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val mostRecentItem = expectMostRecentItem()
            assert(mostRecentItem is Event.RequestCompletionBiometricPrompt)
        }
    }

    @Test
    fun `init sets correct state for use persona ongoing request`() = runTest {
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOngoing
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.Permission)
        }
    }

    @Test
    fun `init sets correct state for use persona accounts and data when accounts are already granted`() = runTest {
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOngoingPlusOngoingData
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        coEvery { dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(any(), any(), any(), any()) } returns listOf(
            SampleDataProvider().randomAddress()
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingPersonaData)
        }
    }

    @Test
    fun `init sets correct state for use persona onetime request`() = runTest {
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOneTimeAccounts
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.ChooseAccount)
        }
    }

    private class StateRepositoryFake: StateRepository {
        override fun observeAccountsOnLedger(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> {
            error("Not needed")
        }

        override suspend fun getNextNFTsPage(
            account: Network.Account,
            resource: Resource.NonFungibleResource
        ): Result<Resource.NonFungibleResource> {
            error("Not needed")
        }

        override suspend fun updateLSUsInfo(
            account: Network.Account,
            validatorsWithStakes: List<ValidatorWithStakes>
        ): Result<List<ValidatorWithStakes>> {
            error("Not needed")
        }

        override suspend fun getResources(
            addresses: Set<String>,
            underAccountAddress: String?,
            withDetails: Boolean
        ): Result<List<Resource>> {
            return Result.success(emptyList())
        }

        override suspend fun getPool(poolAddress: String): Result<Pool> {
            error("Not needed")
        }

        override suspend fun getValidator(validatorAddress: String): Result<ValidatorDetail> {
            error("Not needed")
        }

        override suspend fun getValidators(validatorAddresses: Set<String>): Result<List<ValidatorDetail>> {
            error("Not needed")
        }

        override suspend fun getNFTDetails(
            resourceAddress: String,
            localIds: Set<String>
        ): Result<List<Resource.NonFungibleResource.Item>> {
            error("Not needed")
        }

        override suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>> {
            error("Not needed")
        }

        override suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, List<PublicKeyHash>>> {
            error("Not needed")
        }

        override suspend fun getDAppsDetails(definitionAddresses: List<String>, skipCache: Boolean): Result<List<DApp>> {
            return Result.success(
                listOf(
                    DApp(
                        dAppAddress = "dapp_address",
                        metadata = listOf(
                            Metadata.Primitive(ExplicitMetadataKey.NAME.key, "dApp", MetadataType.String)
                        )
                    )
                )
            )
        }

        override suspend fun cacheNewlyCreatedResources(newResources: List<Resource>): Result<Unit> {
            error("Not needed")
        }

        override suspend fun clearCachedState(): Result<Unit> {
            error("Not needed")
        }

    }

}

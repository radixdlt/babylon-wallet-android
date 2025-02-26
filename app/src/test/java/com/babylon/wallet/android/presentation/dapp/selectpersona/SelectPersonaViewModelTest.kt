package com.babylon.wallet.android.presentation.dapp.selectpersona

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.domain.usecases.signing.SignAuthUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.ARG_AUTHORIZED_REQUEST_INTERACTION_ID
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.ARG_DAPP_DEFINITION_ADDRESS
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.SelectPersonaViewModel
import com.babylon.wallet.android.utils.AppEventBusImpl
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedDappPreferenceDeposits
import com.radixdlt.sargon.AuthorizedDappPreferences
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
import org.junit.Ignore
import org.junit.Test
import rdx.works.core.domain.DApp
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.authorizedDApps
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class SelectPersonaViewModelTest : StateViewModelTest<SelectPersonaViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val incomingRequestRepository = IncomingRequestRepositoryImpl(AppEventBusImpl())
    private val signAuthUseCase = mockk<SignAuthUseCase>()

    private val dApp = DApp.sampleMainnet()
    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities().let {
        val mainnet = it.networks.asIdentifiable().getBy(NetworkId.MAINNET)!!
        val samplePersonas = mainnet.personas
        it.copy(
            networks = it.networks.asIdentifiable().updateOrAppend(
                mainnet.copy(
                    authorizedDapps = AuthorizedDapps(
                        AuthorizedDapp(
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
                                        ids = listOf(mainnet.accounts.first().address)
                                    )
                                )
                            ).asList(),
                            preferences = AuthorizedDappPreferences(
                                deposits = AuthorizedDappPreferenceDeposits.VISIBLE
                            )
                        )
                    ).asList()
                )
            ).asList()
        )
    }
    private val authorizedDapp = profile.currentNetwork!!.authorizedDApps().first()
    private val dAppConnectionRepository = DAppConnectionRepositoryFake().apply {
        this.savedDApp = authorizedDapp
        state = DAppConnectionRepositoryFake.InitialState.SavedDapp
    }

    override fun initVM(): SelectPersonaViewModel {
        return SelectPersonaViewModel(
            savedStateHandle,
            dAppConnectionRepository,
            getProfileUseCase,
            preferencesManager,
            incomingRequestRepository,
            signAuthUseCase
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        coEvery { preferencesManager.firstPersonaCreated } returns flowOf(true)
        every { savedStateHandle.get<String>(ARG_AUTHORIZED_REQUEST_INTERACTION_ID) } returns dApp.dAppAddress.string
        every { savedStateHandle.get<String>(ARG_DAPP_DEFINITION_ADDRESS) } returns dApp.dAppAddress.string
        coEvery { getProfileUseCase() } returns profile
        every { getProfileUseCase.flow } returns flowOf(profile)
    }

    @Test
    fun `connected dapp exist and has authorized persona`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.isContinueButtonEnabled)
            assert(item.personas.size == 2)
            val onePersonaAuthorized = item.personas.count { it.lastUsedOn != null } == 1
            assert(onePersonaAuthorized)
        }
    }

    @Ignore("TODO Integration")
    @Test
    fun `connected dapp does not exist`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(!item.isContinueButtonEnabled)
            assert(item.personas.size == 2)
            val noPersonaAuthorized = item.personas.all { it.lastUsedOn == null }
            assert(noPersonaAuthorized)
        }
    }

}

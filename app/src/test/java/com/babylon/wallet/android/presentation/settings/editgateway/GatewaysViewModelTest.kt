package com.babylon.wallet.android.presentation.settings.editgateway

import app.cash.turbine.test
import com.babylon.wallet.android.domain.model.NetworkInfo
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.settings.preferences.gateways.GatewayAddFailure
import com.babylon.wallet.android.presentation.settings.preferences.gateways.GatewaysViewModel
import com.babylon.wallet.android.presentation.settings.preferences.gateways.SettingsEditGatewayEvent
import com.babylon.wallet.android.utils.isValidUrl
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.default
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.AddGatewayUseCase
import rdx.works.profile.domain.gateway.ChangeGatewayIfNetworkExistUseCase
import rdx.works.profile.domain.gateway.DeleteGatewayUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class GatewaysViewModelTest {
    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private lateinit var vm: GatewaysViewModel

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val changeGatewayIfNetworkExistUseCase = mockk<ChangeGatewayIfNetworkExistUseCase>()
    private val addGatewayUseCase = mockk<AddGatewayUseCase>()
    private val deleteGatewayUseCase = mockk<DeleteGatewayUseCase>()
    private val getNetworkInfoUseCase = mockk<GetNetworkInfoUseCase>()

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET))

    @Before
    fun setUp() = runTest {
        vm = GatewaysViewModel(
            getProfileUseCase = getProfileUseCase,
            changeGatewayIfNetworkExistUseCase = changeGatewayIfNetworkExistUseCase,
            addGatewayUseCase = addGatewayUseCase,
            deleteGatewayUseCase = deleteGatewayUseCase,
            getNetworkInfoUseCase = getNetworkInfoUseCase
        )
        every { getProfileUseCase.flow } returns flowOf(profile)
        coEvery { changeGatewayIfNetworkExistUseCase(any()) } returns true
        coEvery { addGatewayUseCase(any()) } returns Unit
        coEvery { getNetworkInfoUseCase(any()) } returns Result.success(
            NetworkInfo(NetworkId.MAINNET, 0L)
        )
        mockkStatic("com.babylon.wallet.android.utils.StringExtensionsKt")
        every { any<String>().isValidUrl() } returns true
    }

    @Test
    fun `init loads profile and url`() = runTest {
        assert(vm.state.value.currentGateway != null)
    }

    @Test
    fun `url change updates it's value and valid state`() = runTest {
        val sampleUrl = "https://test.com"
        vm.onNewUrlChanged(sampleUrl)
        assert(vm.state.value.newUrl == sampleUrl)
        assert(vm.state.value.newUrlValid)
    }

    @Test
    fun `adding network triggers network save`() = runTest {
        val sampleUrl = Gateway.forNetwork(NetworkId.MAINNET).url.toString()
        vm.onNewUrlChanged(sampleUrl)
        vm.onAddGateway()
        advanceUntilIdle()
        coVerify(exactly = 1) { addGatewayUseCase(any()) }
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is SettingsEditGatewayEvent.GatewayAdded)
        }
    }

    @Test
    fun `network switch calls for create account`() = runTest {
        val gateway = Gateway.forNetwork(NetworkId.KISHARNET)
        val sampleUrl = gateway.url.toString()
        vm.onNewUrlChanged(sampleUrl)
        coEvery { changeGatewayIfNetworkExistUseCase(gateway) } returns false
        vm.onGatewayClick(gateway)
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is SettingsEditGatewayEvent.CreateProfileOnNetwork)
        }
    }

    @Test
    fun `network switch calls changes gateway when there are accounts present`() = runTest {
        val gateway = Gateway.forNetwork(NetworkId.KISHARNET)
        val sampleUrl = gateway.url.toString()
        vm.onNewUrlChanged(sampleUrl)
        coEvery { changeGatewayIfNetworkExistUseCase(gateway) } returns true
        vm.onGatewayClick(gateway)
        advanceUntilIdle()
        coVerify(exactly = 1) { changeGatewayIfNetworkExistUseCase(gateway) }
    }

    @Test
    fun `trying to switch to current network is no op`() = runTest {
        val gateway = Gateway.default
        val sampleUrl = gateway.url.toString()
        vm.onNewUrlChanged(sampleUrl)
        vm.onGatewayClick(gateway)
        advanceUntilIdle()
        coVerify(exactly = 0) { changeGatewayIfNetworkExistUseCase(gateway) }
        assert(vm.state.value.gatewayAddFailure == GatewayAddFailure.AlreadyExist)
    }

    @Test
    fun `network info error triggers ui error`() = runTest {
        coEvery { getNetworkInfoUseCase(any()) } returns Result.failure(
            Throwable()
        )
        val sampleUrl = Gateway.forNetwork(NetworkId.NEBUNET).url.toString()
        vm.onNewUrlChanged(sampleUrl)
        vm.onAddGateway()
        advanceUntilIdle()
        assert(vm.state.value.gatewayAddFailure == GatewayAddFailure.ErrorWhileAdding)
    }
}

package com.babylon.wallet.android.presentation.settings.editgateway

import app.cash.turbine.test
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.NetworkInfo
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.settings.appsettings.gateways.GatewayAddFailure
import com.babylon.wallet.android.presentation.settings.appsettings.gateways.SettingsEditGatewayEvent
import com.babylon.wallet.android.presentation.settings.appsettings.gateways.GatewaysViewModel
import com.babylon.wallet.android.utils.isValidUrl
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
import rdx.works.profile.data.model.apppreferences.Radix
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

    private val profile = SampleDataProvider().sampleProfile()

    @Before
    fun setUp() = runTest {
        vm = GatewaysViewModel(
            getProfileUseCase = getProfileUseCase,
            changeGatewayIfNetworkExistUseCase = changeGatewayIfNetworkExistUseCase,
            addGatewayUseCase = addGatewayUseCase,
            deleteGatewayUseCase = deleteGatewayUseCase,
            getNetworkInfoUseCase = getNetworkInfoUseCase
        )
        every { getProfileUseCase() } returns flowOf(profile)
        coEvery { changeGatewayIfNetworkExistUseCase(any()) } returns true
        coEvery { addGatewayUseCase(any()) } returns Unit
        coEvery { getNetworkInfoUseCase(any()) } returns Result.success(
            NetworkInfo(Radix.Network.nebunet, 0L)
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
        val sampleUrl = Radix.Gateway.nebunet.url
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
        val sampleUrl = Radix.Gateway.kisharnet.url
        val gateway = Radix.Gateway(sampleUrl, Radix.Network.kisharnet)
        vm.onNewUrlChanged(sampleUrl)
        coEvery { changeGatewayIfNetworkExistUseCase(gateway) } returns false
        vm.onGatewayClick(Radix.Gateway(sampleUrl, Radix.Network.kisharnet))
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is SettingsEditGatewayEvent.CreateProfileOnNetwork)
        }
    }

    @Test
    fun `network switch calls changes gateway when there are accounts present`() = runTest {
        val sampleUrl = Radix.Gateway.kisharnet.url
        val gateway = Radix.Gateway(sampleUrl, Radix.Network.kisharnet)
        vm.onNewUrlChanged(sampleUrl)
        coEvery { changeGatewayIfNetworkExistUseCase(gateway) } returns true
        vm.onGatewayClick(Radix.Gateway(sampleUrl, Radix.Network.kisharnet))
        advanceUntilIdle()
        coVerify(exactly = 1) { changeGatewayIfNetworkExistUseCase(gateway) }
    }

    @Test
    fun `trying to switch to current network is no op`() = runTest {
        val sampleUrl = Radix.Gateway.default.url
        val gateway = Radix.Gateway.default
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
        val sampleUrl = Radix.Gateway.nebunet.url
        vm.onNewUrlChanged(sampleUrl)
        vm.onAddGateway()
        advanceUntilIdle()
        assert(vm.state.value.gatewayAddFailure == GatewayAddFailure.ErrorWhileAdding)
    }
}

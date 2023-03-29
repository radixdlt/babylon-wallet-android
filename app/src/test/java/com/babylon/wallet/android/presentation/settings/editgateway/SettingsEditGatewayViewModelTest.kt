package com.babylon.wallet.android.presentation.settings.editgateway

import app.cash.turbine.test
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.utils.isValidUrl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.domain.gateway.AddGatewayUseCase
import rdx.works.profile.domain.gateway.ChangeGatewayUseCase
import rdx.works.profile.domain.gateway.DeleteGatewayUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsEditGatewayViewModelTest {
    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private lateinit var vm: SettingsEditGatewayViewModel

    private val profileDataSource = mockk<ProfileDataSource>()
    private val changeGatewayUseCase = mockk<ChangeGatewayUseCase>()
    private val addGatewayUseCase = mockk<AddGatewayUseCase>()
    private val deleteGatewayUseCase = mockk<DeleteGatewayUseCase>()
    private val networkInfoRepository = mockk<NetworkInfoRepository>()

    private val profile = SampleDataProvider().sampleProfile()

    @Before
    fun setUp() = runTest {
        vm = SettingsEditGatewayViewModel(
            profileDataSource = profileDataSource,
            changeGatewayUseCase = changeGatewayUseCase,
            addGatewayUseCase = addGatewayUseCase,
            deleteGatewayUseCase = deleteGatewayUseCase,
            networkInfoRepository = networkInfoRepository
        )
        every { profileDataSource.gateways } returns flow { emit(profile.appPreferences.gateways) }
        coEvery { changeGatewayUseCase(any()) } returns true
        coEvery { addGatewayUseCase(any()) } returns Unit
        coEvery { networkInfoRepository.getNetworkInfo(any()) } returns Result.Success("nebunet")
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
        val sampleUrl = Radix.Gateway.hammunet.url
        val gateway = Radix.Gateway(sampleUrl, Radix.Network.hammunet)
        vm.onNewUrlChanged(sampleUrl)
        coEvery { changeGatewayUseCase(gateway) } returns false
        vm.onGatewayClick(Radix.Gateway(sampleUrl, Radix.Network.hammunet))
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is SettingsEditGatewayEvent.CreateProfileOnNetwork)
        }
    }

    @Test
    fun `network switch calls changes gateway when there are accounts present`() = runTest {
        val sampleUrl = Radix.Gateway.hammunet.url
        val gateway = Radix.Gateway(sampleUrl, Radix.Network.hammunet)
        vm.onNewUrlChanged(sampleUrl)
        coEvery { changeGatewayUseCase(gateway) } returns true
        vm.onGatewayClick(Radix.Gateway(sampleUrl, Radix.Network.hammunet))
        advanceUntilIdle()
        coVerify(exactly = 1) { changeGatewayUseCase(gateway) }
    }

    @Test
    fun `trying to switch to current network is no op`() = runTest {
        val sampleUrl = Radix.Gateway.default.url
        val gateway = Radix.Gateway.default
        vm.onNewUrlChanged(sampleUrl)
        vm.onGatewayClick(gateway)
        advanceUntilIdle()
        coVerify(exactly = 0) { changeGatewayUseCase(gateway) }
        assert(vm.state.value.gatewayAddFailure == GatewayAddFailure.AlreadyExist)
    }

    @Test
    fun `network info error triggers ui error`() = runTest {
        coEvery { networkInfoRepository.getNetworkInfo(any()) } returns Result.Error()
        val sampleUrl = Radix.Gateway.nebunet.url
        vm.onNewUrlChanged(sampleUrl)
        vm.onAddGateway()
        advanceUntilIdle()
        assert(vm.state.value.gatewayAddFailure == GatewayAddFailure.ErrorWhileAdding)
    }
}

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
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.repository.ProfileDataSource

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsEditGatewayViewModelTest {
    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private lateinit var vm: SettingsEditGatewayViewModel

    private val profileDataSource = mockk<ProfileDataSource>()
    private val networkInfoRepository = mockk<NetworkInfoRepository>()

    private val profile = SampleDataProvider().sampleProfile()

    @Before
    fun setUp() = runTest {
        vm = SettingsEditGatewayViewModel(profileDataSource, networkInfoRepository)
        every { profileDataSource.gateways } returns flow { emit(profile.appPreferences.gateways) }
        coEvery { profileDataSource.changeGateway(any()) } just Runs
        coEvery { profileDataSource.addGateway(any()) } just Runs
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
        val sampleUrl = Gateway.nebunet.url
        vm.onNewUrlChanged(sampleUrl)
        vm.onAddGateway()
        advanceUntilIdle()
        coVerify(exactly = 1) { profileDataSource.addGateway(any()) }
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is SettingsEditGatewayEvent.GatewayAdded)
        }
    }

    @Test
    fun `network switch calls for create account`() = runTest {
        val sampleUrl = Gateway.nebunet.url
        val gateway = Gateway(sampleUrl, Network.nebunet)
        vm.onNewUrlChanged(sampleUrl)
        coEvery { profileDataSource.hasAccountForGateway(gateway) } returns false
        vm.onGatewayClick(Gateway(sampleUrl, Network.nebunet))
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is SettingsEditGatewayEvent.CreateProfileOnNetwork)
        }
    }

    @Test
    fun `network switch calls changes gateway when ther eare accounts present`() = runTest {
        val sampleUrl = Gateway.nebunet.url
        val gateway = Gateway(sampleUrl, Network.nebunet)
        vm.onNewUrlChanged(sampleUrl)
        coEvery { profileDataSource.hasAccountForGateway(gateway) } returns true
        vm.onGatewayClick(Gateway(sampleUrl, Network.nebunet))
        advanceUntilIdle()
        coVerify(exactly = 1) { profileDataSource.changeGateway(gateway) }
    }

    @Test
    fun `trying to switch to current network is no op`() = runTest {
        val sampleUrl = Gateway.hammunet.url
        val gateway = Gateway(sampleUrl, Network.nebunet)
        vm.onNewUrlChanged(sampleUrl)
        vm.onGatewayClick(Gateway(sampleUrl, Network.nebunet))
        advanceUntilIdle()
        coVerify(exactly = 0) { profileDataSource.changeGateway(gateway) }
        assert(vm.state.value.gatewayAddFailure == GatewayAddFailure.AlreadyExist)
    }

    @Test
    fun `network info error triggers ui error`() = runTest {
        coEvery { networkInfoRepository.getNetworkInfo(any()) } returns Result.Error()
        val sampleUrl = Gateway.nebunet.url
        vm.onNewUrlChanged(sampleUrl)
        vm.onAddGateway()
        advanceUntilIdle()
        assert(vm.state.value.gatewayAddFailure == GatewayAddFailure.ErrorWhileAdding)
    }
}

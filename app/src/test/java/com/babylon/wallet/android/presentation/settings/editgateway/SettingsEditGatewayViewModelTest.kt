package com.babylon.wallet.android.presentation.settings.editgateway

import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.common.InfoMessageType
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.isValidUrl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
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
        every { profileDataSource.networkAndGateway } returns flow { emit(profile.appPreferences.networkAndGateway) }
        coEvery { profileDataSource.setNetworkAndGateway(any(), any()) } just Runs
        coEvery { networkInfoRepository.getNetworkInfo(any()) } returns Result.Success("mardunet")
        mockkStatic("com.babylon.wallet.android.utils.StringExtensionsKt")
        every { any<String>().isValidUrl() } returns true
    }

    @Test
    fun `init loads profile and url`() = runTest {
        assert(vm.state.newUrl.isNotEmpty())
        assert(vm.state.currentNetworkAndGateway != null)
    }

    @Test
    fun `url change updates it's value and valid state`() = runTest {
        val sampleUrl = "https://test.com"
        vm.onNewUrlChanged(sampleUrl)
        assert(vm.state.newUrl == sampleUrl)
        assert(vm.state.newUrlValid)
    }

    @Test
    fun `on message shown clears UI message`() = runTest {
        vm.onMessageShown()
        assert(vm.state.uiMessage == null)
    }

    @Test
    fun `on network switch changes network`() = runTest {
        val sampleUrl = NetworkAndGateway.betanet.gatewayAPIEndpointURL
        vm.onNewUrlChanged(sampleUrl)
        coEvery { profileDataSource.hasAccountOnNetwork(sampleUrl, any()) } returns true
        vm.onSwitchToClick()
        advanceUntilIdle()
        coVerify(exactly = 1) { profileDataSource.setNetworkAndGateway(any(), any()) }
    }

    @Test
    fun `on network switch calls for create account`() = runTest {
        val sampleUrl = NetworkAndGateway.betanet.gatewayAPIEndpointURL
        vm.onNewUrlChanged(sampleUrl)
        coEvery { profileDataSource.hasAccountOnNetwork(sampleUrl, any()) } returns false
        vm.onSwitchToClick()
        advanceUntilIdle()
        assert(vm.oneOffEvent.first() is SettingsEditGatewayEvent.CreateProfileOnNetwork)
    }

    @Test
    fun `network info error triggers ui error`() = runTest {
        coEvery { networkInfoRepository.getNetworkInfo(any()) } returns Result.Error()
        val sampleUrl = NetworkAndGateway.betanet.gatewayAPIEndpointURL
        vm.onNewUrlChanged(sampleUrl)
        vm.onSwitchToClick()
        advanceUntilIdle()
        val uiMessage = vm.state.uiMessage
        assert(uiMessage is UiMessage.InfoMessage && uiMessage.type == InfoMessageType.GatewayInvalid)
    }
}

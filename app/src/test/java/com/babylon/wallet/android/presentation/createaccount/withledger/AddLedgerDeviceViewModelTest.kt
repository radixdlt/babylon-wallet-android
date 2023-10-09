package com.babylon.wallet.android.presentation.createaccount.withledger

import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.settings.accountsecurity.ledgerhardwarewallets.AddLedgerDeviceUiState
import com.babylon.wallet.android.presentation.settings.accountsecurity.ledgerhardwarewallets.AddLedgerDeviceViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.core.HexCoded32Bytes
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceResult
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class AddLedgerDeviceViewModelTest : StateViewModelTest<AddLedgerDeviceViewModel>() {

    private val firstDeviceId = "5f47ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010996f5"

    private val getProfileUseCaseMock = mockk<GetProfileUseCase>()
    private val ledgerMessengerMock = mockk<LedgerMessenger>()
    private val addLedgerFactorSourceUseCaseMock = mockk<AddLedgerFactorSourceUseCase>()

    override fun initVM(): AddLedgerDeviceViewModel {
        return AddLedgerDeviceViewModel(
            getProfileUseCase = getProfileUseCaseMock,
            ledgerMessenger = ledgerMessengerMock,
            addLedgerFactorSourceUseCase = addLedgerFactorSourceUseCaseMock
        )
    }

    @Before
    fun setup() {
        coEvery { ledgerMessengerMock.isConnected } returns flowOf(true)
    }

    @Test
    fun `sending ledger device info requests call proper method and sets proper state`() = runTest {
        coEvery { getProfileUseCaseMock() } returns flowOf(profile())
        coEvery { ledgerMessengerMock.sendDeviceInfoRequest(any()) } returns Result.success(
            MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse(
                interactionId = "1",
                model = MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoS,
                deviceId = HexCoded32Bytes(firstDeviceId)
            )
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.onSendAddLedgerRequestClick()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.showContent == AddLedgerDeviceUiState.ShowContent.NameLedgerDevice)
            assert(item.newConnectedLedgerDevice != null)
        }
        coVerify(exactly = 1) { ledgerMessengerMock.sendDeviceInfoRequest(any()) }
    }

    @Test
    fun `adding ledger and providing name`() = runTest {
        val ledgerDeviceToAdd = (profile().factorSources[1] as LedgerHardwareWalletFactorSource)
        coEvery {
            addLedgerFactorSourceUseCaseMock(
                ledgerId = (profile().factorSources[1] as LedgerHardwareWalletFactorSource).id.body,
                model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
                name = ledgerDeviceToAdd.hint.name
            )
        } returns AddLedgerFactorSourceResult.Added(
            ledgerFactorSource = ledgerDeviceToAdd
        )
        coEvery { getProfileUseCaseMock() } returns flowOf(profile())
        coEvery { ledgerMessengerMock.sendDeviceInfoRequest(any()) } returns Result.success(
            MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse(
                interactionId = "1",
                model = MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoS,
                deviceId = ledgerDeviceToAdd.id.body
            )
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.onSendAddLedgerRequestClick()
        advanceUntilIdle()
        vm.onConfirmLedgerNameClick("My Ledger")
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.showContent == AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo)
        }
    }
}
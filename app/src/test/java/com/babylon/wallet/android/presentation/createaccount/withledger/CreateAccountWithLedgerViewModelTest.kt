package com.babylon.wallet.android.presentation.createaccount.withledger

import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.AddLedgerFactorSourceResult

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateAccountWithLedgerViewModelTest : StateViewModelTest<CreateAccountWithLedgerViewModel>() {
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val ledgerMessenger = mockk<LedgerMessenger>()
    private val addLedgerFactorSourceUseCase = mockk<AddLedgerFactorSourceUseCase>()
    private val eventBus = mockk<AppEventBus>()

    private val firstDeviceId = "5f47ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010996f5"
    private val secondDeviceId = "5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5"

    override fun initVM(): CreateAccountWithLedgerViewModel {
        return CreateAccountWithLedgerViewModel(getProfileUseCase, ledgerMessenger, addLedgerFactorSourceUseCase, eventBus)
    }

    @Before
    override fun setUp() {
        super.setUp()
        coEvery { eventBus.sendEvent(any()) } just Runs
        coEvery { getProfileUseCase() } returns flowOf(profile())
        coEvery { ledgerMessenger.sendDeviceInfoRequest(any()) } returns Result.success(
            MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse(
                interactionId = "1",
                model = MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoS,
                deviceId = FactorSource.HexCoded32Bytes(firstDeviceId)
            )
        )
        coEvery {
            addLedgerFactorSourceUseCase(
                FactorSource.HexCoded32Bytes(firstDeviceId),
                any(),
                any()
            )
        } returns AddLedgerFactorSourceResult.Added(
            LedgerHardwareWalletFactorSource.newSource(
                model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
                name = "ledger",
                deviceID = FactorSource.HexCoded32Bytes(secondDeviceId)
            )
        )
        coEvery { ledgerMessenger.sendDerivePublicKeyRequest(any(), any(), any()) } returns Result.success(
            MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse(
                "1",
                listOf(
                    MessageFromDataChannel.LedgerResponse.DerivedPublicKey(
                        MessageFromDataChannel.LedgerResponse.DerivedPublicKey.Curve.Curve25519,
                        "publicKeyHex",
                        "path"
                    )
                )
            )
        )
    }

    @Test
    fun `initial state is correct with 1 factor source and no p2pLinks`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.ledgerFactorSources.size == 1)
            assert(!item.hasP2pLinks)
            assert(item.ledgerFactorSources.first { it.selected }.data.id.body.value == secondDeviceId)
        }
    }

    @Test
    fun `initial state is correct with 1 factor source and 1 p2pLink`() = runTest {
        coEvery { getProfileUseCase() } returns flowOf(profile(p2pLinks = listOf(P2PLink("pwd", "chrome"))))
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.ledgerFactorSources.size == 1)
            assert(!item.hasP2pLinks)
            assert(item.ledgerFactorSources.first { it.selected }.data.id.body.value == secondDeviceId)
        }
    }

    @Test
    fun `sending ledger device info requests call proper method and sets proper state`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onSendAddLedgerRequest()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.addLedgerSheetState == AddLedgerSheetState.InputLedgerName)
            assert(item.recentlyConnectedLedgerDevice != null)
        }
        coVerify(exactly = 1) { ledgerMessenger.sendDeviceInfoRequest(any()) }
    }

    @Test
    fun `adding ledger and providing name`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onSendAddLedgerRequest()
        advanceUntilIdle()
        vm.onConfirmLedgerName("My Ledger")
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.addLedgerSheetState == AddLedgerSheetState.Connect)
        }
    }

    @Test
    fun `adding ledger and skipping name`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onSendAddLedgerRequest()
        advanceUntilIdle()
        val ledgerName = mutableListOf<String?>()
        vm.onSkipLedgerName()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            println("item = $item")
            assert(item.addLedgerSheetState == AddLedgerSheetState.Connect)
        }
        coVerify(exactly = 1) {
            addLedgerFactorSourceUseCase(
                ledgerId = FactorSource.HexCoded32Bytes(firstDeviceId),
                model = any(),
                name = captureNullable(ledgerName)
            )
        }
        assert(ledgerName.first() == null)
    }

    @Test
    fun `use ledger to create account`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onUseLedger()
        advanceUntilIdle()
        val event = slot<AppEvent.DerivedAccountPublicKeyWithLedger>()
        coVerify(exactly = 1) { ledgerMessenger.sendDerivePublicKeyRequest(any(), any(), any()) }
        coVerify(exactly = 1) { eventBus.sendEvent(capture(event)) }
        assert(event.captured.derivedPublicKeyHex == "publicKeyHex")
        assert(event.captured.factorSourceID.body.value == secondDeviceId)
    }
}

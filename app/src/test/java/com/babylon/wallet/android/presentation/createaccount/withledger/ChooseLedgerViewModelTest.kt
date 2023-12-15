package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.account.createaccount.withledger.ARG_NETWORK_ID
import com.babylon.wallet.android.presentation.account.createaccount.withledger.ARG_SELECTION_PURPOSE
import com.babylon.wallet.android.presentation.account.createaccount.withledger.ChooseLedgerViewModel
import com.babylon.wallet.android.presentation.account.createaccount.withledger.LedgerSelectionPurpose
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import rdx.works.core.HexCoded32Bytes
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceResult
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.domain.p2pLinks

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChooseLedgerViewModelTest : StateViewModelTest<ChooseLedgerViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val ledgerMessenger = mockk<LedgerMessenger>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val addLedgerFactorSourceUseCase = mockk<AddLedgerFactorSourceUseCase>()
    private val ensureBabylonFactorSourceExistUseCase = mockk<EnsureBabylonFactorSourceExistUseCase>()
    private val eventBus = mockk<AppEventBus>()
    private val savedStateHandle = mockk<SavedStateHandle>()


    private val firstDeviceId = "5f47ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010996f5"
    private val secondDeviceId = "5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5"

    override fun initVM(): ChooseLedgerViewModel {
        return ChooseLedgerViewModel(
            getProfileUseCase,
            getCurrentGatewayUseCase,
            ledgerMessenger,
            ensureBabylonFactorSourceExistUseCase,
            eventBus,
            savedStateHandle
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        coEvery { ledgerMessenger.isConnected } returns flowOf(true)
        coEvery { eventBus.sendEvent(any()) } just Runs
        coEvery { getProfileUseCase() } returns flowOf(profile())
        every { savedStateHandle.get<Int>(ARG_NETWORK_ID) } returns Radix.Gateway.mainnet.network.id
        every { savedStateHandle.get<LedgerSelectionPurpose>(ARG_SELECTION_PURPOSE) } returns LedgerSelectionPurpose.CreateAccount
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.mainnet
        coEvery {
            addLedgerFactorSourceUseCase(
                HexCoded32Bytes(firstDeviceId),
                any(),
                any()
            )
        } returns AddLedgerFactorSourceResult.Added(
            LedgerHardwareWalletFactorSource.newSource(
                model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
                name = "ledger",
                deviceID = HexCoded32Bytes(secondDeviceId)
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
            assert(item.ledgerDevices.size == 1)
            assert(item.ledgerDevices.first { it.selected }.data.id.body.value == secondDeviceId)
        }
    }

    @Test
    fun `initial state is correct with 1 factor source and 1 p2pLink`() = runTest {
        coEvery { getProfileUseCase() } returns flowOf(profile(p2pLinks = listOf(P2PLink("pwd", "chrome"))))
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.ledgerDevices.size == 1)
            assert(item.ledgerDevices.first { it.selected }.data.id.body.value == secondDeviceId)
        }
    }


    @Ignore
    @Test
    fun `use ledger to create account`() = runTest {
        coEvery { getProfileUseCase.p2pLinks } returns flowOf(listOf(P2PLink("pwd", "chrome")))
        val vm = vm.value
        advanceUntilIdle()
        vm.onUseLedgerContinueClick { true }
        advanceUntilIdle()
        val event = slot<AppEvent.DerivedAccountPublicKeyWithLedger>()
        coVerify(exactly = 1) {
            ledgerMessenger.sendDerivePublicKeyRequest(any(), any(), any())
        }
        coVerify(exactly = 1) {
            eventBus.sendEvent(capture(event))
        }
        assert(event.captured.derivedPublicKeyHex == "publicKeyHex")
        assert(event.captured.factorSourceID.body.value == secondDeviceId)
    }
}

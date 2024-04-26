package com.babylon.wallet.android.presentation.createaccount.withledger

import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets.AddLedgerDeviceUiState
import com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets.AddLedgerDeviceViewModel
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCommon
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.FactorSources
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.LedgerHardwareWalletHint
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.append
import com.radixdlt.sargon.extensions.get
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import rdx.works.core.sargon.babylon
import rdx.works.profile.domain.AddLedgerFactorSourceResult
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase

@Ignore("TODO integration")
@OptIn(ExperimentalCoroutinesApi::class)
class AddLedgerDeviceViewModelTest : StateViewModelTest<AddLedgerDeviceViewModel>() {

    private val firstDeviceId = FactorSourceId.Hash(
        FactorSourceIdFromHash(
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET, Exactly32Bytes.init(
                "5f47ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010996f5".hexToBagOfBytes()
            )
        )
    )
    private val profile = Profile.sample().let {
        val factorSources = FactorSources.init(
            it.factorSources().filterNot { fs -> fs.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET }
        ).append(
            FactorSource.Ledger(
                LedgerHardwareWalletFactorSource(
                    firstDeviceId.value,
                    FactorSourceCommon(
                        cryptoParameters = FactorSourceCryptoParameters.babylon,
                        addedOn = Timestamp.now(),
                        lastUsedOn = Timestamp.now(),
                        flags = emptyList()
                    ),
                    LedgerHardwareWalletHint("Name", LedgerHardwareWalletModel.NANO_S)
                )
            )
        )
        it.copy(factorSources = factorSources)
    }

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
        coEvery { ledgerMessengerMock.isAnyLinkedConnectorConnected } returns flowOf(true)
    }

    @Test
    fun `sending ledger device info requests call proper method and sets proper state`() = runTest {
        coEvery { getProfileUseCaseMock() } returns profile
        every { getProfileUseCaseMock.flow } returns flowOf(profile)
        coEvery { ledgerMessengerMock.sendDeviceInfoRequest(any()) } returns Result.success(
            MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse(
                interactionId = "1",
                model = MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoS,
                deviceId = firstDeviceId.value.body
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
        val ledgerDeviceToAdd = profile.factorSources.get(firstDeviceId) as FactorSource.Ledger
        coEvery {
            addLedgerFactorSourceUseCaseMock(
                ledgerId = firstDeviceId,
                model = LedgerHardwareWalletModel.NANO_S,
                name = "Name"
            )
        } returns AddLedgerFactorSourceResult.Added(
            ledgerFactorSource = ledgerDeviceToAdd
        )
        coEvery { getProfileUseCaseMock() } returns profile
        coEvery { ledgerMessengerMock.sendDeviceInfoRequest(any()) } returns Result.success(
            MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse(
                interactionId = "1",
                model = MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoS,
                deviceId = ledgerDeviceToAdd.value.id.body
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
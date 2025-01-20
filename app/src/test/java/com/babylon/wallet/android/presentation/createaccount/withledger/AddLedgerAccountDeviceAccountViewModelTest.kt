package com.babylon.wallet.android.presentation.createaccount.withledger

import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice.AddLedgerDeviceUiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice.AddLedgerDeviceViewModel
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCommon
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.LedgerHardwareWalletHint
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.FactorSources
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
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
import org.junit.Test
import rdx.works.core.sargon.babylon
import rdx.works.profile.domain.AddLedgerFactorSourceResult
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class AddLedgerAccountDeviceAccountViewModelTest : StateViewModelTest<AddLedgerDeviceViewModel>() {

    private val firstDeviceId = FactorSourceId.Hash(
        FactorSourceIdFromHash(
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET, Exactly32Bytes.init(
                "5f47ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010996f5".hexToBagOfBytes()
            )
        )
    )
    private val profile = Profile.sample().let {
        val factorSources = FactorSources(
            it.factorSources.filterNot { fs -> fs.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET }
        ).append(
            FactorSource.Ledger(
                LedgerHardwareWalletFactorSource(
                    id = FactorSourceIdFromHash(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        body = Exactly32Bytes.init("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5".hexToBagOfBytes())
                    ),
                    FactorSourceCommon(
                        cryptoParameters = FactorSourceCryptoParameters.babylon,
                        addedOn = Timestamp.now(),
                        lastUsedOn = Timestamp.now(),
                        flags = emptyList()
                    ),
                    LedgerHardwareWalletHint("My Ledger", LedgerHardwareWalletModel.NANO_S)
                )
            )
        )
        it.copy(factorSources = factorSources.asList())
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
            LedgerResponse.GetDeviceInfoResponse(
                interactionId = "1",
                model = LedgerResponse.LedgerDeviceModel.NanoS,
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
        val ledgerDeviceToAdd =
            profile.factorSources.first { it.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET } as FactorSource.Ledger
        coEvery {
            addLedgerFactorSourceUseCaseMock(
                ledgerId = ledgerDeviceToAdd.value.id.asGeneral(),
                model = LedgerHardwareWalletModel.NANO_S,
                name = ledgerDeviceToAdd.value.hint.label
            )
        } returns AddLedgerFactorSourceResult.Added(
            ledgerFactorSource = ledgerDeviceToAdd
        )
        coEvery { getProfileUseCaseMock() } returns profile
        coEvery { ledgerMessengerMock.sendDeviceInfoRequest(any()) } returns Result.success(
            LedgerResponse.GetDeviceInfoResponse(
                interactionId = "1",
                model = LedgerResponse.LedgerDeviceModel.NanoS,
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
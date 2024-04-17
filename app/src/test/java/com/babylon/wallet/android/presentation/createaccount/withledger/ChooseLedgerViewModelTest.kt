package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.account.createaccount.withledger.ARG_SELECTION_PURPOSE
import com.babylon.wallet.android.presentation.account.createaccount.withledger.ChooseLedgerViewModel
import com.babylon.wallet.android.presentation.account.createaccount.withledger.LedgerSelectionPurpose
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCommon
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.LedgerHardwareWalletHint
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.FactorSources
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.samples.sample
import com.babylon.wallet.android.utils.AppEventBusImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rdx.works.core.sargon.babylon
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChooseLedgerViewModelTest : StateViewModelTest<ChooseLedgerViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val p2pLinksRepository = mockk<P2PLinksRepository>()
    private val ledgerMessenger = mockk<LedgerMessenger>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val addLedgerFactorSourceUseCase = mockk<AddLedgerFactorSourceUseCase>()
    private val eventBus = mockk<AppEventBus>()
    private val savedStateHandle = mockk<SavedStateHandle>()


    private val firstDeviceId = FactorSourceId.Hash(
        FactorSourceIdFromHash(
            kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
            body = Exactly32Bytes.init("5f47ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010996f5".hexToBagOfBytes())
        )
    )

    private val profile = Profile.sample().let {
        val factorSources = FactorSources(
            it.factorSources.filterNot { fs -> fs.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET }
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
                    LedgerHardwareWalletHint("", LedgerHardwareWalletModel.NANO_S)
                )
            )
        )
        it.copy(factorSources = factorSources.asList())
    }

    override fun initVM(): ChooseLedgerViewModel {
        return ChooseLedgerViewModel(
            getProfileUseCase,
            eventBus,
            p2pLinksRepository,
            savedStateHandle
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        coEvery { ledgerMessenger.isAnyLinkedConnectorConnected } returns flowOf(true)
        coEvery { eventBus.sendEvent(any()) } just Runs
        coEvery { getProfileUseCase() } returns profile
        every { getProfileUseCase.flow } returns flowOf(profile)
        every { savedStateHandle.get<LedgerSelectionPurpose>(ARG_SELECTION_PURPOSE) } returns LedgerSelectionPurpose.DerivePublicKey
        coEvery { getCurrentGatewayUseCase() } returns Gateway.forNetwork(NetworkId.MAINNET)
    }

    @Test
    fun `initial state is correct with 1 factor source and no p2pLinks`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assertEquals(1, item.ledgerDevices.size)
            assertEquals(firstDeviceId, item.ledgerDevices.first { it.selected }.data.id)
        }
    }
}

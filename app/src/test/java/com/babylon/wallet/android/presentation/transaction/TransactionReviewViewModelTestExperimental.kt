package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.gateway.generated.models.CoreApiTransactionReceipt
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitResponse
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.usecases.SignTransactionUseCase
import com.babylon.wallet.android.fakes.fakeProfileDataSource
import com.babylon.wallet.android.mockdata.createProfile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.transaction.vectors.requestMetadata
import com.babylon.wallet.android.presentation.transaction.vectors.sampleManifest
import com.babylon.wallet.android.presentation.transaction.vectors.testViewModel
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AssetsTransfersRecipient
import com.radixdlt.sargon.CompiledNotarizedIntent
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PerRecipientAssetTransfer
import com.radixdlt.sargon.PerRecipientAssetTransfers
import com.radixdlt.sargon.PerRecipientFungibleTransfer
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.perRecipientTransfers
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.xrd
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.transaction.NotarizationResult
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.sargon.toSargon
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionReviewViewModelTestExperimental : StateViewModelTest<TransactionReviewViewModel>(
    testDispatcherRule = TestDispatcherRule(dispatcher = UnconfinedTestDispatcher())
) {

    @get:Rule
    val defaultLocaleTestRule = DefaultLocaleRule()

    private val transactionId = UUID.randomUUID().toString()
    private val testProfile = createProfile(
        gateway = Radix.Gateway.stokenet,
        numberOfAccounts = 2
    )

    private val savedStateHandle = mockk<SavedStateHandle>().apply {
        every { get<String>(ARG_TRANSACTION_REQUEST_ID) } returns transactionId
    }
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val transactionRepository = mockk<TransactionRepository>().apply {
        coEvery { getLedgerEpoch() } returns Result.success(1000.toULong())
        coEvery { getTransactionPreview(any()) } returns Result.success(
            TransactionPreviewResponse(
                encodedReceipt = "",
                receipt = CoreApiTransactionReceipt(status = "success"),
                logs = emptyList()
            )
        )
    }
    private val stateRepository = mockk<StateRepository>()
    private val dAppMessenger = mockk<DappMessenger>()
    private val appEventBus = mockk<AppEventBus>()
    private val exceptionMessageProvider = mockk<ExceptionMessageProvider>()
    private val signTransactionUseCase = mockk<SignTransactionUseCase>().apply {
        every { signingState } returns flowOf()
    }
    private val preferencesManager = mockk<PreferencesManager>()

    private val profileRepository = fakeProfileDataSource(initialProfileState = ProfileState.Restored(testProfile))
    private val testScope = TestScope(context = coroutineRule.dispatcher)

    override fun initVM(): TransactionReviewViewModel = testViewModel(
        transactionRepository = transactionRepository,
        incomingRequestRepository = incomingRequestRepository,
        signTransactionUseCase = signTransactionUseCase,
        profileRepository = profileRepository,
        stateRepository = stateRepository,
        dAppMessenger = dAppMessenger,
        appEventBus = appEventBus,
        preferencesManager = preferencesManager,
        exceptionMessageProvider = exceptionMessageProvider,
        savedStateHandle = savedStateHandle,
        testScope = testScope
    )

    @Test
    fun `given transaction id, when this id does not exist in the queue, then dismiss the transaction`() = runTest {
        every { incomingRequestRepository.getTransactionWriteRequest(transactionId) } returns null

        vm.value.state.test {
            assertTrue("The transaction should be dismissed, but didn't", awaitItem().isTransactionDismissed)
        }
    }

    @Ignore("Not ready yet")
    @Test
    fun `transaction approval success`() = runTest {
        mockManifestInput(manifestData = simpleXRDTransfer(testProfile))
        coEvery { stateRepository.getOwnedXRD(testProfile.networks.first().accounts) } returns Result.success(
            testProfile.networks.first().accounts.associateWith { 10.toDecimal192() }
        )
        val notarization = NotarizationResult(
            intentHash = IntentHash.sample(),
            compiledNotarizedIntent = CompiledNotarizedIntent.sample(),
            endEpoch = 0u
        )
        coEvery { signTransactionUseCase.sign(any(), any()) } returns Result.success(notarization)
        coEvery { transactionRepository.submitTransaction(any()) } returns Result.success(TransactionSubmitResponse(duplicate = false))


        vm.value.state.test {
            println("---------> ${awaitItem()}")

            //vm.value.onPayerSelected(selectedFeePayer = testProfile.networks.first().accounts.first())
            //println("---------> ${awaitItem()}")
            //ensureAllEventsConsumed()
            //vm.value.approveTransaction { true }

//            coVerify(exactly = 1) {
//                dAppMessenger.sendTransactionWriteResponseSuccess(
//                    remoteConnectorId = "remoteConnectorId",
//                    requestId = transactionId,
//                    txId = notarisation.txIdHash
//                )
//            }
        }
    }

    class DefaultLocaleRule : TestRule {
        override fun apply(base: Statement, description: Description): Statement {
            return object : Statement() {
                override fun evaluate() {
                    Locale.setDefault(Locale.UK)
                    base.evaluate()
                }
            }
        }
    }

    private fun mockManifestInput(manifestData: TransactionManifestData = sampleManifest(instructions = "")) {
        val transactionRequest = MessageFromDataChannel.IncomingRequest.TransactionRequest(
            remoteConnectorId = "",
            requestId = transactionId,
            transactionManifestData = manifestData,
            requestMetadata = requestMetadata(manifestData = manifestData)
        ).also {
            println(it.transactionManifestData.instructions)
        }
        coEvery { incomingRequestRepository.getTransactionWriteRequest(transactionId) } returns transactionRequest
    }

    private fun simpleXRDTransfer(withProfile: Profile): TransactionManifestData = TransactionManifestData.from(
        manifest = TransactionManifest.perRecipientTransfers(
            transfers = PerRecipientAssetTransfers(
                addressOfSender = AccountAddress.init(withProfile.networks.first().accounts.first().address),
                transfers = listOf(
                    PerRecipientAssetTransfer(
                        recipient = AssetsTransfersRecipient.MyOwnAccount(value = withProfile.networks.first().accounts[1].toSargon()),
                        fungibles = listOf(
                            PerRecipientFungibleTransfer(
                                useTryDepositOrAbort = false,
                                amount = 2.toDecimal192(),
                                divisibility = null,
                                resourceAddress = ResourceAddress.xrd(NetworkId.init(withProfile.networks.first().networkID.toUByte()))
                            )
                        ),
                        nonFungibles = emptyList()
                    )
                )
            )
        ),
    )
}
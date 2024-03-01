package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
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
import com.babylon.wallet.android.presentation.transaction.vectors.requestMetadata
import com.babylon.wallet.android.presentation.transaction.vectors.sampleManifest
import com.babylon.wallet.android.presentation.transaction.vectors.testViewModel
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import rdx.works.core.domain.resources.XrdResource
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.ret.ManifestPoet
import rdx.works.profile.ret.transaction.TransactionManifestData
import rdx.works.profile.ret.transaction.TransactionSigner
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionReviewViewModelTestExperimental : StateViewModelTest<TransactionReviewViewModel>() {

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
        coEvery { getLedgerEpoch() } returns Result.success(1000)
        coEvery { getTransactionPreview(any()) } returns Result.success(TransactionPreviewResponse(
            encodedReceipt = "",
            receipt = CoreApiTransactionReceipt(status = "success"),
            logs = emptyList()
        ))
    }
    private val stateRepository = mockk<StateRepository>()
    private val dAppMessenger = mockk<DappMessenger>()
    private val appEventBus = mockk<AppEventBus>()
    private val exceptionMessageProvider = mockk<ExceptionMessageProvider>()
    private val signTransactionUseCase = mockk<SignTransactionUseCase>().apply {
        every { signingState } returns flowOf()
    }

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
        exceptionMessageProvider = exceptionMessageProvider,
        savedStateHandle = savedStateHandle,
        testScope = testScope
    )

    @Test
    fun `given transaction id, when this id does not exist in the queue, then dismiss the transaction`() = runTest {
        every { incomingRequestRepository.getTransactionWriteRequest(transactionId) } returns null

        val viewModel = vm.value
        advanceUntilIdle()

        assertTrue("The transaction should be dismissed, but didn't", viewModel.state.value.isTransactionDismissed)
    }

    @Ignore("Not ready")
    @Test
    fun `transaction approval success`() = runTest {
        mockManifestInput(manifestData = simpleXRDTransfer(testProfile))
        coEvery { stateRepository.getOwnedXRD(testProfile.networks.first().accounts) } returns Result.success(
            testProfile.networks.first().accounts.associateWith { BigDecimal.TEN }
        )

        vm.value
        advanceUntilIdle()

        val notarisation = TransactionSigner.Notarization(txIdHash = "tx_id", notarizedTransactionIntentHex = "intent_hash", endEpoch = 0u)
        coEvery { signTransactionUseCase.sign(any(), any()) } returns Result.success(notarisation)
        coEvery { transactionRepository.submitTransaction(any()) } returns Result.success(TransactionSubmitResponse(duplicate = false))
        vm.value.onPayerSelected(selectedFeePayer = testProfile.networks.first().accounts.first())
        advanceUntilIdle()

        vm.value.approveTransaction { true }
        advanceUntilIdle()

        coVerify(exactly = 1) {
            dAppMessenger.sendTransactionWriteResponseSuccess(
                remoteConnectorId = "remoteConnectorId",
                requestId = transactionId,
                txId = notarisation.txIdHash
            )
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
        )
        coEvery { incomingRequestRepository.getTransactionWriteRequest(transactionId) } returns transactionRequest
    }

    private fun simpleXRDTransfer(withProfile: Profile): TransactionManifestData = ManifestPoet.buildTransfer(
        fromAccount = withProfile.networks.first().accounts.first(),
        depositFungibles = listOf(
            ManifestPoet.FungibleTransfer(
                toAccountAddress = withProfile.networks.first().accounts[1].address,
                resourceAddress = XrdResource.address(withProfile.networks.first().networkID),
                amount = 2.toBigDecimal(),
                signatureRequired = true
            )
        ),
        depositNFTs = emptyList()
    ).getOrThrow()

}
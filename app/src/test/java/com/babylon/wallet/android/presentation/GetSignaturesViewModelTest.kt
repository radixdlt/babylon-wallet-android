package com.babylon.wallet.android.presentation

import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.signing.EntityWithSignature
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.domain.usecases.signing.SignWithDeviceFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignWithLedgerFactorSourceUseCase
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.mockdata.sampleWithLedgerAccount
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.GetSignaturesViewModel
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.GetSignaturesViewModel.State
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.sargon.allAccountsOnCurrentNetwork
import rdx.works.core.sargon.transactionSigningFactorInstance
import rdx.works.profile.domain.GetProfileUseCase

private val sampleProfile = Profile.sampleWithLedgerAccount()

private val signers = sampleProfile.allAccountsOnCurrentNetwork.map { it.asProfileEntity() }
private val signRequest = SignRequest.SignTransactionRequest(
    intent = TransactionIntent.sample()
)

@OptIn(ExperimentalCoroutinesApi::class)
class GetSignaturesViewModelTest : StateViewModelTest<GetSignaturesViewModel>() {

    private val signWithDeviceFactorSourceUseCaseMock = mockk<SignWithDeviceFactorSourceUseCase>()
    private val signWithLedgerFactorSourceUseCaseMock = mockk<SignWithLedgerFactorSourceUseCase>()

    private val accessFactorSourcesProxyFake = AccessFactorSourcesProxyFake()

    override fun initVM(): GetSignaturesViewModel {
        return GetSignaturesViewModel(
            accessFactorSourcesIOHandler = accessFactorSourcesProxyFake,
            signWithDeviceFactorSourceUseCase = signWithDeviceFactorSourceUseCaseMock,
            signWithLedgerFactorSourceUseCase = signWithLedgerFactorSourceUseCaseMock,
            getProfileUseCase = GetProfileUseCase(profileRepository = FakeProfileRepository(sampleProfile)),
            defaultDispatcher = StandardTestDispatcher()
        )
    }

    @Test
    fun `given singers with ledger and device factor sources, the first factor source to sign is always ledger`() = runTest {
        coEvery { signWithLedgerFactorSourceUseCaseMock(any(), any(), any()) } returns Result.success(listOf())
        coEvery { signWithDeviceFactorSourceUseCaseMock(any(), any(), any()) } returns Result.success(listOf())

        val viewModel = vm.value
        advanceUntilIdle()
        viewModel.state.test {
            val state = expectMostRecentItem()
            // here we get the second signer because the first one (ledger) has already signed! (because of the mock above)
            // Therefore the second signer must be FactorSourceKind.DEVICE
            val nextRequest = state.nextRequest as? State.FactorSourceRequest.DeviceRequest
            val factorSourceKindOfSecondSigner = nextRequest?.deviceFactorSources?.keys?.first()?.kind
            assertNotNull(factorSourceKindOfSecondSigner)
            assertTrue(factorSourceKindOfSecondSigner == FactorSourceKind.DEVICE)
        }
    }

    @Test
    fun `given device factor source, when signing, then show bottom sheet content for device factors`() = runTest {
        coEvery { signWithLedgerFactorSourceUseCaseMock(any(), any(), any()) } returns Result.success(listOf())
        coEvery { signWithDeviceFactorSourceUseCaseMock(any(), any(), any()) } returns Result.success(listOf())

        val viewModel = vm.value
        advanceUntilIdle()
        viewModel.state.test {
            val state = expectMostRecentItem()
            val showContentForFactorSource = state.showContentForFactorSource
            assertTrue(showContentForFactorSource == State.ShowContentForFactorSource.Device)
        }
    }

    @Test
    fun `given ledger factor source to sign, when fails with generic error, then keep the bottom sheet for ledger open`() = runTest {
        coEvery { signWithLedgerFactorSourceUseCaseMock(any(), any(), any()) } returns Result.failure(
            exception = RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(reason = LedgerErrorCode.Generic)
        )

        val viewModel = vm.value
        advanceUntilIdle()
        viewModel.state.test {
            val state = expectMostRecentItem()

            val nextRequest = state.nextRequest as? State.FactorSourceRequest.LedgerRequest
            val factorSourceKindOfSecondSigner = nextRequest?.factorSource?.kind
            assertNotNull(factorSourceKindOfSecondSigner)
            assertTrue(factorSourceKindOfSecondSigner == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET)

            val showContentForFactorSource = state.showContentForFactorSource
            assertTrue(
                showContentForFactorSource == State.ShowContentForFactorSource.Ledger(
                    ledgerFactorSource = sampleProfile.factorSources[2] as FactorSource.Ledger
                )
            )
        }
    }

    // caller = the WalletSignatureGatherer of the SignTransactionUseCase -> TransactionSubmitDelegate -> TransactionReviewViewModel
    @Test
    fun `given ledger and device factor sources to sign, when signing successfully, then return successful output to the caller`() = runTest {
        assertTrue(signers[0].securityState.transactionSigningFactorInstance.factorSourceId.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET)
        val signerWithLedgerFactorSource = signers[0]
        assertTrue(signers[1].securityState.transactionSigningFactorInstance.factorSourceId.kind == FactorSourceKind.DEVICE)
        val signerWithDeviceFactorSource = signers[1]

        val signatureFromLedger = SignatureWithPublicKey.sample.invoke()
        val signatureFromDevice = SignatureWithPublicKey.sample.other()

        backgroundScope.launch(Dispatchers.Default)   { // TransactionReviewScreen needs to access factor sources to get signatures
            val result = accessFactorSourcesProxyFake.getSignatures(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                    signers = listOf(signerWithLedgerFactorSource, signerWithDeviceFactorSource),
                    signRequest = signRequest
                )
            )
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.signersWithSignatures.size == 2)
            assertTrue(result.getOrNull()!!.signersWithSignatures[signers[0]] == signatureFromLedger)
            assertTrue(result.getOrNull()!!.signersWithSignatures[signers[1]] == signatureFromDevice)
        }

        coEvery { signWithLedgerFactorSourceUseCaseMock(any(), any(), any()) } returns Result.success(
            listOf(EntityWithSignature(entity = signerWithLedgerFactorSource, signatureWithPublicKey = signatureFromLedger))
        )
        coEvery { signWithDeviceFactorSourceUseCaseMock(any(), any(), any()) } returns Result.success(
            listOf(EntityWithSignature(entity = signerWithDeviceFactorSource, signatureWithPublicKey = signatureFromDevice))
        )

        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.collectSignaturesForDeviceFactorSource()
        advanceUntilIdle()
        viewModel.state.test {
            val state = expectMostRecentItem()
            val isSignatureFromLedgerCollected = state.entitiesWithSignatures.values.toList().contains(signatureFromLedger)
            assertTrue(isSignatureFromLedgerCollected)
            val isSignatureFromDeviceCollected = state.entitiesWithSignatures.values.toList().contains(signatureFromDevice)
            assertTrue(isSignatureFromDeviceCollected)
        }
    }

    // caller = the WalletSignatureGatherer of the SignTransactionUseCase -> TransactionSubmitDelegate -> TransactionReviewViewModel
    @Test
    fun `given ledger and device factor sources to sign, when one of the factor source sign fails, then end signing process and return failure`() = runTest {
        backgroundScope.launch(Dispatchers.Default)  { // TransactionReviewScreen needs to access factor sources to get signatures
            val result = accessFactorSourcesProxyFake.getSignatures(AccessFactorSourcesInput.ToGetSignatures(signers, signRequest))
            assertTrue(result.isFailure)
        }

        val signature = SignatureWithPublicKey.sample()
        coEvery { signWithLedgerFactorSourceUseCaseMock(any(), any(), any()) } returns Result.success(
            listOf(EntityWithSignature(entity = signers[0], signatureWithPublicKey = signature))
        )
        coEvery { signWithDeviceFactorSourceUseCaseMock(any(), any(), any()) } returns Result.failure(exception = IllegalStateException("User has no fingers"))

        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.collectSignaturesForDeviceFactorSource()
        advanceUntilIdle()
        viewModel.state.test {
            val state = expectMostRecentItem()
            val isSignatureCollected = state.entitiesWithSignatures.containsValue(signature)
            assertTrue(isSignatureCollected)
        }
    }
}


class AccessFactorSourcesProxyFake : AccessFactorSourcesProxy, AccessFactorSourcesIOHandler {

    private val _output = MutableSharedFlow<AccessFactorSourcesOutput>()

    override suspend fun getPublicKeyAndDerivationPathForFactorSource(accessFactorSourcesInput: AccessFactorSourcesInput.ToDerivePublicKey): Result<AccessFactorSourcesOutput.HDPublicKey> {
        TODO("Not yet implemented")
    }

    override suspend fun reDeriveAccounts(accessFactorSourcesInput: AccessFactorSourcesInput.ToReDeriveAccounts): Result<AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath> {
        TODO("Not yet implemented")
    }

    override suspend fun getSignatures(accessFactorSourcesInput: AccessFactorSourcesInput.ToGetSignatures): Result<AccessFactorSourcesOutput.EntitiesWithSignatures> {
        val result = _output.first()
        return if (result is AccessFactorSourcesOutput.Failure) {
            Result.failure(result.error)
        } else {
            Result.success(result as AccessFactorSourcesOutput.EntitiesWithSignatures)
        }
    }

    override fun setTempMnemonicWithPassphrase(mnemonicWithPassphrase: MnemonicWithPassphrase) {
        TODO("Not yet implemented")
    }

    override fun getTempMnemonicWithPassphrase(): MnemonicWithPassphrase? {
        TODO("Not yet implemented")
    }

    override fun getInput(): AccessFactorSourcesInput {
        return AccessFactorSourcesInput.ToGetSignatures(
            signers = signers,
            signRequest = signRequest
        )
    }

    override suspend fun setOutput(output: AccessFactorSourcesOutput) {
        _output.emit(output)
    }

}

package com.babylon.wallet.android.presentation.interactor

import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.HDSignatureInput
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.HdSignature
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.InputPerFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.InputPerTransaction
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.OutputPerFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.SignaturesPerFactorSource
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AuthIntent
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPurpose
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HdSignatureInputOfAuthIntentHash
import com.radixdlt.sargon.HdSignatureInputOfSubintentHash
import com.radixdlt.sargon.HdSignatureInputOfTransactionIntentHash
import com.radixdlt.sargon.HdSignatureOfAuthIntentHash
import com.radixdlt.sargon.HdSignatureOfSubintentHash
import com.radixdlt.sargon.HdSignatureOfTransactionIntentHash
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequest
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.KeyDerivationResponse
import com.radixdlt.sargon.KeyDerivationResponsePerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.SignRequestOfAuthIntent
import com.radixdlt.sargon.SignRequestOfSubintent
import com.radixdlt.sargon.SignRequestOfTransactionIntent
import com.radixdlt.sargon.SignResponseOfAuthIntentHash
import com.radixdlt.sargon.SignResponseOfSubintentHash
import com.radixdlt.sargon.SignResponseOfTransactionIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfAuthIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfSubintentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.SignaturesPerFactorSourceOfAuthIntentHash
import com.radixdlt.sargon.SignaturesPerFactorSourceOfSubintentHash
import com.radixdlt.sargon.SignaturesPerFactorSourceOfTransactionIntentHash
import com.radixdlt.sargon.Subintent
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.TransactionSignRequestInputOfAuthIntent
import com.radixdlt.sargon.TransactionSignRequestInputOfSubintent
import com.radixdlt.sargon.TransactionSignRequestInputOfTransactionIntent
import com.radixdlt.sargon.TransactionToSignPerFactorSourceOfAuthIntent
import com.radixdlt.sargon.TransactionToSignPerFactorSourceOfSubintent
import com.radixdlt.sargon.TransactionToSignPerFactorSourceOfTransactionIntent
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.sign
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import rdx.works.core.sargon.Signable
import rdx.works.core.sargon.transactionSigningFactorInstance

class WalletInteractorTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val mnemonicWithPassphrase = MnemonicWithPassphrase.sample()
    private val proxy = mockk<AccessFactorSourcesProxy>()
    private val sut = WalletInteractor(accessFactorSourcesProxy = proxy)

    @Test
    fun testDeriveKeys() = runTest {
        val factorSource = DeviceFactorSource.sample()
        val derivationPaths = listOf(
            DerivationPath.sample(),
            DerivationPath.sample.other(),
        )
        val expectedFactorInstances = listOf(
            HierarchicalDeterministicFactorInstance(
                factorSourceId = factorSource.id,
                publicKey = mnemonicWithPassphrase.derivePublicKey(derivationPaths[0])
            ),
            HierarchicalDeterministicFactorInstance(
                factorSourceId = factorSource.id,
                publicKey = mnemonicWithPassphrase.derivePublicKey(derivationPaths[1])
            )
        )
        coEvery {
            proxy.derivePublicKeys(
                AccessFactorSourcesInput.ToDerivePublicKeys(
                    purpose = DerivationPurpose.CREATING_NEW_ACCOUNT,
                    factorSourceId = factorSource.id,
                    derivationPaths = derivationPaths
                )
            )
        } returns AccessFactorSourcesOutput.DerivedPublicKeys.Success(
            factorSourceId = factorSource.id,
            factorInstances = expectedFactorInstances
        )

        val response = sut.deriveKeys(
            KeyDerivationRequest(
                derivationPurpose = DerivationPurpose.CREATING_NEW_ACCOUNT,
                perFactorSource = listOf(
                    KeyDerivationRequestPerFactorSource(
                        factorSourceId = factorSource.id,
                        derivationPaths = derivationPaths,
                    )
                )
            )
        )

        assertEquals(
            KeyDerivationResponse(
                perFactorSource = listOf(
                    KeyDerivationResponsePerFactorSource(
                        factorSourceId = factorSource.id,
                        factorInstances = expectedFactorInstances
                    )
                )
            ),
            response
        )
    }

    @Test
    fun testSignTransactions() = runTest {
        val device = DeviceFactorSource.sample()
        val transaction = TransactionIntent.sample()

        val accounts = listOf(
            Account.sampleMainnet(),
            Account.sampleMainnet.other()
        )
        val instances = accounts.map {
            OwnedFactorInstance(
                owner = it.asProfileEntity().address,
                factorInstance = it.securityState.transactionSigningFactorInstance
            )

        }
        val expectedSignatures: Map<OwnedFactorInstance, SignatureWithPublicKey> = instances.associateWith {
            mnemonicWithPassphrase.sign(transaction.hash().hash, it.factorInstance.publicKey.derivationPath)
        }

        coEvery {
            proxy.sign<Signable.Payload.Transaction, Signable.ID.Transaction>(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign(
                    kind = FactorSourceKind.DEVICE,
                    perFactorSource = listOf(
                        InputPerFactorSource(
                            factorSourceId = device.id,
                            transactions = listOf(
                                InputPerTransaction(
                                    payload = Signable.Payload.Transaction(transaction.compile()),
                                    factorSourceId = device.id,
                                    ownedFactorInstances = instances
                                )
                            )
                        )
                    ),
                    purpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents
                )
            )
        } returns AccessFactorSourcesOutput.SignOutput(
            perFactorSource = listOf(
                OutputPerFactorSource.Signed(
                    signatures = SignaturesPerFactorSource(
                        factorSourceId = device.id,
                        hdSignatures = expectedSignatures.map { entry ->
                            HdSignature(
                                input = HDSignatureInput(
                                    payloadId = Signable.ID.Transaction(transaction.hash()),
                                    ownedFactorInstance = entry.key
                                ),
                                signature = entry.value
                            )
                        }
                    )
                )
            )
        )

        val response = sut.signTransactions(
            request = SignRequestOfTransactionIntent(
                factorSourceKind = FactorSourceKind.DEVICE,
                perFactorSource = listOf(
                    TransactionToSignPerFactorSourceOfTransactionIntent(
                        factorSourceId = device.id,
                        transactions = listOf(
                            TransactionSignRequestInputOfTransactionIntent(
                                payload = transaction.compile(),
                                factorSourceId = device.id,
                                ownedFactorInstances = instances
                            ),
                        )
                    )
                ),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertEquals(
            SignWithFactorsOutcomeOfTransactionIntentHash.Signed(
                producedSignatures = SignResponseOfTransactionIntentHash(
                    perFactorSource = listOf(
                        SignaturesPerFactorSourceOfTransactionIntentHash(
                            factorSourceId = device.id,
                            hdSignatures = expectedSignatures.map {
                                HdSignatureOfTransactionIntentHash(
                                    input = HdSignatureInputOfTransactionIntentHash(
                                        payloadId = transaction.hash(),
                                        ownedFactorInstance = it.key
                                    ),
                                    signature = it.value
                                )
                            }
                        )
                    )
                )
            ),
            response
        )
    }

    @Test
    fun testSignSubintent() = runTest {
        val device = DeviceFactorSource.sample()
        val subintent = Subintent.sample()

        val accounts = listOf(
            Account.sampleMainnet(),
            Account.sampleMainnet.other()
        )
        val instances = accounts.map {
            OwnedFactorInstance(
                owner = it.asProfileEntity().address,
                factorInstance = it.securityState.transactionSigningFactorInstance
            )

        }
        val expectedSignatures: Map<OwnedFactorInstance, SignatureWithPublicKey> = instances.associateWith {
            mnemonicWithPassphrase.sign(subintent.hash().hash, it.factorInstance.publicKey.derivationPath)
        }

        coEvery {
            proxy.sign<Signable.Payload.Subintent, Signable.ID.Subintent>(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign(
                    kind = FactorSourceKind.DEVICE,
                    perFactorSource = listOf(
                        InputPerFactorSource(
                            factorSourceId = device.id,
                            transactions = listOf(
                                InputPerTransaction(
                                    payload = Signable.Payload.Subintent(subintent.compile()),
                                    factorSourceId = device.id,
                                    ownedFactorInstances = instances
                                )
                            )
                        )
                    ),
                    purpose = AccessFactorSourcesInput.ToSign.Purpose.SubIntents
                )
            )
        } returns AccessFactorSourcesOutput.SignOutput(
            perFactorSource = listOf(
                OutputPerFactorSource.Signed(
                    signatures = SignaturesPerFactorSource(
                        factorSourceId = device.id,
                        hdSignatures = expectedSignatures.map { entry ->
                            HdSignature(
                                input = HDSignatureInput(
                                    payloadId = Signable.ID.Subintent(subintent.hash()),
                                    ownedFactorInstance = entry.key
                                ),
                                signature = entry.value
                            )
                        }
                    )
                )
            )
        )

        val response = sut.signSubintents(
            request = SignRequestOfSubintent(
                factorSourceKind = FactorSourceKind.DEVICE,
                perFactorSource = listOf(
                    TransactionToSignPerFactorSourceOfSubintent(
                        factorSourceId = device.id,
                        transactions = listOf(
                            TransactionSignRequestInputOfSubintent(
                                payload = subintent.compile(),
                                factorSourceId = device.id,
                                ownedFactorInstances = instances
                            ),
                        )
                    )
                ),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertEquals(
            SignWithFactorsOutcomeOfSubintentHash.Signed(
                producedSignatures = SignResponseOfSubintentHash(
                    perFactorSource = listOf(
                        SignaturesPerFactorSourceOfSubintentHash(
                            factorSourceId = device.id,
                            hdSignatures = expectedSignatures.map {
                                HdSignatureOfSubintentHash(
                                    input = HdSignatureInputOfSubintentHash(
                                        payloadId = subintent.hash(),
                                        ownedFactorInstance = it.key
                                    ),
                                    signature = it.value
                                )
                            }
                        )
                    )
                )
            ),
            response
        )
    }

    @Test
    fun testSignAuth() = runTest {
        val device = DeviceFactorSource.sample()
        val authIntent = AuthIntent.sample()

        val accounts = listOf(
            Account.sampleMainnet(),
            Account.sampleMainnet.other()
        )
        val instances = accounts.map {
            OwnedFactorInstance(
                owner = it.asProfileEntity().address,
                factorInstance = it.securityState.transactionSigningFactorInstance
            )

        }
        val expectedSignatures: Map<OwnedFactorInstance, SignatureWithPublicKey> = instances.associateWith {
            mnemonicWithPassphrase.sign(authIntent.hash().payload.hash(), it.factorInstance.publicKey.derivationPath)
        }

        coEvery {
            proxy.sign<Signable.Payload.Auth, Signable.ID.Auth>(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign(
                    kind = FactorSourceKind.DEVICE,
                    perFactorSource = listOf(
                        InputPerFactorSource(
                            factorSourceId = device.id,
                            transactions = listOf(
                                InputPerTransaction(
                                    payload = Signable.Payload.Auth(authIntent),
                                    factorSourceId = device.id,
                                    ownedFactorInstances = instances
                                )
                            )
                        )
                    ),
                    purpose = AccessFactorSourcesInput.ToSign.Purpose.AuthIntents
                )
            )
        } returns AccessFactorSourcesOutput.SignOutput(
            perFactorSource = listOf(
                OutputPerFactorSource.Signed(
                    signatures = SignaturesPerFactorSource(
                        factorSourceId = device.id,
                        hdSignatures = expectedSignatures.map { entry ->
                            HdSignature(
                                input = HDSignatureInput(
                                    payloadId = Signable.ID.Auth(authIntent.hash()),
                                    ownedFactorInstance = entry.key
                                ),
                                signature = entry.value
                            )
                        }
                    )
                )
            )
        )

        val response = sut.signAuth(
            request = SignRequestOfAuthIntent(
                factorSourceKind = FactorSourceKind.DEVICE,
                perFactorSource = listOf(
                    TransactionToSignPerFactorSourceOfAuthIntent(
                        factorSourceId = device.id,
                        transactions = listOf(
                            TransactionSignRequestInputOfAuthIntent(
                                payload = authIntent,
                                factorSourceId = device.id,
                                ownedFactorInstances = instances
                            ),
                        )
                    )
                ),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertEquals(
            SignWithFactorsOutcomeOfAuthIntentHash.Signed(
                producedSignatures = SignResponseOfAuthIntentHash(
                    perFactorSource = listOf(
                        SignaturesPerFactorSourceOfAuthIntentHash(
                            factorSourceId = device.id,
                            hdSignatures = expectedSignatures.map {
                                HdSignatureOfAuthIntentHash(
                                    input = HdSignatureInputOfAuthIntentHash(
                                        payloadId = authIntent.hash(),
                                        ownedFactorInstance = it.key
                                    ),
                                    signature = it.value
                                )
                            }
                        )
                    )
                )
            ),
            response
        )
    }
}
package com.babylon.wallet.android.presentation.sargonInteractors

import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AuthIntent
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPurpose
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.FactorOutcomeOfSubintentHash
import com.radixdlt.sargon.FactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.FactorSource
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
import com.radixdlt.sargon.PerFactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.PerFactorOutcomeOfSubintentHash
import com.radixdlt.sargon.PerFactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.PerFactorSourceInputOfAuthIntent
import com.radixdlt.sargon.PerFactorSourceInputOfSubintent
import com.radixdlt.sargon.PerFactorSourceInputOfTransactionIntent
import com.radixdlt.sargon.SignRequestOfAuthIntent
import com.radixdlt.sargon.SignRequestOfSubintent
import com.radixdlt.sargon.SignRequestOfTransactionIntent
import com.radixdlt.sargon.SignResponseOfAuthIntentHash
import com.radixdlt.sargon.SignResponseOfSubintentHash
import com.radixdlt.sargon.SignResponseOfTransactionIntentHash
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.SpotCheckResponse
import com.radixdlt.sargon.Subintent
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.TransactionSignRequestInputOfAuthIntent
import com.radixdlt.sargon.TransactionSignRequestInputOfSubintent
import com.radixdlt.sargon.TransactionSignRequestInputOfTransactionIntent
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.sign
import com.radixdlt.sargon.extensions.unsecuredControllingFactorInstance
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.HdSignature
import com.radixdlt.sargon.os.signing.HdSignatureInput
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import com.radixdlt.sargon.os.signing.TransactionSignRequestInput
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

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
                    request = KeyDerivationRequestPerFactorSource(
                        factorSourceId = factorSource.id,
                        derivationPaths = derivationPaths
                    )
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
                factorInstance = requireNotNull(it.unsecuredControllingFactorInstance)
            )

        }
        val expectedSignatures: Map<OwnedFactorInstance, SignatureWithPublicKey> = instances.associateWith {
            mnemonicWithPassphrase.sign(transaction.hash().hash, it.factorInstance.publicKey.derivationPath)
        }

        coEvery {
            proxy.sign(
                AccessFactorSourcesInput.SignTransaction(
                    factorSourceId = device.id,
                    input = PerFactorSourceInputOfTransactionIntent(
                        factorSourceId = device.id,
                        perTransaction = listOf(
                            TransactionSignRequestInputOfTransactionIntent(
                                payload = transaction.compile(),
                                factorSourceId = device.id,
                                ownedFactorInstances = instances
                            ),
                        ),
                        invalidTransactionsIfNeglected = emptyList()
                    )
                )
            )
        } returns AccessFactorSourcesOutput.SignTransaction(
            outcome = PerFactorOutcomeOfTransactionIntentHash(
                factorSourceId = device.id,
                outcome = FactorOutcomeOfTransactionIntentHash.Signed(
                    producedSignatures = expectedSignatures.map { entry ->
                        HdSignatureOfTransactionIntentHash(
                            input = HdSignatureInputOfTransactionIntentHash(
                                payloadId = transaction.hash(),
                                ownedFactorInstance = entry.key
                            ),
                            signature = entry.value
                        )
                    }
                )
            )
        )

        val response = sut.signTransactions(
            request = SignRequestOfTransactionIntent(
                factorSourceKind = FactorSourceKind.DEVICE,
                perFactorSource = listOf(
                    PerFactorSourceInputOfTransactionIntent(
                        factorSourceId = device.id,
                        perTransaction = listOf(
                            TransactionSignRequestInputOfTransactionIntent(
                                payload = transaction.compile(),
                                factorSourceId = device.id,
                                ownedFactorInstances = instances
                            ),
                        ),
                        invalidTransactionsIfNeglected = emptyList()
                    )
                )
            )
        )

        assertEquals(
            SignResponseOfTransactionIntentHash(
                perFactorOutcome = listOf(
                    PerFactorOutcomeOfTransactionIntentHash(
                        factorSourceId = device.id,
                        outcome = FactorOutcomeOfTransactionIntentHash.Signed(
                            producedSignatures = expectedSignatures.map {
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
                factorInstance = requireNotNull(it.unsecuredControllingFactorInstance)
            )

        }
        val expectedSignatures: Map<OwnedFactorInstance, SignatureWithPublicKey> = instances.associateWith {
            mnemonicWithPassphrase.sign(subintent.hash().hash, it.factorInstance.publicKey.derivationPath)
        }

        coEvery {
            proxy.sign(
                AccessFactorSourcesInput.SignSubintent(
                    factorSourceId = device.id,
                    input = PerFactorSourceInputOfSubintent(
                        factorSourceId = device.id,
                        perTransaction = listOf(
                            TransactionSignRequestInputOfSubintent(
                                payload = subintent.compile(),
                                factorSourceId = device.id,
                                ownedFactorInstances = instances
                            ),
                        ),
                        invalidTransactionsIfNeglected = emptyList()
                    )
                )
            )
        } returns AccessFactorSourcesOutput.SignSubintent(
            outcome = PerFactorOutcomeOfSubintentHash(
                factorSourceId = device.id,
                outcome = FactorOutcomeOfSubintentHash.Signed(
                    producedSignatures = expectedSignatures.map { entry ->
                        HdSignatureOfSubintentHash(
                            input = HdSignatureInputOfSubintentHash(
                                payloadId = subintent.hash(),
                                ownedFactorInstance = entry.key
                            ),
                            signature = entry.value
                        )
                    }
                )
            )
        )

        val response = sut.signSubintents(
            request = SignRequestOfSubintent(
                factorSourceKind = FactorSourceKind.DEVICE,
                perFactorSource = listOf(
                    PerFactorSourceInputOfSubintent(
                        factorSourceId = device.id,
                        perTransaction = listOf(
                            TransactionSignRequestInputOfSubintent(
                                payload = subintent.compile(),
                                factorSourceId = device.id,
                                ownedFactorInstances = instances
                            ),
                        ),
                        invalidTransactionsIfNeglected = emptyList()
                    )
                )
            )
        )

        assertEquals(
            SignResponseOfSubintentHash(
                perFactorOutcome = listOf(
                    PerFactorOutcomeOfSubintentHash(
                        factorSourceId = device.id,
                        outcome = FactorOutcomeOfSubintentHash.Signed(
                            producedSignatures = expectedSignatures.map {
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
                factorInstance = requireNotNull(it.unsecuredControllingFactorInstance)
            )

        }
        val expectedSignatures: Map<OwnedFactorInstance, SignatureWithPublicKey> = instances.associateWith {
            mnemonicWithPassphrase.sign(authIntent.hash().payload.hash(), it.factorInstance.publicKey.derivationPath)
        }

        coEvery {
            proxy.sign(
                AccessFactorSourcesInput.SignAuth(
                    factorSourceId = device.id,
                    input = PerFactorSourceInputOfAuthIntent(
                        factorSourceId = device.id,
                        perTransaction = listOf(
                            TransactionSignRequestInputOfAuthIntent(
                                payload = authIntent,
                                factorSourceId = device.id,
                                ownedFactorInstances = instances
                            ),
                        ),
                        invalidTransactionsIfNeglected = emptyList()
                    )
                )
            )
        } returns AccessFactorSourcesOutput.SignAuth(
            outcome = PerFactorOutcomeOfAuthIntentHash(
                factorSourceId = device.id,
                outcome = FactorOutcomeOfAuthIntentHash.Signed(
                    producedSignatures = expectedSignatures.map { entry ->
                        HdSignatureOfAuthIntentHash(
                            input = HdSignatureInputOfAuthIntentHash(
                                payloadId = authIntent.hash(),
                                ownedFactorInstance = entry.key
                            ),
                            signature = entry.value
                        )
                    }
                )
            )
        )

        val response = sut.signAuth(
            request = SignRequestOfAuthIntent(
                factorSourceKind = FactorSourceKind.DEVICE,
                perFactorSource = listOf(
                    PerFactorSourceInputOfAuthIntent(
                        factorSourceId = device.id,
                        perTransaction = listOf(
                            TransactionSignRequestInputOfAuthIntent(
                                payload = authIntent,
                                factorSourceId = device.id,
                                ownedFactorInstances = instances
                            ),
                        ),
                        invalidTransactionsIfNeglected = emptyList()
                    )
                )
            )
        )

        assertEquals(
            SignResponseOfAuthIntentHash(
                perFactorOutcome = listOf(
                    PerFactorOutcomeOfAuthIntentHash(
                        factorSourceId = device.id,
                        outcome = FactorOutcomeOfAuthIntentHash.Signed(
                            producedSignatures = expectedSignatures.map {
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

    @Test
    fun testSpotCheck() = runTest {
        val factorSource = FactorSource.sample()
        val expectedResponse = SpotCheckResponse.VALID
        coEvery { proxy.spotCheck(factorSource, false) } returns AccessFactorSourcesOutput.SpotCheckOutput.Completed(
            response = expectedResponse
        )

        val response = sut.spotCheck(
            factorSource = factorSource,
            allowSkip = false
        )

        assertEquals(
            expectedResponse,
            response
        )
    }
}
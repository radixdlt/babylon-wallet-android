package com.babylon.wallet.android.presentation.interactor

import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPurpose
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HdSignatureInputOfSubintentHash
import com.radixdlt.sargon.HdSignatureInputOfTransactionIntentHash
import com.radixdlt.sargon.HdSignatureOfSubintentHash
import com.radixdlt.sargon.HdSignatureOfTransactionIntentHash
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequest
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.KeyDerivationResponse
import com.radixdlt.sargon.KeyDerivationResponsePerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.SignRequestOfSubintent
import com.radixdlt.sargon.SignRequestOfTransactionIntent
import com.radixdlt.sargon.SignResponseOfSubintentHash
import com.radixdlt.sargon.SignResponseOfTransactionIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfSubintentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.SignaturesPerFactorSourceOfSubintentHash
import com.radixdlt.sargon.SignaturesPerFactorSourceOfTransactionIntentHash
import com.radixdlt.sargon.Subintent
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.TransactionSignRequestInputOfSubintent
import com.radixdlt.sargon.TransactionSignRequestInputOfTransactionIntent
import com.radixdlt.sargon.TransactionToSignPerFactorSourceOfSubintent
import com.radixdlt.sargon.TransactionToSignPerFactorSourceOfTransactionIntent
import com.radixdlt.sargon.extensions.ProfileEntity
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

        val instances = mapOf(
            Account.sampleMainnet() to DerivationPath.sample(),
            Account.sampleMainnet.other() to DerivationPath.sample.other(),
        )
        val expectedSignatures: Map<ProfileEntity, SignatureWithPublicKey> = instances.keys.associate {
            it.asProfileEntity() to mnemonicWithPassphrase.sign(transaction.hash().hash, instances[it]!!)
        }

        coEvery {
            proxy.sign(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.Transactions(
                    perFactorSource = listOf(
                        TransactionToSignPerFactorSourceOfTransactionIntent(
                            factorSourceId = device.id,
                            transactions = listOf(
                                TransactionSignRequestInputOfTransactionIntent(
                                    payload = transaction.compile(),
                                    factorSourceId = device.id,
                                    ownedFactorInstances = instances.keys.map { owner ->
                                        OwnedFactorInstance(
                                            owner = AddressOfAccountOrPersona.Account(owner.address),
                                            factorInstance = HierarchicalDeterministicFactorInstance(
                                                factorSourceId = device.id,
                                                publicKey = mnemonicWithPassphrase.derivePublicKey(instances[owner]!!)
                                            )
                                        )
                                    }
                                )
                            )
                        )
                    )
                )
            )
        } returns AccessFactorSourcesOutput.EntitiesWithSignatures.Success(signersWithSignatures = expectedSignatures)

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
                                ownedFactorInstances = instances.keys.map { owner ->
                                    OwnedFactorInstance(
                                        owner = AddressOfAccountOrPersona.Account(owner.address),
                                        factorInstance = HierarchicalDeterministicFactorInstance(
                                            factorSourceId = device.id,
                                            publicKey = mnemonicWithPassphrase.derivePublicKey(instances[owner]!!)
                                        )
                                    )
                                }
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
                                        ownedFactorInstance = OwnedFactorInstance(
                                            owner = it.key.address,
                                            factorInstance = it.key.securityState.transactionSigningFactorInstance
                                        )
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

        val instances = mapOf(
            Account.sampleMainnet() to DerivationPath.sample(),
            Account.sampleMainnet.other() to DerivationPath.sample.other(),
        )
        val expectedSignatures: Map<ProfileEntity, SignatureWithPublicKey> = instances.keys.associate {
            it.asProfileEntity() to mnemonicWithPassphrase.sign(subintent.hash().hash, instances[it]!!)
        }
        coEvery {
            proxy.sign(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.Subintents(
                    perFactorSource = TransactionSignRequestInputOfSubintent(
                        payload = subintent.compile(),
                        ownedFactorInstances = instances.keys.map { owner ->
                            OwnedFactorInstance(
                                owner = AddressOfAccountOrPersona.Account(owner.address),
                                factorInstance = HierarchicalDeterministicFactorInstance(
                                    factorSourceId = device.id,
                                    publicKey = mnemonicWithPassphrase.derivePublicKey(instances[owner]!!)
                                )
                            )
                        },
                        factorSourceId = device.id
                    )
                )
            )
        } returns AccessFactorSourcesOutput.EntitiesWithSignatures.Success(signersWithSignatures = expectedSignatures)

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
                                ownedFactorInstances = instances.keys.map { owner ->
                                    OwnedFactorInstance(
                                        owner = AddressOfAccountOrPersona.Account(owner.address),
                                        factorInstance = HierarchicalDeterministicFactorInstance(
                                            factorSourceId = device.id,
                                            publicKey = mnemonicWithPassphrase.derivePublicKey(instances[owner]!!)
                                        )
                                    )
                                }
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
                                        ownedFactorInstance = OwnedFactorInstance(
                                            owner = it.key.address,
                                            factorInstance = it.key.securityState.transactionSigningFactorInstance
                                        )
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
package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.AuthIntent
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSourceCommon
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.LedgerHardwareWalletHint
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.SpotCheckInput
import com.radixdlt.sargon.Subintent
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.bip32String
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.publicKey
import com.radixdlt.sargon.extensions.sign
import com.radixdlt.sargon.extensions.signature
import com.radixdlt.sargon.extensions.spotCheck
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.newLedgerHardwareWalletFromMnemonicWithPassphrase
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.HdSignature
import com.radixdlt.sargon.os.signing.HdSignatureInput
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import com.radixdlt.sargon.os.signing.TransactionSignRequestInput
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.profile.domain.UpdateFactorSourceLastUsedUseCase

class AccessLedgerHardwareWalletFactorSourceUseCaseTest {

    private val ledgerMessenger = mockk<LedgerMessenger>()
    private val updateFactorSourceLastUsedUseCase = mockk<UpdateFactorSourceLastUsedUseCase>()

    private val mnemonicWithPassphrase = MnemonicWithPassphrase.sample()
    private val ledger = newLedgerHardwareWalletFromMnemonicWithPassphrase(
        mwp = mnemonicWithPassphrase,
        hint = LedgerHardwareWalletHint(
            label = "Test",
            model = LedgerHardwareWalletModel.NANO_X
        ),
        common = FactorSourceCommon(
            cryptoParameters = FactorSourceCryptoParameters(
                supportedCurves = listOf(Slip10Curve.CURVE25519),
                supportedDerivationPathSchemes = listOf(DerivationPathScheme.CAP26)
            ),
            addedOn = Timestamp.now(),
            lastUsedOn = Timestamp.now(),
            flags = emptyList()
        )
    )

    private val ownedFactorInstance = OwnedFactorInstance(
        owner = AddressOfAccountOrPersona.sampleMainnet(),
        factorInstance = HierarchicalDeterministicFactorInstance.sample()
    )

    private val sut = AccessLedgerHardwareWalletFactorSourceUseCase(
        ledgerMessenger = ledgerMessenger,
        updateFactorSourceLastUsedUseCase = updateFactorSourceLastUsedUseCase
    )

    @Test
    fun derivePublicKeysFailsDueToAnyErrorToLedgerMessenger() = runTest {
        coEvery {
            ledgerMessenger.sendDerivePublicKeyRequest(
                interactionId = any(),
                keyParameters = any(),
                ledgerDevice = any()
            )
        } returns Result.failure(RadixWalletException.LedgerCommunicationException.FailedToDerivePublicKeys)

        val result = sut.derivePublicKeys(
            factorSource = ledger.asGeneral(),
            input = KeyDerivationRequestPerFactorSource(
                factorSourceId = ledger.id,
                derivationPaths = listOf(DerivationPath.sample())
            )
        )

        assertTrue(result.exceptionOrNull() is RadixWalletException.LedgerCommunicationException.FailedToDerivePublicKeys)
    }

    @Test
    fun signMonoFailsDueToAnyErrorToLedgerMessengerWhenSigningTransactions() = runTest {
        coEvery {
            ledgerMessenger.signTransactionRequest(
                interactionId = any(),
                ledgerDevice = any(),
                compiledTransactionIntent = any(),
                hdPublicKeys = any(),
                displayHashOnLedgerDisplay = any()
            )
        } returns Result.failure(RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(
            reason = LedgerErrorCode.Generic,
            message = null
        ))

        val result = sut.signMono(
            factorSource = ledger.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = ledger.id,
                perTransaction = listOf(
                    TransactionSignRequestInput(
                        payload = Signable.Payload.Transaction(TransactionIntent.sample().compile()),
                        factorSourceId = ledger.id,
                        ownedFactorInstances = listOf(ownedFactorInstance)
                    )
                ),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertTrue(result.exceptionOrNull() is RadixWalletException.LedgerCommunicationException.FailedToSignTransaction)
    }

    @Test
    fun signMonoFailsDueToAnyErrorToLedgerMessengerWhenSigningSubintents() = runTest {
        coEvery {
            ledgerMessenger.signSubintentHashRequest(
                interactionId = any(),
                ledgerDevice = any(),
                subintentHash = any(),
                hdPublicKeys = any(),
            )
        } returns Result.failure(RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(
            reason = LedgerErrorCode.Generic,
            message = null
        ))

        val result = sut.signMono(
            factorSource = ledger.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = ledger.id,
                perTransaction = listOf(
                    TransactionSignRequestInput(
                        payload = Signable.Payload.Subintent(Subintent.sample().compile()),
                        factorSourceId = ledger.id,
                        ownedFactorInstances = listOf(ownedFactorInstance)
                    )
                ),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertTrue(result.exceptionOrNull() is RadixWalletException.LedgerCommunicationException.FailedToSignTransaction)
    }

    @Test
    fun signMonoFailsDueToAnyErrorToLedgerMessengerWhenSigningAuthIntents() = runTest {
        coEvery {
            ledgerMessenger.signChallengeRequest(
                interactionId = any(),
                ledgerDevice = any(),
                hdPublicKeys = any(),
                challengeHex = any(),
                origin = any(),
                dAppDefinitionAddress = any()
            )
        } returns Result.failure(RadixWalletException.LedgerCommunicationException.FailedToSignAuthChallenge)

        val result = sut.signMono(
            factorSource = ledger.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = ledger.id,
                perTransaction = listOf(
                    TransactionSignRequestInput(
                        payload = Signable.Payload.Auth(AuthIntent.sample()),
                        factorSourceId = ledger.id,
                        ownedFactorInstances = listOf(ownedFactorInstance)
                    )
                ),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertTrue(result.exceptionOrNull() is RadixWalletException.LedgerCommunicationException.FailedToSignAuthChallenge)
    }

    @Test
    fun spotCheckFailsDueToAnyErrorToLedgerMessenger() = runTest {
        coEvery {
            ledgerMessenger.sendDeviceInfoRequest(interactionId = any())
        } returns Result.failure(RadixWalletException.LedgerCommunicationException.FailedToGetDeviceId)

        val result = sut.spotCheck(factorSource = ledger.asGeneral())

        assertTrue(result.exceptionOrNull() is RadixWalletException.LedgerCommunicationException.FailedToGetDeviceId)
    }

    @Test
    fun derivePublicKeysSucceeds() = runTest {
        val expectedHDPublicKey = mnemonicWithPassphrase.derivePublicKey(
            ownedFactorInstance.factorInstance.publicKey.derivationPath
        )
        coEvery { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) } just Runs
        coEvery {
            ledgerMessenger.sendDerivePublicKeyRequest(
                interactionId = any(),
                keyParameters = listOf(
                    LedgerInteractionRequest.KeyParameters.from(ownedFactorInstance.factorInstance.publicKey.derivationPath)
                ),
                ledgerDevice = any()
            )
        } returns Result.success(
            LedgerResponse.DerivePublicKeyResponse(
                interactionId = "",
                publicKeys = listOf(
                    LedgerResponse.DerivedPublicKey(
                        curve = LedgerResponse.DerivedPublicKey.Curve.Curve25519,
                        publicKeyHex = expectedHDPublicKey.publicKey.hex,
                        derivationPath = expectedHDPublicKey.derivationPath.bip32String
                    )
                )
            )
        )

        val result = sut.derivePublicKeys(
            factorSource = ledger.asGeneral(),
            input = KeyDerivationRequestPerFactorSource(
                factorSourceId = ledger.id,
                derivationPaths = listOf(ownedFactorInstance.factorInstance.publicKey.derivationPath)
            )
        )

        assertEquals(
            listOf(
                HierarchicalDeterministicFactorInstance(
                    factorSourceId = ledger.id,
                    publicKey = HierarchicalDeterministicPublicKey(
                        publicKey = expectedHDPublicKey.publicKey,
                        derivationPath = expectedHDPublicKey.derivationPath
                    )
                )
            ),
            result.getOrNull()
        )
        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) }
    }

    @Test
    fun signMonoForTransactionIntentSucceeds() = runTest {
        val transaction = TransactionIntent.sample()
        val signature = mnemonicWithPassphrase.sign(
            transaction.hash().hash,
            ownedFactorInstance.factorInstance.publicKey.derivationPath
        )

        coEvery { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) } just Runs
        coEvery {
            ledgerMessenger.signTransactionRequest(
                interactionId = any(),
                hdPublicKeys = listOf(ownedFactorInstance.factorInstance.publicKey),
                compiledTransactionIntent = transaction.compile().bytes.hex,
                ledgerDevice = any()
            )
        } returns Result.success(
            LedgerResponse.SignTransactionResponse(
                interactionId = "",
                signatures = listOf(
                    LedgerResponse.SignatureOfSigner(
                        derivedPublicKey = LedgerResponse.DerivedPublicKey(
                            curve = LedgerResponse.DerivedPublicKey.Curve.Curve25519,
                            publicKeyHex = signature.publicKey.hex,
                            derivationPath = ownedFactorInstance.factorInstance.publicKey.derivationPath.bip32String
                        ),
                        signature = signature.signature.bytes.hex
                    )
                )
            )
        )

        val result = sut.signMono(
            factorSource = ledger.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = ledger.id,
                perTransaction = listOf(
                    TransactionSignRequestInput(
                        payload = Signable.Payload.Transaction(transaction.compile()),
                        factorSourceId = ledger.id,
                        ownedFactorInstances = listOf(ownedFactorInstance)
                    )
                ),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertEquals(
            PerFactorOutcome(
                factorSourceId = ledger.id,
                outcome = FactorOutcome.Signed(
                    producedSignatures = listOf(
                        HdSignature(
                            input = HdSignatureInput(
                                payloadId = Signable.ID.Transaction(transaction.hash()),
                                ownedFactorInstance = ownedFactorInstance
                            ),
                            signature = signature
                        )
                    )
                )
            ),
            result.getOrNull()
        )
        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) }
    }

    @Test
    fun signMonoForSubintentIntentSucceeds() = runTest {
        val subintent = Subintent.sample()
        val signature = mnemonicWithPassphrase.sign(
            subintent.hash().hash,
            ownedFactorInstance.factorInstance.publicKey.derivationPath
        )

        coEvery { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) } just Runs
        coEvery {
            ledgerMessenger.signSubintentHashRequest(
                interactionId = any(),
                hdPublicKeys = listOf(ownedFactorInstance.factorInstance.publicKey),
                subintentHash = subintent.hash().hash.hex,
                ledgerDevice = any()
            )
        } returns Result.success(
            LedgerResponse.SignSubintentHashResponse(
                interactionId = "",
                signatures = listOf(
                    LedgerResponse.SignatureOfSigner(
                        derivedPublicKey = LedgerResponse.DerivedPublicKey(
                            curve = LedgerResponse.DerivedPublicKey.Curve.Curve25519,
                            publicKeyHex = signature.publicKey.hex,
                            derivationPath = ownedFactorInstance.factorInstance.publicKey.derivationPath.bip32String
                        ),
                        signature = signature.signature.bytes.hex
                    )
                )
            )
        )

        val result = sut.signMono(
            factorSource = ledger.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = ledger.id,
                perTransaction = listOf(
                    TransactionSignRequestInput(
                        payload = Signable.Payload.Subintent(subintent.compile()),
                        factorSourceId = ledger.id,
                        ownedFactorInstances = listOf(ownedFactorInstance)
                    )
                ),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertEquals(
            PerFactorOutcome(
                factorSourceId = ledger.id,
                outcome = FactorOutcome.Signed(
                    producedSignatures = listOf(
                        HdSignature(
                            input = HdSignatureInput(
                                payloadId = Signable.ID.Subintent(subintent.hash()),
                                ownedFactorInstance = ownedFactorInstance
                            ),
                            signature = signature
                        )
                    )
                )
            ),
            result.getOrNull()
        )
        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) }
    }

    @Test
    fun signMonoForAuthIntentSucceeds() = runTest {
        val authIntent = AuthIntent.sample()
        val signature = mnemonicWithPassphrase.sign(
            authIntent.hash().payload.hash(),
            ownedFactorInstance.factorInstance.publicKey.derivationPath
        )

        coEvery { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) } just Runs
        coEvery {
            ledgerMessenger.signChallengeRequest(
                interactionId = any(),
                hdPublicKeys = listOf(ownedFactorInstance.factorInstance.publicKey),
                challengeHex = authIntent.challengeNonce.hex,
                origin = authIntent.origin,
                dAppDefinitionAddress = authIntent.dappDefinitionAddress.string,
                ledgerDevice = any()
            )
        } returns Result.success(
            LedgerResponse.SignChallengeResponse(
                interactionId = "",
                signatures = listOf(
                    LedgerResponse.SignatureOfSigner(
                        derivedPublicKey = LedgerResponse.DerivedPublicKey(
                            curve = LedgerResponse.DerivedPublicKey.Curve.Curve25519,
                            publicKeyHex = signature.publicKey.hex,
                            derivationPath = ownedFactorInstance.factorInstance.publicKey.derivationPath.bip32String
                        ),
                        signature = signature.signature.bytes.hex
                    )
                )
            )
        )

        val result = sut.signMono(
            factorSource = ledger.asGeneral(),
            input = PerFactorSourceInput(
                factorSourceId = ledger.id,
                perTransaction = listOf(
                    TransactionSignRequestInput(
                        payload = Signable.Payload.Auth(authIntent),
                        factorSourceId = ledger.id,
                        ownedFactorInstances = listOf(ownedFactorInstance)
                    )
                ),
                invalidTransactionsIfNeglected = emptyList()
            )
        )

        assertEquals(
            PerFactorOutcome(
                factorSourceId = ledger.id,
                outcome = FactorOutcome.Signed(
                    producedSignatures = listOf(
                        HdSignature(
                            input = HdSignatureInput(
                                payloadId = Signable.ID.Auth(authIntent.hash()),
                                ownedFactorInstance = ownedFactorInstance
                            ),
                            signature = signature
                        )
                    )
                )
            ),
            result.getOrNull()
        )
        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) }
    }

    @Test
    fun spotCheckSucceeds() = runTest {
        val expectedDeviceId = Exactly32Bytes.sample()
        coEvery { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) } just Runs
        coEvery {
            ledgerMessenger.sendDeviceInfoRequest(interactionId = any())
        } returns Result.success(
            LedgerResponse.GetDeviceInfoResponse(
                interactionId = "",
                model = LedgerResponse.LedgerDeviceModel.NanoS,
                deviceId = expectedDeviceId
            )
        )
        mockkStatic("com.radixdlt.sargon.SargonKt")
        every {
            ledger.asGeneral().spotCheck(input = SpotCheckInput.Ledger(expectedDeviceId))
        } returns true

        val result = sut.spotCheck(factorSource = ledger.asGeneral()).getOrThrow()

        unmockkStatic("com.radixdlt.sargon.SargonKt")
        assertTrue(result)
        coVerify { updateFactorSourceLastUsedUseCase(factorSourceId = ledger.id.asGeneral()) }
    }
}
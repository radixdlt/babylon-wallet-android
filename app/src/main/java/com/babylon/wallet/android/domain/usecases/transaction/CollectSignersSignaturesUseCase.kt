package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.hash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.domain.SigningPurpose
import rdx.works.core.hash
import rdx.works.core.toByteArray
import rdx.works.profile.domain.signing.GetSigningEntitiesByFactorSourceUseCase
import rdx.works.profile.domain.signing.SignWithDeviceFactorSourceUseCase
import javax.inject.Inject

class CollectSignersSignaturesUseCase @Inject constructor(
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase,
    private val getSigningEntitiesByFactorSourceUseCase: GetSigningEntitiesByFactorSourceUseCase,
) {

    private val _interactionState = MutableStateFlow<InteractionState?>(null)
    val interactionState: Flow<InteractionState?> = _interactionState.asSharedFlow()

    @Suppress("ReturnCount", "LongMethod")
    suspend operator fun invoke(
        signers: List<ProfileEntity>,
        signRequest: SignRequest,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean,
        signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
    ): Result<List<SignatureWithPublicKey>> {
        var deviceAuthenticated = false
        val signaturesWithPublicKeys = mutableListOf<SignatureWithPublicKey>()
        val signersPerFactorSource = getSigningEntitiesByFactorSourceUseCase(signers)
        signersPerFactorSource.forEach { (factorSource, signers) ->
            when (factorSource) {
                is FactorSource.Device -> {
                    // here I assume that in the future we will grant
                    // access to keystore key for few seconds, so I ask for auth only once, instead of on every DEVICE signature
                    if (!deviceAuthenticated) {
                        deviceAuthenticated = deviceBiometricAuthenticationProvider()
                    }
                    if (!deviceAuthenticated) {
                        _interactionState.update { null }
                        return Result.failure(RadixWalletException.SignatureCancelled)
                    }
                    _interactionState.update { InteractionState.Device.Pending(factorSource, signingPurpose) }
                    signWithDeviceFactorSourceUseCase(
                        deviceFactorSource = factorSource,
                        signers = signers,
                        dataToSign = signRequest.hashedDataToSign,
                        signingPurpose = signingPurpose
                    ).onSuccess { signatures ->
                        signaturesWithPublicKeys.addAll(signatures)
                    }.onFailure {
                        _interactionState.update { null }
                        return Result.failure(it)
                    }
                }
                is FactorSource.Ledger -> {
                    if (!deviceAuthenticated) {
                        deviceAuthenticated = deviceBiometricAuthenticationProvider()
                    }
                    if (!deviceAuthenticated) {
                        _interactionState.update { null }
                        return Result.failure(RadixWalletException.SignatureCancelled)
                    }
                    _interactionState.update { InteractionState.Ledger.Pending(factorSource, signingPurpose) }
                    signWithLedgerFactorSourceUseCase(
                        ledgerFactorSource = factorSource,
                        signers = signers,
                        signRequest = signRequest,
                        signingPurpose = signingPurpose
                    ).onSuccess { signatures ->
                        _interactionState.update { null }
                        signaturesWithPublicKeys.addAll(signatures)
                    }.onFailure { error ->
                        _interactionState.update { null }
                        return Result.failure(error)
                    }
                }
            }
        }
        return Result.success(signaturesWithPublicKeys)
    }

    fun cancel() {
        _interactionState.update { null }
    }
}

sealed interface SignRequest {

    val dataToSign: ByteArray
    val hashedDataToSign: ByteArray

    class SignTransactionRequest(
        intent: TransactionIntent
    ) : SignRequest {
        override val dataToSign: ByteArray = intent.compile().toByteArray()
        override val hashedDataToSign: ByteArray = intent.hash().hash.bytes.bytes.toByteArray()
    }

    class SignAuthChallengeRequest(
        val challengeHex: String,
        val origin: String,
        val dAppDefinitionAddress: String
    ) : SignRequest {

        override val dataToSign: ByteArray
            get() {
                require(dAppDefinitionAddress.length <= UByte.MAX_VALUE.toInt())
                return byteArrayOf(ROLA_PAYLOAD_PREFIX.toByte()) + challengeHex.decodeHex() + dAppDefinitionAddress.length.toUByte()
                    .toByte() + dAppDefinitionAddress.toByteArray() + origin.toByteArray()
            }

        val payloadHex: String
            get() = dataToSign.toHexString()

        override val hashedDataToSign: ByteArray
            get() = dataToSign.hash().bytes.bytes.toByteArray()

        companion object {
            const val ROLA_PAYLOAD_PREFIX = 0x52
        }
    }
}

package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.transaction.SigningState
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.toolkit.hash
import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.SigningPurpose
import rdx.works.profile.domain.signing.GetSigningEntitiesByFactorSourceUseCase
import rdx.works.profile.domain.signing.SignWithDeviceFactorSourceUseCase
import javax.inject.Inject

class CollectSignersSignaturesUseCase @Inject constructor(
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase,
    private val getSigningEntitiesByFactorSourceUseCase: GetSigningEntitiesByFactorSourceUseCase,
) {

    private val _signingState = MutableStateFlow<SigningState?>(null)
    val signingState: Flow<SigningState?> = _signingState.asSharedFlow()

    suspend operator fun invoke(
        signers: List<Entity>,
        signRequest: SignRequest,
        signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
    ): Result<List<SignatureWithPublicKey>> {
        val signaturesWithPublicKeys = mutableListOf<SignatureWithPublicKey>()
        val signersPerFactorSource = getSigningEntitiesByFactorSourceUseCase(signers)
        signersPerFactorSource.forEach { (factorSource, signers) ->
            when (factorSource.kind) {
                FactorSourceKind.DEVICE -> {
                    _signingState.update { SigningState.Device.Pending(factorSource) }
                    val signatures = signWithDeviceFactorSourceUseCase(
                        deviceFactorSource = factorSource,
                        signers = signers,
                        dataToSign = signRequest.hashedDataToSign,
                        signingPurpose = signingPurpose
                    )
                    signaturesWithPublicKeys.addAll(signatures)
                    _signingState.update { SigningState.Device.Success(factorSource) }
                }

                FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> {
                    _signingState.update { SigningState.Ledger.Pending(factorSource) }
                    signWithLedgerFactorSourceUseCase(
                        ledgerFactorSource = factorSource,
                        signers = signers,
                        signRequest = signRequest,
                        signingPurpose = signingPurpose
                    ).onSuccess { signatures ->
                        _signingState.update { SigningState.Ledger.Success(factorSource) }
                        signaturesWithPublicKeys.addAll(signatures)
                    }.onFailure {
                        _signingState.update { SigningState.Ledger.Failure(factorSource) }
                        return Result.failure(it)
                    }
                }
            }
        }
        return Result.success(signaturesWithPublicKeys)
    }
}

sealed interface SignRequest {

    val hashedDataToSign: ByteArray

    class SignTransactionRequest(
        private val compiledTransactionIntentHash: ByteArray
    ) : SignRequest {

        override val hashedDataToSign: ByteArray
            get() = compiledTransactionIntentHash

    }

    class SignAuthChallengeRequest(
        val challengeHex: String,
        val origin: String,
        val dAppDefinitionAddress: String
    ) : SignRequest {

        private val payloadToSign: ByteArray
            get() {
                require(dAppDefinitionAddress.length <= UByte.MAX_VALUE.toInt())
                return challengeHex.decodeHex() + dAppDefinitionAddress.length.toUByte()
                    .toByte() + dAppDefinitionAddress.toByteArray() + origin.toByteArray()
            }

        val payloadHex: String
            get() = payloadToSign.toHexString()

        override val hashedDataToSign: ByteArray
            get() = hash(payloadToSign)
    }
}

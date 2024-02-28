@file:Suppress("TooGenericExceptionCaught", "ReturnCount", "LongMethod", "TooManyFunctions")

package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.gateway.extensions.asGatewayPublicKey
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequestFlags
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.model.TransactionApprovalRequest
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.ret.Intent
import com.radixdlt.ret.NotarizedTransaction
import com.radixdlt.ret.SignedIntent
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.NonceGenerator
import rdx.works.core.then
import rdx.works.profile.ret.addLockFeeInstructionToManifest
import rdx.works.profile.ret.crypto.PublicKey
import rdx.works.profile.ret.crypto.Signature
import rdx.works.profile.ret.crypto.SignatureWithPublicKey
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val resolveNotaryAndSignersUseCase: ResolveNotaryAndSignersUseCase,
    private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase
) {
    val signingState = collectSignersSignaturesUseCase.interactionState

    private val logger = Timber.tag("TransactionClient")

    fun cancelSigning() {
        collectSignersSignaturesUseCase.cancel()
    }

    suspend fun signTransaction(
        request: TransactionApprovalRequest,
        lockFee: BigDecimal,
        tipPercentage: UShort,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ): Result<NotarizedTransactionResult> = prepareSignedTransactionIntent(
        request = request,
        lockFee = lockFee,
        tipPercentage = tipPercentage,
        deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
    )

    private suspend fun prepareSignedTransactionIntent(
        request: TransactionApprovalRequest,
        lockFee: BigDecimal,
        tipPercentage: UShort,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ): Result<NotarizedTransactionResult> {
        val manifestWithTransactionFee = if (request.feePayerAddress == null) {
            request.manifest
        } else {
            request.manifest.addLockFeeInstructionToManifest(
                addressToLockFee = request.feePayerAddress,
                fee = lockFee
            )
        }
        return resolveNotaryAndSignersUseCase(
            summary = manifestWithTransactionFee.summary(request.networkId.value.toUByte()),
            notary = request.ephemeralNotaryPrivateKey
        ).then { notaryAndSigners ->
            buildTransactionHeader(
                networkId = request.networkId.value,
                notaryAndSigners = notaryAndSigners,
                tipPercentage = tipPercentage
            ).map { header -> notaryAndSigners to header }
        }.then { signersAndHeader ->
            val notaryAndSigners = signersAndHeader.first
            val header = signersAndHeader.second
            val transactionIntent = kotlin.runCatching {
                Intent(
                    header = header,
                    manifest = manifestWithTransactionFee,
                    message = request.message.toEngineMessage()
                )
            }.getOrElse {
                return Result.failure(RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent())
            }

            val transactionIntentHash = runCatching {
                transactionIntent.intentHash()
            }.getOrElse {
                return Result.failure(RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent())
            }

            val compiledTransactionIntent = runCatching {
                transactionIntent.compile()
            }.getOrElse {
                return Result.failure(RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction())
            }

            val signatures = collectSignersSignaturesUseCase(
                signers = notaryAndSigners.signers,
                signRequest = SignRequest.SignTransactionRequest(
                    dataToSign = compiledTransactionIntent,
                    hashedDataToSign = transactionIntentHash.bytes()
                ),
                deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
            ).getOrElse { throwable ->
                return if (throwable is RadixWalletException) {
                    Result.failure(throwable)
                } else {
                    Result.failure(RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent(throwable))
                }
            }

            val signedTransactionIntent = runCatching {
                SignedIntent(
                    intent = transactionIntent,
                    intentSignatures = signatures.map {
                        when (it) {
                            is SignatureWithPublicKey.Ed25519 -> com.radixdlt.ret.SignatureWithPublicKey.Ed25519(it.signature, it.publicKey)
                            is SignatureWithPublicKey.Secp256k1 -> com.radixdlt.ret.SignatureWithPublicKey.Secp256k1(it.signature)
                        }
                    }
                )
            }.getOrElse {
                return Result.failure(RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent(it))
            }

            val signedIntentHash = runCatching {
                signedTransactionIntent.signedIntentHash()
            }.getOrElse { error ->
                return Result.failure(RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction(error))
            }

            val notarySignature = notaryAndSigners.signWithNotary(hashedData = signedIntentHash.bytes())
            val compiledNotarizedIntent = runCatching {
                NotarizedTransaction(
                    signedIntent = signedTransactionIntent,
                    notarySignature = when (notarySignature) {
                        is Signature.Ed25519 -> com.radixdlt.ret.Signature.Ed25519(notarySignature.value)
                        is Signature.Secp256k1 -> com.radixdlt.ret.Signature.Secp256k1(notarySignature.value)
                    }
                ).compile()
            }.getOrElse { e ->
                return Result.failure(RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction(e))
            }
            Result.success(
                NotarizedTransactionResult(
                    txIdHash = transactionIntentHash.asStr(),
                    notarizedTransactionIntentHex = compiledNotarizedIntent.toHexString(),
                    transactionHeader = header
                )
            )
        }.onFailure {
            logger.w(it)
        }
    }

    private suspend fun buildTransactionHeader(
        networkId: Int,
        notaryAndSigners: NotaryAndSigners,
        tipPercentage: UShort,
    ): Result<TransactionHeader> {
        val epochResult = transactionRepository.getLedgerEpoch()
        return epochResult.getOrNull()?.let { epoch ->
            val expiryEpoch = epoch.toULong() + TransactionConfig.EPOCH_WINDOW
            return try {
                Result.success(
                    TransactionHeader(
                        networkId = networkId.toUByte(),
                        startEpochInclusive = epoch.toULong(),
                        endEpochExclusive = expiryEpoch,
                        nonce = NonceGenerator(),
                        notaryPublicKey = when (val key = notaryAndSigners.notaryPublicKey()) {
                            is PublicKey.Ed25519 -> com.radixdlt.ret.PublicKey.Ed25519(key.value)
                            is PublicKey.Secp256k1 -> com.radixdlt.ret.PublicKey.Secp256k1(key.value)
                        },
                        notaryIsSignatory = notaryAndSigners.notaryIsSignatory,
                        tipPercentage = tipPercentage
                    )
                )
            } catch (e: Exception) {
                Result.failure(RadixWalletException.PrepareTransactionException.BuildTransactionHeader(cause = e))
            }
        } ?: Result.failure(RadixWalletException.DappRequestException.GetEpoch)
    }
}

data class NotarizedTransactionResult(
    val txIdHash: String,
    val notarizedTransactionIntentHex: String,
    val transactionHeader: TransactionHeader
) {
    val endEpoch: ULong
        get() = transactionHeader.endEpochExclusive
}

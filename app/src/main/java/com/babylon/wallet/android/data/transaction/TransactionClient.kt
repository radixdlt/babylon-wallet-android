@file:Suppress("TooGenericExceptionCaught", "ReturnCount", "LongMethod", "TooManyFunctions")

package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.gateway.extensions.asGatewayPublicKey
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequestFlags
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.manifest.toPrettyString
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.data.transaction.model.TransactionApprovalRequest
import com.babylon.wallet.android.domain.common.asKotlinResult
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.findAccountWithEnoughXRDBalance
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.ret.Address
import com.radixdlt.ret.Intent
import com.radixdlt.ret.NotarizedTransaction
import com.radixdlt.ret.Signature
import com.radixdlt.ret.SignatureWithPublicKey
import com.radixdlt.ret.SignedIntent
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.ret.crypto.PrivateKey
import rdx.works.core.then
import rdx.works.core.toByteArray
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.personasOnCurrentNetwork
import timber.log.Timber
import java.math.BigDecimal
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.Result
import com.babylon.wallet.android.domain.common.Result as ResultInternal

@Suppress("LongParameterList")
class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase,
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val submitTransactionUseCase: SubmitTransactionUseCase
) {
    val signingState = collectSignersSignaturesUseCase.interactionState

    private val logger = Timber.tag("TransactionClient")

    fun cancelSigning() {
        collectSignersSignaturesUseCase.cancel()
    }

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

        val signers = getSigningEntities(manifestWithTransactionFee)
        val notaryAndSigners = NotaryAndSigners(signers, request.ephemeralNotaryPrivateKey)
        return buildTransactionHeader(
            networkId = request.networkId.value,
            notaryAndSigners = notaryAndSigners,
            tipPercentage = tipPercentage
        ).then { header ->
            val transactionIntent = kotlin.runCatching {
                Intent(
                    header = header,
                    manifest = manifestWithTransactionFee,
                    message = request.message.toEngineMessage()
                )
            }.getOrElse {
                return Result.failure(DappRequestException(DappRequestFailure.TransactionApprovalFailure.SignCompiledTransactionIntent))
            }

            val transactionIntentHash = runCatching {
                transactionIntent.intentHash()
            }.getOrElse {
                return Result.failure(DappRequestException(DappRequestFailure.TransactionApprovalFailure.SignCompiledTransactionIntent))
            }

            val compiledTransactionIntent = runCatching {
                transactionIntent.compile()
            }.getOrElse {
                return Result.failure(DappRequestException(DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction))
            }

            val signatures = collectSignersSignaturesUseCase(
                signers = signers,
                signRequest = SignRequest.SignTransactionRequest(
                    dataToSign = compiledTransactionIntent.toByteArray(),
                    hashedDataToSign = transactionIntentHash.bytes().toByteArray()
                ),
                deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
            ).getOrElse {
                return Result.failure(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.SignCompiledTransactionIntent,
                        e = it
                    )
                )
            }

            val signedTransactionIntent = runCatching {
                SignedIntent(
                    intent = transactionIntent,
                    intentSignatures = signatures
                )
            }.getOrElse {
                return Result.failure(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.SignCompiledTransactionIntent,
                        e = it
                    )
                )
            }

            val signedIntentHash = runCatching {
                signedTransactionIntent.signedIntentHash()
            }.getOrElse { error ->
                return Result.failure(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction,
                        msg = error.message,
                        e = error
                    )
                )
            }

            val notarySignature = notaryAndSigners.signWithNotary(hashedData = signedIntentHash.bytes().toByteArray())
            val compiledNotarizedIntent = runCatching {
                NotarizedTransaction(
                    signedIntent = signedTransactionIntent,
                    notarySignature = notarySignature
                ).compile()
            }.getOrElse { e ->
                return Result.failure(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction,
                        msg = e.message,
                        e = e
                    )
                )
            }
            val txId = transactionIntentHash.bytes().toByteArray().toHexString()
            Result.success(NotarizedTransactionResult(txId, compiledNotarizedIntent.toByteArray().toHexString()))
        }.onFailure {
            logger.w(it)
        }
    }

    suspend fun signAndSubmitTransaction(
        request: TransactionApprovalRequest,
        lockFee: BigDecimal,
        tipPercentage: UShort,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ): Result<String> {
        return prepareSignedTransactionIntent(
            request = request,
            lockFee = lockFee,
            tipPercentage = tipPercentage,
            deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
        ).mapCatching { notarizedTransactionResult ->
            submitTransactionUseCase(
                notarizedTransactionResult.txIdHex,
                notarizedTransactionResult.notarizedTransactionIntentHex
            ).getOrThrow()
        }
    }

    @Suppress("UnusedPrivateMember")
    /**
     * this might be handy in case of further signing changes
     */
    private fun printDebug(compiledNotarizedTransactionIntent: List<UByte>) {
        val decompiled = NotarizedTransaction.decompile(compiledNotarizedTransaction = compiledNotarizedTransactionIntent)
        val signedIntent = decompiled.signedIntent()
        val signatures = signedIntent.intentSignatures().map { it as SignatureWithPublicKey.Ed25519 }
        val intent = signedIntent.intent()

        logger.d("================= Transaction")
        logger.d("== Header:")
        logger.d(intent.header().toPrettyString())
        logger.d("== Manifest:")
        logger.d(intent.manifest().toPrettyString())
        logger.d("== Signatures:")
        signatures.forEach { signature ->
            logger.d("Public key: ${signature.publicKey}, signature: ${signature.signature}")
        }
        logger.d("Notary Signature: ${(decompiled.notarySignature() as Signature.Ed25519)}")
        logger.d("== Compiled tx intent: ${intent.compile()}")
        logger.d("== Compiled Notarized tx intent: ${compiledNotarizedTransactionIntent.toByteArray().toHexString()}")
    }

    suspend fun findFeePayerInManifest(manifest: TransactionManifest, lockFee: BigDecimal): Result<FeePayerSearchResult> {
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork()

        // 1. accountsWithdrawnFrom
        findFeePayerCandidatesWithinOwnedAccounts(
            entityAddress = manifest.accountsWithdrawnFrom(),
            ownedAccounts = allAccounts
        ).let {
            val withdrawnFromCandidate = findFeePayerWithFundsWithin(accounts = it, lockFee = lockFee)
            if (withdrawnFromCandidate != null) {
                return Result.success(
                    FeePayerSearchResult(
                        feePayerAddressFromManifest = withdrawnFromCandidate,
                        candidates = allAccounts
                    )
                )
            }
        }

        // 2. accountsDepositedInto
        findFeePayerCandidatesWithinOwnedAccounts(
            entityAddress = manifest.accountsDepositedInto(),
            ownedAccounts = allAccounts
        ).let {
            val depositedIntoCandidate = findFeePayerWithFundsWithin(accounts = it, lockFee = lockFee)
            if (depositedIntoCandidate != null) {
                return Result.success(
                    FeePayerSearchResult(
                        feePayerAddressFromManifest = depositedIntoCandidate,
                        candidates = allAccounts
                    )
                )
            }
        }

        // 3. accountsRequiringAuth
        findFeePayerCandidatesWithinOwnedAccounts(
            entityAddress = manifest.accountsRequiringAuth(),
            ownedAccounts = allAccounts
        ).let {
            val requiringAuthCandidate = findFeePayerWithFundsWithin(accounts = it, lockFee = lockFee)
            if (requiringAuthCandidate != null) {
                return Result.success(
                    FeePayerSearchResult(
                        feePayerAddressFromManifest = requiringAuthCandidate,
                        candidates = allAccounts
                    )
                )
            }
        }

        return Result.success(
            FeePayerSearchResult(
                candidates = allAccounts
            )
        )
    }

    private fun findFeePayerCandidatesWithinOwnedAccounts(
        entityAddress: List<Address>,
        ownedAccounts: List<Network.Account>
    ): List<Network.Account> = entityAddress.mapNotNull { address ->
        ownedAccounts.find { it.address == address.addressString() }
    }

    private suspend fun findFeePayerWithFundsWithin(
        accounts: List<Network.Account>,
        lockFee: BigDecimal
    ): String? {
        return getAccountsWithResourcesUseCase(accounts = accounts, isRefreshing = true)
            .value()?.findAccountWithEnoughXRDBalance(lockFee)?.account?.address
    }

    private suspend fun buildTransactionHeader(
        networkId: Int,
        notaryAndSigners: NotaryAndSigners,
        tipPercentage: UShort,
    ): Result<TransactionHeader> {
        val epochResult = transactionRepository.getLedgerEpoch()
        if (epochResult is ResultInternal.Success) {
            val epoch = epochResult.data
            return try {
                Result.success(
                    TransactionHeader(
                        networkId = networkId.toUByte(),
                        startEpochInclusive = epoch.toULong(),
                        endEpochExclusive = epoch.toULong() + TransactionConfig.EPOCH_WINDOW,
                        nonce = generateNonce(),
                        notaryPublicKey = notaryAndSigners.notaryPublicKey(),
                        notaryIsSignatory = notaryAndSigners.notaryIsSignatory,
                        tipPercentage = tipPercentage
                    )
                )
            } catch (e: Exception) {
                Result.failure(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.BuildTransactionHeader,
                        e.message,
                        e
                    )
                )
            }
        } else {
            return Result.failure(DappRequestException(DappRequestFailure.GetEpoch))
        }
    }

    suspend fun getSigningEntities(manifest: TransactionManifest): List<Entity> {
        val manifestAccountsRequiringAuth = manifest.accountsRequiringAuth().map { it.addressString() }
        val manifestIdentitiesRequiringAuth = manifest.identitiesRequiringAuth().map { it.addressString() }

        return getProfileUseCase.accountsOnCurrentNetwork().filter { account ->
            manifestAccountsRequiringAuth.contains(account.address)
        } + getProfileUseCase.personasOnCurrentNetwork().filter { account ->
            manifestIdentitiesRequiringAuth.contains(account.address)
        }
    }

    suspend fun getTransactionPreview(
        manifest: TransactionManifest,
        ephemeralNotaryPrivateKey: PrivateKey,
    ): Result<TransactionPreviewResponse> {
        val (startEpochInclusive, endEpochExclusive) = with(transactionRepository.getLedgerEpoch()) {
            val epoch = this.value() ?: return@with (0L to 0L)

            (epoch to epoch + 1L)
        }

        val notaryAndSigners = NotaryAndSigners(
            signers = getSigningEntities(manifest),
            ephemeralNotaryPrivateKey = ephemeralNotaryPrivateKey
        )

        return transactionRepository.getTransactionPreview(
            TransactionPreviewRequest(
                manifest = manifest.instructions().asStr(),
                startEpochInclusive = startEpochInclusive,
                endEpochExclusive = endEpochExclusive,
                tipPercentage = 0,
                nonce = generateNonce().toLong(),
                signerPublicKeys = notaryAndSigners.signersPublicKeys().map { it.asGatewayPublicKey() },
                flags = TransactionPreviewRequestFlags(
                    useFreeCredit = true,
                    assumeAllSignatureProofs = false,
                    skipEpochCheck = false
                ),
                blobsHex = manifest.blobs().map { it.toByteArray().toHexString() },
                notaryPublicKey = notaryAndSigners.notaryPublicKey().asGatewayPublicKey(),
                notaryIsSignatory = notaryAndSigners.notaryIsSignatory
            )
        ).asKotlinResult().fold(
            onSuccess = { preview ->
                if (preview.receipt.isFailed) {
                    Result.failure(Throwable(preview.receipt.errorMessage))
                } else {
                    Result.success(preview)
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    @Suppress("MagicNumber")
    private fun generateNonce(): UInt {
        val nonceBytes = ByteArray(UInt.SIZE_BYTES)
        SecureRandom().nextBytes(nonceBytes)
        var nonce: UInt = 0u
        nonceBytes.forEachIndexed { index, byte ->
            nonce = nonce or (byte.toUInt() shl 8 * index)
        }
        return nonce
    }
}

data class NotarizedTransactionResult(
    val txIdHex: String,
    val notarizedTransactionIntentHex: String
)

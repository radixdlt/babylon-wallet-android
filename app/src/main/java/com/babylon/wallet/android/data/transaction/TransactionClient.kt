@file:Suppress("TooGenericExceptionCaught", "ReturnCount", "LongMethod", "TooManyFunctions")

package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.gateway.generated.models.PublicKey
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEddsaEd25519
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyType
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
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.crypto.toECKeyPair
import com.radixdlt.extensions.removeLeadingZero
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
    val signingState = collectSignersSignaturesUseCase.signingState

    private val logger = Timber.tag("TransactionClient")

    suspend fun signAndSubmitTransaction(
        request: TransactionApprovalRequest
    ): Result<String> {
        val manifestWithTransactionFee = if (request.feePayerAddress == null) {
            request.manifest
        } else {
            request.manifest.addLockFeeInstructionToManifest(
                addressToLockFee = request.feePayerAddress,
                fee = TransactionConfig.DEFAULT_LOCK_FEE.toBigDecimal()
            )
        }

        val signers = getSigningEntities(manifestWithTransactionFee)
        val notaryAndSigners = NotaryAndSigners(signers, request.ephemeralNotaryPrivateKey)
        return buildTransactionHeader(request.networkId.value, notaryAndSigners).then { header ->
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
                )
            ).getOrElse {
                return Result.failure(DappRequestException(DappRequestFailure.TransactionApprovalFailure.SignCompiledTransactionIntent))
            }

            val signedTransactionIntent = runCatching {
                SignedIntent(
                    intent = transactionIntent,
                    intentSignatures = signatures
                )
            }.getOrElse {
                return Result.failure(DappRequestException(DappRequestFailure.TransactionApprovalFailure.SignCompiledTransactionIntent))
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

            submitTransactionUseCase(
                transactionIntentHash.bytes().toByteArray().toHexString(),
                compiledNotarizedIntent.toByteArray().toHexString()
            )
        }.onFailure {
            logger.w(it)
        }
    }

    fun cancelSigning() {
        collectSignersSignaturesUseCase.cancel()
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

    suspend fun findFeePayerInManifest(manifest: TransactionManifest): Result<FeePayerSearchResult> {
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork()

        val searchedAccounts = mutableSetOf<Network.Account>().apply {
            addAll(
                findFeePayerCandidatesWithinOwnedAccounts(
                    entityAddress = manifest.accountsWithdrawnFrom(),
                    ownedAccounts = allAccounts
                ).also {
                    val withdrawnFromCandidate = findFeePayerWithFundsWithin(it)
                    if (withdrawnFromCandidate != null) {
                        return Result.success(FeePayerSearchResult(feePayerAddressFromManifest = withdrawnFromCandidate))
                    }
                }
            )
            addAll(
                findFeePayerCandidatesWithinOwnedAccounts(
                    entityAddress = manifest.accountsRequiringAuth(),
                    ownedAccounts = allAccounts
                ).also {
                    val requiringAuthCandidate = findFeePayerWithFundsWithin(it)
                    if (requiringAuthCandidate != null) {
                        return Result.success(FeePayerSearchResult(feePayerAddressFromManifest = requiringAuthCandidate))
                    }
                }
            )
            addAll(
                findFeePayerCandidatesWithinOwnedAccounts(
                    entityAddress = manifest.accountsDepositedInto(),
                    ownedAccounts = allAccounts
                ).also {
                    val depositedIntoCandidate = findFeePayerWithFundsWithin(it)
                    if (depositedIntoCandidate != null) {
                        return Result.success(FeePayerSearchResult(feePayerAddressFromManifest = depositedIntoCandidate))
                    }
                }
            )
        }
        val candidatesWithinOwnAccounts = findFeePayerCandidatesWithinOwnedAccounts(allAccounts.minus(searchedAccounts))
        return if (candidatesWithinOwnAccounts.isEmpty()) {
            Result.failure(DappRequestFailure.TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee)
        } else {
            Result.success(FeePayerSearchResult(candidates = candidatesWithinOwnAccounts))
        }
    }

    private fun findFeePayerCandidatesWithinOwnedAccounts(
        entityAddress: List<Address>,
        ownedAccounts: List<Network.Account>
    ): List<Network.Account> = entityAddress.mapNotNull { address ->
        ownedAccounts.find { it.address == address.addressString() }
    }

    private suspend fun findFeePayerWithFundsWithin(accounts: List<Network.Account>): String? {
        return getAccountsWithResourcesUseCase(accounts = accounts, isRefreshing = true)
            .value()?.findAccountWithEnoughXRDBalance(TransactionConfig.DEFAULT_LOCK_FEE)?.account?.address
    }

    private suspend fun findFeePayerCandidatesWithinOwnedAccounts(accounts: List<Network.Account>): List<Network.Account> {
        return getAccountsWithResourcesUseCase(accounts = accounts, isRefreshing = true)
            .value()?.filter { it.resources?.hasXrd(TransactionConfig.DEFAULT_LOCK_FEE) == true }?.map { it.account }.orEmpty()
    }

    private suspend fun buildTransactionHeader(
        networkId: Int,
        notaryAndSigners: NotaryAndSigners,
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
                        tipPercentage = TransactionConfig.TIP_PERCENTAGE
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
        val notaryPrivateKey = notaryAndSigners.notaryPrivateKeySLIP10()
        val notaryPublicKey: PublicKey = PublicKeyEddsaEd25519(
            keyType = PublicKeyType.eddsaEd25519,
            keyHex = notaryPrivateKey.toECKeyPair().getCompressedPublicKey().removeLeadingZero().toHexString()
        )
        return transactionRepository.getTransactionPreview(
            // TODO things like tipPercentage might change later on
            TransactionPreviewRequest(
                manifest = manifest.instructions().asStr(),
                startEpochInclusive = startEpochInclusive,
                endEpochExclusive = endEpochExclusive,
                tipPercentage = 0,
                nonce = generateNonce().toLong(),
                signerPublicKeys = listOf(),
                flags = TransactionPreviewRequestFlags(
                    useFreeCredit = false,
                    assumeAllSignatureProofs = false,
                    skipEpochCheck = false
                ),
                blobsHex = manifest.blobs().map { it.toByteArray().toHexString() },
                notaryPublicKey = notaryPublicKey,
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

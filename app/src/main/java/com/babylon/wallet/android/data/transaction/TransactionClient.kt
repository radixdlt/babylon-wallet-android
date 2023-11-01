@file:Suppress("TooGenericExceptionCaught", "ReturnCount", "LongMethod", "TooManyFunctions")

package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.gateway.extensions.asGatewayPublicKey
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequestFlags
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.data.transaction.model.TransactionApprovalRequest
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.assets.findAccountWithEnoughXRDBalance
import com.babylon.wallet.android.domain.usecases.GetAccountsWithAssetsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.ret.Address
import com.radixdlt.ret.Intent
import com.radixdlt.ret.NotarizedTransaction
import com.radixdlt.ret.SignedIntent
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.decodeHex
import rdx.works.core.ret.crypto.PrivateKey
import rdx.works.core.then
import rdx.works.core.toByteArray
import rdx.works.core.toUByteList
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.personasOnCurrentNetwork
import timber.log.Timber
import java.math.BigDecimal
import java.security.SecureRandom
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase,
    private val getAccountsWithAssetsUseCase: GetAccountsWithAssetsUseCase
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

        val notaryAndSigners = getNotaryAndSigners(
            manifest = manifestWithTransactionFee,
            ephemeralNotaryPrivateKey = request.ephemeralNotaryPrivateKey
        )
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
                    dataToSign = compiledTransactionIntent.toByteArray(),
                    hashedDataToSign = transactionIntentHash.bytes().toByteArray()
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
                    intentSignatures = signatures
                )
            }.getOrElse {
                return Result.failure(RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent(it))
            }

            val signedIntentHash = runCatching {
                signedTransactionIntent.signedIntentHash()
            }.getOrElse { error ->
                return Result.failure(RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction(error))
            }

            val notarySignature = notaryAndSigners.signWithNotary(hashedData = signedIntentHash.bytes().toByteArray())
            val compiledNotarizedIntent = runCatching {
                NotarizedTransaction(
                    signedIntent = signedTransactionIntent,
                    notarySignature = notarySignature
                ).compile()
            }.getOrElse { e ->
                return Result.failure(RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction(e))
            }
            Result.success(
                NotarizedTransactionResult(
                    txIdHash = transactionIntentHash.asStr(),
                    notarizedTransactionIntentHex = compiledNotarizedIntent.toByteArray().toHexString(),
                    transactionHeader = header
                )
            )
        }.onFailure {
            logger.w(it)
        }
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

    suspend fun findFeePayerInManifest(manifest: TransactionManifest, lockFee: BigDecimal): Result<FeePayerSearchResult> {
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork()
        val allAccountsWithResources = getAccountsWithAssetsUseCase(
            accounts = allAccounts,
            isRefreshing = false
        ).getOrNull()
        val candidates = allAccountsWithResources?.map {
            FeePayerSearchResult.FeePayerCandidate(
                account = it.account,
                xrdAmount = it.assets?.xrd?.ownedAmount ?: BigDecimal.ZERO
            )
        }.orEmpty()

        // 1. accountsWithdrawnFrom
        findFeePayerCandidatesWithinOwnedAccounts(
            entityAddress = manifest.accountsWithdrawnFrom(),
            ownedAccounts = allAccounts
        ).let {
            val withdrawnFromCandidate = findFeePayerWithFundsWithin(accounts = it, lockFee = lockFee)
            if (withdrawnFromCandidate != null) {
                return Result.success(
                    FeePayerSearchResult(
                        feePayerAddress = withdrawnFromCandidate,
                        candidates = candidates
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
                        feePayerAddress = depositedIntoCandidate,
                        candidates = candidates
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
                        feePayerAddress = requiringAuthCandidate,
                        candidates = candidates
                    )
                )
            }
        }

        return Result.success(
            FeePayerSearchResult(
                candidates = candidates
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
        return getAccountsWithAssetsUseCase(accounts = accounts, isRefreshing = true)
            .getOrNull()?.findAccountWithEnoughXRDBalance(lockFee)?.account?.address
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
                        nonce = generateNonce(),
                        notaryPublicKey = notaryAndSigners.notaryPublicKey(),
                        notaryIsSignatory = notaryAndSigners.notaryIsSignatory,
                        tipPercentage = tipPercentage
                    )
                )
            } catch (e: Exception) {
                Result.failure(RadixWalletException.PrepareTransactionException.BuildTransactionHeader(cause = e))
            }
        } ?: Result.failure(RadixWalletException.DappRequestException.GetEpoch)
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

    suspend fun getNotaryAndSigners(
        manifest: TransactionManifest,
        ephemeralNotaryPrivateKey: PrivateKey
    ): NotaryAndSigners {
        return NotaryAndSigners(
            signers = getSigningEntities(manifest),
            ephemeralNotaryPrivateKey = ephemeralNotaryPrivateKey
        )
    }

    suspend fun getTransactionPreview(
        manifest: TransactionManifest,
        notaryAndSigners: NotaryAndSigners
    ): Result<TransactionPreviewResponse> {
        val (startEpochInclusive, endEpochExclusive) = with(transactionRepository.getLedgerEpoch()) {
            val epoch = this.getOrNull() ?: return@with (0L to 0L)

            (epoch to epoch + 1L)
        }

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
        ).fold(
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

    fun analyzeExecution(
        manifest: TransactionManifest,
        preview: TransactionPreviewResponse
    ) = runCatching {
        manifest.analyzeExecution(transactionReceipt = preview.encodedReceipt.decodeHex().toUByteList())
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
    val txIdHash: String,
    val notarizedTransactionIntentHex: String,
    val transactionHeader: TransactionHeader
) {
    val txProcessingTime: String
        get() = ((transactionHeader.endEpochExclusive - transactionHeader.startEpochInclusive) * 5u).toString()
}

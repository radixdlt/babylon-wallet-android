@file:Suppress("TooGenericExceptionCaught", "ReturnCount", "LongMethod", "TooManyFunctions")

package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.gateway.generated.models.PublicKey
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEddsaEd25519
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyType
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequestFlags
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.manifest.convertManifestInstructionsToString
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.TransactionConfig.COST_UNIT_LIMIT
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.data.transaction.model.TransactionApprovalRequest
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
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.crypto.PrivateKey
import com.radixdlt.toolkit.models.crypto.Signature
import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import com.radixdlt.toolkit.models.request.AnalyzeTransactionExecutionRequest
import com.radixdlt.toolkit.models.request.AnalyzeTransactionExecutionResponse
import com.radixdlt.toolkit.models.request.CompileNotarizedTransactionRequest
import com.radixdlt.toolkit.models.request.CompileTransactionIntentRequest
import com.radixdlt.toolkit.models.request.ConvertManifestRequest
import com.radixdlt.toolkit.models.request.ConvertManifestResponse
import com.radixdlt.toolkit.models.request.DecompileNotarizedTransactionRequest
import com.radixdlt.toolkit.models.request.ExtractAddressesFromManifestRequest
import com.radixdlt.toolkit.models.transaction.ManifestInstructionsKind
import com.radixdlt.toolkit.models.transaction.SignedTransactionIntent
import com.radixdlt.toolkit.models.transaction.TransactionHeader
import com.radixdlt.toolkit.models.transaction.TransactionIntent
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SigningEntity
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.domain.personaOnCurrentNetwork
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.Result
import com.babylon.wallet.android.domain.common.Result as ResultInternal

@Suppress("LongParameterList")
class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase,
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val submitTransactionUseCase: SubmitTransactionUseCase
) {

    private val engine = RadixEngineToolkit
    val signingState = collectSignersSignaturesUseCase.signingEvent

    suspend fun signAndSubmitTransaction(request: TransactionApprovalRequest): Result<String> {
        val networkId = getCurrentGatewayUseCase().network.networkId().value
        return signAndSubmitTransaction(
            request.manifest,
            request.ephemeralNotaryPrivateKey,
            networkId,
            request.feePayerAddress
        )
    }

    suspend fun manifestInStringFormat(manifest: TransactionManifest): Result<TransactionManifest> {
        val networkId = getCurrentGatewayUseCase().network.networkId()
        val manifestConversionResult = manifest.convertManifestInstructionsToString(
            networkId = networkId.value
        )
        return manifestConversionResult.map { response ->
            response
        }
    }

    private suspend fun signAndSubmitTransaction(
        jsonTransactionManifest: TransactionManifest,
        ephemeralNotaryPrivateKey: PrivateKey,
        networkId: Int,
        feePayerAddress: String?,
    ): Result<String> {
        val manifestWithTransactionFee = if (feePayerAddress == null) {
            jsonTransactionManifest
        } else {
            jsonTransactionManifest.addLockFeeInstructionToManifest(feePayerAddress)
        }
        Timber.d("Approving: \n${Json.encodeToString(manifestWithTransactionFee)}")
        val signers = getSigningEntities(networkId, manifestWithTransactionFee)
        val notaryAndSigners = NotaryAndSigners(signers, ephemeralNotaryPrivateKey)
        return buildTransactionHeader(networkId, notaryAndSigners).map { header ->
            val compileTransactionIntentRequest = CompileTransactionIntentRequest(
                header,
                manifestWithTransactionFee
            )
            val txId = compileTransactionIntentRequest.transactionId().getOrNull() ?: return Result.failure(
                DappRequestException(
                    DappRequestFailure.TransactionApprovalFailure.CompileTransactionIntent
                )
            )
            val compiledTransactionIntent = engine.compileTransactionIntent(
                compileTransactionIntentRequest
            ).getOrNull()?.compiledIntent ?: return Result.failure(
                DappRequestException(
                    DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction
                )
            )
            val signaturesResult = collectSignersSignaturesUseCase(
                signers = signers,
                signRequest = SignRequest.SignTransactionRequest(compiledTransactionIntent)
            )
            if (signaturesResult.isFailure) {
                return Result.failure(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.SignCompiledTransactionIntent
                    )
                )
            }
            val signedTransactionIntent = SignedTransactionIntent(
                TransactionIntent(
                    header = header,
                    manifest = manifestWithTransactionFee
                ),
                intentSignatures = signaturesResult.getOrThrow().toTypedArray()
            )
            val signedCompiledTransactionIntent = signedTransactionIntent.compile().getOrNull() ?: return Result.failure(
                DappRequestException(
                    DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction
                )
            )
            val compiledNotarizedIntent =
                engine.compileNotarizedTransaction(
                    CompileNotarizedTransactionRequest(
                        signedIntent = signedTransactionIntent,
                        notarySignature = notaryAndSigners.signWithNotary(signedCompiledTransactionIntent)
                    )
                ).getOrElse { e ->
                    return Result.failure(
                        DappRequestException(
                            DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction,
                            msg = e.message,
                            e = e
                        )
                    )
                }.compiledNotarizedIntent
            val submitResult = submitTransactionUseCase(
                txId.toHexString(),
                compiledNotarizedIntent
            )
            if (submitResult.isFailure) {
                return Result.failure(
                    submitResult.exceptionOrNull() ?: DappRequestFailure.TransactionApprovalFailure.SubmitNotarizedTransaction
                )
            }
            submitResult.getOrThrow()
        }
    }

    @Suppress("UnusedPrivateMember")
    /**
     * this might be handy in case of further signing changes
     */
    private fun printDebug(compiledNotarizedTransactionIntent: ByteArray) {
        val decompiled = engine.decompileNotarizedTransaction(
            DecompileNotarizedTransactionRequest(
                ManifestInstructionsKind.Parsed,
                compiledNotarizedTransactionIntent
            )
        ).getOrThrow()
        val signedIntent = decompiled.signedIntent
        val signatures = signedIntent.intentSignatures.map { it as SignatureWithPublicKey.EddsaEd25519 }
        val intent = signedIntent.intent
        Timber.d("Debug Transaction")
        Timber.d("TXID: ${decompiled.transactionId().getOrThrow().toHexString()}")
        Timber.d("Transaction Intent: $intent")
        Timber.d("Signatures --------")
        signatures.forEach { signature ->
            Timber.d("Public key: ${signature.publicKey()}, signature: ${signature.signature()}")
        }
        Timber.d("Notary Signature: ${(decompiled.notarySignature as Signature.EddsaEd25519)}")
        Timber.d("Compiled tx intent: ${intent.compile().getOrThrow()}")
        Timber.d("Compiled Notarized tx intent: ${compiledNotarizedTransactionIntent.toHexString()}")
    }

    suspend fun findFeePayerInManifest(manifestJson: TransactionManifest): Result<FeePayerSearchResult> {
        val networkId = getCurrentGatewayUseCase().network.networkId().value
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork()
        val result = engine.extractAddressesFromManifest(ExtractAddressesFromManifestRequest(networkId.toUByte(), manifestJson))
        val searchedAccounts = mutableSetOf<Network.Account>()
        return if (result.isSuccess) {
            val analyzeManifestResponse = result.getOrThrow()
            val withdrawnFromCandidates = findFeePayerCandidatesWithinOwnedAccounts(
                entityAddress = analyzeManifestResponse.accountsWithdrawnFrom.toList(),
                ownedAccounts = allAccounts
            )
            searchedAccounts.addAll(withdrawnFromCandidates)
            val withdrawnFromCandidate = findFeePayerWithFundsWithin(withdrawnFromCandidates)
            if (withdrawnFromCandidate != null) return Result.success(FeePayerSearchResult(withdrawnFromCandidate))

            val requiringAuthCandidates = findFeePayerCandidatesWithinOwnedAccounts(
                entityAddress = analyzeManifestResponse.accountsRequiringAuth.toList(),
                ownedAccounts = allAccounts
            )
            searchedAccounts.addAll(requiringAuthCandidates)
            val requiringAuthCandidate = findFeePayerWithFundsWithin(requiringAuthCandidates)
            if (requiringAuthCandidate != null) return Result.success(FeePayerSearchResult(requiringAuthCandidate))

            val depositedIntoCandidates = findFeePayerCandidatesWithinOwnedAccounts(
                entityAddress = analyzeManifestResponse.accountsDepositedInto.toList(),
                ownedAccounts = allAccounts
            )
            searchedAccounts.addAll(depositedIntoCandidates)
            val depositedIntoCandidate = findFeePayerWithFundsWithin(depositedIntoCandidates)
            if (depositedIntoCandidate != null) return Result.success(FeePayerSearchResult(depositedIntoCandidate))

            val accountsLeftToSearch = allAccounts.minus(searchedAccounts)
            val candidatesWithinOwnAccounts = findFeePayerCandidatesWithinOwnedAccounts(accountsLeftToSearch.toList())
            if (candidatesWithinOwnAccounts.isEmpty()) {
                Result.failure(DappRequestFailure.TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee)
            } else {
                Result.success(FeePayerSearchResult(candidates = candidatesWithinOwnAccounts))
            }
        } else {
            Result.failure(
                result.exceptionOrNull() ?: DappRequestFailure.TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee
            )
        }
    }

    private fun findFeePayerCandidatesWithinOwnedAccounts(
        entityAddress: List<String>,
        ownedAccounts: List<Network.Account>
    ): List<Network.Account> {
        return entityAddress
            .mapNotNull { address ->
                ownedAccounts.find { it.address == address }
            }
    }

    private suspend fun findFeePayerWithFundsWithin(accounts: List<Network.Account>): String? {
        return getAccountsWithResourcesUseCase(accounts = accounts, isRefreshing = true)
            .value()?.findAccountWithEnoughXRDBalance(TransactionConfig.DEFAULT_LOCK_FEE)?.account?.address
    }

    private suspend fun findFeePayerCandidatesWithinOwnedAccounts(accounts: List<Network.Account>): List<Network.Account> {
        return getAccountsWithResourcesUseCase(accounts = accounts, isRefreshing = true)
            .value()?.filter { it.hasXrd(TransactionConfig.DEFAULT_LOCK_FEE) }?.map { it.account }.orEmpty()
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
                        version = TransactionVersion.Default.value.toUByte(),
                        networkId = networkId.toUByte(),
                        startEpochInclusive = epoch.toULong(),
                        endEpochExclusive = epoch.toULong() + TransactionConfig.EPOCH_WINDOW,
                        nonce = generateNonce(),
                        notaryPublicKey = notaryAndSigners.notaryPublicKey(),
                        notaryAsSignatory = notaryAndSigners.notaryAsSignatory,
                        costUnitLimit = COST_UNIT_LIMIT,
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

    suspend fun convertManifestInstructionsToJSON(
        manifest: TransactionManifest
    ): Result<ConvertManifestResponse> {
        val networkId = getCurrentGatewayUseCase().network.networkId()
        return try {
            Result.success(
                engine.convertManifest(
                    ConvertManifestRequest(
                        networkId = networkId.value.toUByte(),
                        instructionsOutputKind = ManifestInstructionsKind.Parsed,
                        manifest = manifest
                    )
                ).getOrThrow()
            )
        } catch (e: Exception) {
            Result.failure(
                DappRequestException(
                    failure = DappRequestFailure.TransactionApprovalFailure.ConvertManifest,
                    msg = e.message,
                    e = e
                )
            )
        }
    }

    suspend fun getSigningEntities(networkId: Int, manifestJson: TransactionManifest): List<SigningEntity> {
        val result = engine.extractAddressesFromManifest(ExtractAddressesFromManifestRequest(networkId.toUByte(), manifestJson))
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork()
        return result.getOrNull()?.let { analyzeManifestResponse ->
            val accountsNeededToSign = analyzeManifestResponse.accountsRequiringAuth.toSet()
            val identitiesNeededToSign = analyzeManifestResponse.identitiesRequiringAuth.toSet()
            allAccounts.filter {
                accountsNeededToSign.contains(it.address)
            } + identitiesNeededToSign.mapNotNull { identityAddress ->
                getProfileUseCase.personaOnCurrentNetwork(identityAddress)
            }
        }.orEmpty()
    }

    suspend fun analyzeManifestWithPreviewContext(
        transactionManifest: TransactionManifest,
        transactionReceipt: ByteArray
    ): Result<AnalyzeTransactionExecutionResponse> {
        val networkId = getCurrentGatewayUseCase().network.networkId().value
        return engine.analyzeTransactionExecution(
            AnalyzeTransactionExecutionRequest(
                networkId = networkId.toUByte(),
                manifest = transactionManifest,
                transactionReceipt = transactionReceipt
            )
        )
    }

    suspend fun getTransactionPreview(
        manifest: TransactionManifest,
        ephemeralNotaryPrivateKey: PrivateKey,
        blobs: Array<out ByteArray>
    ): Result<TransactionPreviewResponse> {
        val networkId = getCurrentGatewayUseCase().network.networkId().value
        var startEpochInclusive = 0L
        var endEpochExclusive = 0L
        val epochResult = transactionRepository.getLedgerEpoch()
        if (epochResult is ResultInternal.Success) {
            val epoch = epochResult.data
            startEpochInclusive = epoch
            endEpochExclusive = epoch + 1L
        }

        val signingEntities = getSigningEntities(networkId, manifest)
        val notaryAndSigners = NotaryAndSigners(signingEntities, ephemeralNotaryPrivateKey)
        val notaryPrivateKey = notaryAndSigners.notaryPrivateKeySLIP10()
        val notaryPublicKey: PublicKey = PublicKeyEddsaEd25519(
            keyType = PublicKeyType.eddsaEd25519,
            keyHex = notaryPrivateKey.toECKeyPair().getCompressedPublicKey().removeLeadingZero().toHexString()
        )
        val previewResult = transactionRepository.getTransactionPreview(
            // TODO things like tipPercentage might change later on
            TransactionPreviewRequest(
                manifest = manifest.toStringWithoutBlobs(),
                startEpochInclusive = startEpochInclusive,
                endEpochExclusive = endEpochExclusive,
                costUnitLimit = COST_UNIT_LIMIT.toLong(),
                tipPercentage = 5,
                nonce = generateNonce().toString(),
                signerPublicKeys = listOf(),
                flags = TransactionPreviewRequestFlags(true, true, true, true),
                blobsHex = blobs.map { it.toHexString() },
                notaryPublicKey = notaryPublicKey,
                notaryAsSignatory = notaryAndSigners.notaryAsSignatory
            )
        )
        return when (val result = previewResult) {
            is ResultInternal.Error -> Result.failure(result.exception ?: DappRequestFailure.InvalidRequest)
            is ResultInternal.Success -> Result.success(result.data)
        }
    }

    @Suppress("MagicNumber")
    private fun generateNonce(): ULong {
        val random = SecureRandom()
        val nonceBytes = ByteArray(ULong.SIZE_BYTES)
        random.nextBytes(nonceBytes)
        var nonce: ULong = 0u
        nonceBytes.forEachIndexed { index, byte ->
            nonce = nonce or (byte.toULong() shl 8 * index)
        }
        return nonce
    }
}

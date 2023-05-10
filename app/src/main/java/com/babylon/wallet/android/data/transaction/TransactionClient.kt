@file:Suppress("TooGenericExceptionCaught", "ReturnCount", "LongMethod", "TooManyFunctions")

package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.gateway.generated.models.PublicKey
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEddsaEd25519
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyType
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequestFlags
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.TransactionConfig.COST_UNIT_LIMIT
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.findAccountWithEnoughXRDBalance
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.crypto.toECKeyPair
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.address.EntityAddress
import com.radixdlt.toolkit.models.crypto.PrivateKey
import com.radixdlt.toolkit.models.crypto.Signature
import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import com.radixdlt.toolkit.models.request.AnalyzeManifestRequest
import com.radixdlt.toolkit.models.request.AnalyzeManifestWithPreviewContextRequest
import com.radixdlt.toolkit.models.request.AnalyzeManifestWithPreviewContextResponse
import com.radixdlt.toolkit.models.request.CompileNotarizedTransactionRequest
import com.radixdlt.toolkit.models.request.CompileTransactionIntentRequest
import com.radixdlt.toolkit.models.request.ConvertManifestRequest
import com.radixdlt.toolkit.models.request.ConvertManifestResponse
import com.radixdlt.toolkit.models.request.DecompileNotarizedTransactionRequest
import com.radixdlt.toolkit.models.transaction.ManifestInstructionsKind
import com.radixdlt.toolkit.models.transaction.SignedTransactionIntent
import com.radixdlt.toolkit.models.transaction.TransactionHeader
import com.radixdlt.toolkit.models.transaction.TransactionIntent
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import rdx.works.profile.data.model.SigningEntity
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.domain.signing.GetFactorSourcesAndSigningEntitiesUseCase
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase,
    private val getFactorSourcesAndSigningEntitiesUseCase: GetFactorSourcesAndSigningEntitiesUseCase,
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val submitTransactionUseCase: SubmitTransactionUseCase
) {

    private val engine = RadixEngineToolkit
    val signingState = collectSignersSignaturesUseCase.signingEvent

    suspend fun signAndSubmitTransaction(request: TransactionApprovalRequest): Result<String> {
        val networkId = getCurrentGatewayUseCase().network.networkId().value
        return signAndSubmitTransaction(request.manifest, request.ephemeralNotaryPrivateKey, networkId, request.hasLockFee)
    }

    suspend fun manifestInStringFormat(manifest: TransactionManifest): Result<TransactionManifest> {
        val manifestConversionResult = convertManifestInstructionsToString(
            manifest = manifest
        )
        val stringManifest = when (manifestConversionResult) {
            is Result.Error -> return manifestConversionResult
            is Result.Success -> manifestConversionResult.data
        }
        return Result.Success(stringManifest)
    }

    private suspend fun signAndSubmitTransaction(
        jsonTransactionManifest: TransactionManifest,
        ephemeralNotaryPrivateKey: PrivateKey,
        networkId: Int,
        hasLockFeeInstruction: Boolean,
    ): Result<String> {
        val manifestWithTransactionFee = if (hasLockFeeInstruction) {
            jsonTransactionManifest
        } else {
            val accountAddressToLockFee = selectAccountAddressToLockFee(networkId, jsonTransactionManifest)
                ?: return Result.Error(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee
                    )
                )
            jsonTransactionManifest.addLockFeeInstructionToManifest(accountAddressToLockFee)
        }
        val signers = getSigningEntities(networkId, manifestWithTransactionFee)
        val signersPerFactorSource = getFactorSourcesAndSigningEntitiesUseCase(signers)
        val notaryAndSigners = NotaryAndSigners(signers, ephemeralNotaryPrivateKey)
        when (val transactionHeaderResult = buildTransactionHeader(networkId, notaryAndSigners)) {
            is Result.Error -> return transactionHeaderResult
            is Result.Success -> {
                val compileTransactionIntentRequest = CompileTransactionIntentRequest(
                    transactionHeaderResult.data,
                    manifestWithTransactionFee
                )
                val txId = compileTransactionIntentRequest.transactionId().getOrNull() ?: return Result.Error(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.CompileTransactionIntent
                    )
                )
                val compiledTransactionIntent = engine.compileTransactionIntent(
                    compileTransactionIntentRequest
                ).getOrNull()?.compiledIntent ?: return Result.Error(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction
                    )
                )
                val signaturesResult = collectSignersSignaturesUseCase(signersPerFactorSource, compiledTransactionIntent)
                if (signaturesResult.isFailure) {
                    return Result.Error(
                        DappRequestException(
                            DappRequestFailure.TransactionApprovalFailure.SignCompiledTransactionIntent
                        )
                    )
                }
                val signedTransactionIntent = SignedTransactionIntent(
                    TransactionIntent(
                        header = transactionHeaderResult.data,
                        manifest = manifestWithTransactionFee
                    ),
                    intentSignatures = signaturesResult.getOrThrow().toTypedArray()
                )
                val signedCompiledTransactionIntent = signedTransactionIntent.compile().getOrNull() ?: return Result.Error(
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
                        return Result.Error(
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
                return if (submitResult.isSuccess) {
                    Result.Success(submitResult.getOrThrow())
                } else {
                    Result.Error(submitResult.exceptionOrNull())
                }
            }
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

    suspend fun selectAccountAddressToLockFee(networkId: Int, manifestJson: TransactionManifest): String? {
        val allAccountsAddresses = getProfileUseCase.accountsOnCurrentNetwork().map { it.address }.toSet()
        val result = engine.analyzeManifest(AnalyzeManifestRequest(networkId.toUByte(), manifestJson))
        val searchedAddresses = mutableSetOf<String>()
        result.onSuccess { analyzeManifestResponse ->
            val withdrawnFromCandidates = findFeePayerCandidates(
                entityAddress = analyzeManifestResponse.accountsWithdrawnFrom.toList(),
                allNetworkAddresses = allAccountsAddresses
            )
            searchedAddresses.addAll(withdrawnFromCandidates)
            val withdrawnFromCandidate = findFeePayerWithin(withdrawnFromCandidates)
            if (withdrawnFromCandidate != null) return withdrawnFromCandidate

            val requiringAuthCandidates = findFeePayerCandidates(
                entityAddress = analyzeManifestResponse.accountsRequiringAuth.toList(),
                allNetworkAddresses = allAccountsAddresses
            )
            searchedAddresses.addAll(requiringAuthCandidates)
            val requiringAuthCandidate = findFeePayerWithin(requiringAuthCandidates)
            if (requiringAuthCandidate != null) return requiringAuthCandidate

            val depositedIntoCandidates = findFeePayerCandidates(
                entityAddress = analyzeManifestResponse.accountsDepositedInto.toList(),
                allNetworkAddresses = allAccountsAddresses
            )
            searchedAddresses.addAll(depositedIntoCandidates)
            val depositedIntoCandidate = findFeePayerWithin(depositedIntoCandidates)
            if (depositedIntoCandidate != null) return depositedIntoCandidate

            val accountsLeftToSearch = allAccountsAddresses.minus(searchedAddresses)
            return findFeePayerWithin(accountsLeftToSearch.toList())
        }
        return null
    }

    private fun findFeePayerCandidates(entityAddress: List<EntityAddress>, allNetworkAddresses: Set<String>): List<String> {
        return entityAddress
            .filterIsInstance<EntityAddress.ComponentAddress>()
            .filter { allNetworkAddresses.contains(it.address) }
            .map { it.address }
    }

    private suspend fun findFeePayerWithin(addresses: List<String>): String? {
        return getAccountsWithResourcesUseCase
            .invoke(
                accounts = getProfileUseCase.accountsOnCurrentNetwork().filter { addresses.contains(it.address) },
                isRefreshing = true
            )
            .value()
            ?.findAccountWithEnoughXRDBalance(TransactionConfig.DEFAULT_LOCK_FEE)
            ?.account
            ?.address
    }

    private suspend fun buildTransactionHeader(
        networkId: Int,
        notaryAndSigners: NotaryAndSigners,
    ): Result<TransactionHeader> {
        val epochResult = transactionRepository.getLedgerEpoch()
        if (epochResult is Result.Success) {
            val epoch = epochResult.data
            return try {
                Result.Success(
                    TransactionHeader(
                        version = TransactionVersion.Default.value.toUByte(),
                        networkId = networkId.toUByte(),
                        startEpochInclusive = epoch.toULong(),
                        endEpochExclusive = epoch.toULong() + TransactionConfig.EPOCH_WINDOW,
                        nonce = generateNonce(),
                        notaryPublicKey = notaryAndSigners.notaryPublicKey(),
                        notaryAsSignatory = notaryAndSigners.notaryAsSignatory,
                        costUnitLimit = TransactionConfig.COST_UNIT_LIMIT,
                        tipPercentage = TransactionConfig.TIP_PERCENTAGE
                    )
                )
            } catch (e: Exception) {
                Result.Error(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.BuildTransactionHeader,
                        e.message,
                        e
                    )
                )
            }
        } else {
            return Result.Error(DappRequestException(DappRequestFailure.GetEpoch))
        }
    }

    suspend fun convertManifestInstructionsToJSON(
        manifest: TransactionManifest
    ): Result<ConvertManifestResponse> {
        val networkId = getCurrentGatewayUseCase().network.networkId()
        return try {
            Result.Success(
                engine.convertManifest(
                    ConvertManifestRequest(
                        networkId = networkId.value.toUByte(),
                        instructionsOutputKind = ManifestInstructionsKind.Parsed,
                        manifest = manifest
                    )
                ).getOrThrow()
            )
        } catch (e: Exception) {
            Result.Error(
                DappRequestException(
                    failure = DappRequestFailure.TransactionApprovalFailure.ConvertManifest,
                    msg = e.message,
                    e = e
                )
            )
        }
    }

    suspend fun convertManifestInstructionsToString(
        manifest: TransactionManifest,
    ): Result<ConvertManifestResponse> {
        val networkId = getCurrentGatewayUseCase().network.networkId()
        return try {
            Result.Success(
                engine.convertManifest(
                    ConvertManifestRequest(
                        networkId = networkId.value.toUByte(),
                        instructionsOutputKind = ManifestInstructionsKind.String,
                        manifest = manifest
                    )
                ).getOrThrow()
            )
        } catch (e: Exception) {
            Result.Error(
                DappRequestException(
                    DappRequestFailure.TransactionApprovalFailure.ConvertManifest,
                    e.message,
                    e
                )
            )
        }
    }

    suspend fun getSigningEntities(networkId: Int, manifestJson: TransactionManifest): List<SigningEntity> {
        val result = engine.analyzeManifest(AnalyzeManifestRequest(networkId.toUByte(), manifestJson))
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork()
        result.onSuccess { analyzeManifestResponse ->
            val addressesNeededToSign = analyzeManifestResponse.accountsRequiringAuth
                .filterIsInstance<EntityAddress.ComponentAddress>()
                .map { it.address }.toSet()
            return allAccounts.filter { addressesNeededToSign.contains(it.address) }
        }
        return emptyList()
    }

    fun analyzeManifestWithPreviewContext(
        networkId: NetworkId,
        transactionManifest: TransactionManifest,
        transactionReceipt: ByteArray
    ): kotlin.Result<AnalyzeManifestWithPreviewContextResponse> {
        return engine.analyzeManifestWithPreviewContext(
            AnalyzeManifestWithPreviewContextRequest(
                networkId = networkId.value.toUByte(),
                manifest = transactionManifest,
                transactionReceipt = transactionReceipt
            )
        )
    }

    suspend fun getTransactionPreview(
        manifest: TransactionManifest,
        ephemeralNotaryPrivateKey: PrivateKey,
        networkId: Int,
        blobs: Array<out ByteArray>
    ): Result<TransactionPreviewResponse> {
        var startEpochInclusive = 0L
        var endEpochExclusive = 0L
        val epochResult = transactionRepository.getLedgerEpoch()
        if (epochResult is Result.Success) {
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
        return transactionRepository.getTransactionPreview(
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

data class TransactionApprovalRequest(
    val manifest: TransactionManifest,
    val hasLockFee: Boolean = false,
    val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom()
)

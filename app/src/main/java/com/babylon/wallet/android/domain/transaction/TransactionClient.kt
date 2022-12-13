package com.babylon.wallet.android.domain.transaction

import RadixEngineToolkit
import builders.TransactionBuilder
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatus
import com.babylon.wallet.android.data.gateway.isComplete
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.common.Result
import com.radixdlt.hex.extensions.toHexString
import kotlinx.coroutines.delay
import models.Instruction
import models.Value
import models.request.CompileNotarizedTransactionIntentResponse
import models.request.ConvertManifestRequest
import models.request.ConvertManifestResponse
import models.transaction.ManifestInstructions
import models.transaction.ManifestInstructionsKind
import models.transaction.TransactionHeader
import models.transaction.TransactionManifest
import rdx.works.peerdroid.helpers.sha256
import rdx.works.profile.data.repository.ProfileRepository
import java.math.BigDecimal
import java.security.SecureRandom
import javax.inject.Inject

class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val engine: RadixEngineToolkit
) {

    suspend fun signAndSubmitTransaction(manifest: TransactionManifest): Result<String> {
        val networkId = profileRepository.getCurrentNetworkId()
        when (val transactionHeaderResult = buildTransactionHeader(networkId, manifest)) {
            is Result.Error -> return transactionHeaderResult
            is Result.Success -> {
                val manifestWithTransactionFee = addLockFeeInstructionToManifest(manifest)
                val notaryAndSigners = getNotaryAndSigners(networkId, manifestWithTransactionFee)
                val notarizedTransactionBuilder =
                    TransactionBuilder().manifest(manifestWithTransactionFee).header(transactionHeaderResult.data)
                val notarizedTransaction =
                    notarizedTransactionBuilder.notarize(notaryAndSigners.notarySigner.notaryPrivateKey.toEngineModel())
                val compiledNotarizedTransaction = notarizedTransaction.compile()
                val txID = notarizedTransaction.transactionId()
                val submitResult = submitNotarizedTransaction(
                    txID.toHexString(),
                    CompileNotarizedTransactionIntentResponse(compiledNotarizedTransaction)
                )
                return when (submitResult) {
                    is Result.Error -> submitResult
                    is Result.Success -> {
                        submitResult
                    }
                }
            }
        }
    }

//    fun compileAndSign(
//        transactionIntent: TransactionIntent,
//        notaryAndSigners: NotaryAndSigners
//    ): Result<CompileSignedTransactionIntentResponse> {
//        val compiledTransactionIntent: CompileTransactionIntentResponse
//        try {
//            compiledTransactionIntent = engine.compileTransactionIntent(
//                CompileTransactionIntentRequest(
//                    transactionIntent.header,
//                    transactionIntent.manifest
//                )
//            )
//        } catch (e: Exception) {
//            return Result.Error(
//                TransactionApprovalException(
//                    TransactionApprovalFailureCause.CompileTxIntent,
//                    cause = e
//                )
//            )
//        }
//
//        val txID: String
//        try {
//            txID = generateTXID(compiledTransactionIntent.compiledIntent)
//        } catch (e: Exception) {
//            return Result.Error(
//                TransactionApprovalException(
//                    TransactionApprovalFailureCause.GenerateTXId,
//                    cause = e
//                )
//            )
//        }
//        val signaturesWithPublicKey: List<SignatureWithPublicKey>
//        try {
////TODO figure this out
//            signaturesWithPublicKey = emptyList()
//        } catch (e: Exception) {
//            return Result.Error(
//                TransactionApprovalException(
//                    TransactionApprovalFailureCause.SigningIntentWithAccountSigners,
//                    cause = e
//                )
//            )
//        }
//        //TODO not sure if we need it
////        val engineSignaturesWithPublicKey: List<SignatureWithPublicKey>
////        try {
////
////            engineSignaturesWithPublicKey = emptyList()
////        } catch (e: Exception) {
////            return Result.failure(
////                TransactionApprovalException(
////                    TransactionApprovalFailureCause.SigningIntentWithAccountSigners,
////                    cause = e
////                )
////            )
////        }
//        val signedTransactionIntent = SignedTransactionIntent(transactionIntent, signaturesWithPublicKey.toTypedArray())
//        Signature
//        val compiledSignedIntent: ByteArray
//        try {
//            compiledSignedIntent = signedTransactionIntent.compile()
//        } catch (e: Exception) {
//            return Result.Error(
//                TransactionApprovalException(
//                    TransactionApprovalFailureCause.CompileTxIntent,
//                    cause = e
//                )
//            )
//        }
//
//        val notarySignatureWithPublicKey: SignatureWithPublicKey
//        try {
//
//        } catch (e: Exception) {
//
//        }
//        val notarizedTransaction = NotarizedTransaction(signedTransactionIntent)
//        val compiledNotarizedTXIntent: ByteArray
//        try {
//            compiledNotarizedTXIntent = notarizedTransaction.compile()
//        } catch (e: Exception) {
//            return Result.Error(
//                TransactionApprovalException(
//                    TransactionApprovalFailureCause.CompileNotarizedTXIntent,
//                    cause = e
//                )
//            )
//        }
//        return Result.Success(CompileSignedTransactionIntentResponse(compiledNotarizedTXIntent))
//    }

    private suspend fun buildTransactionHeader(
        networkId: Int,
        manifest: TransactionManifest
    ): Result<TransactionHeader> {
        val epochResult = transactionRepository.getLedgerEpoch()
        if (epochResult is Result.Success) {
            val epoch = epochResult.data
            val notaryAndSigners = getNotaryAndSigners(networkId, manifest)
            return Result.Success(
                TransactionHeader(
                    TransactionVersion.Default.intValue.toUByte(),
                    networkId.toUByte(),
                    epoch.toULong(),
                    epoch.toULong() + TransactionConfig.EPOCH_WINDOW,
                    generateNonce(),
                    notaryAndSigners.notarySigner.notaryPublicKey.toEngineModel(),
                    false,
                    TransactionConfig.COST_UNIT_LIMIT,
                    tipPercentage = TransactionConfig.TIP_PERCENTAGE
                )
            )
        } else {
            return Result.Error(TransactionApprovalException(TransactionApprovalFailureCause.GetEpoch))
        }
    }

    private suspend fun getNotaryAndSigners(
        networkId: Int,
        manifest: TransactionManifest,
        transactionVersion: TransactionVersion = TransactionVersion.Default
    ): NotaryAndSigners {
        val addressesNeededToSign = getAddressesNeededToSignTransaction(transactionVersion, networkId, manifest)
        val signers = profileRepository.getSignersForAddresses(networkId, addressesNeededToSign)
        return NotaryAndSigners(signers.first(), signers)
    }

    private suspend fun convertManifestInstructionsToJSON(manifestJson: TransactionManifest): Result<ConvertManifestResponse> {
        val version = TransactionVersion.Default
        val networkId = profileRepository.getCurrentNetworkId()
        return try {
            Result.Success(
                engine.convertManifest(
                    ConvertManifestRequest(
                        transactionVersion = version.intValue.toUByte(),
                        networkId = networkId.toUByte(),
                        manifestInstructionsOutputFormat = ManifestInstructionsKind.JSON,
                        manifest = manifestJson
                    )
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun addLockFeeInstructionToManifest(manifest: TransactionManifest): TransactionManifest {
        val version = TransactionVersion.Default
        val networkId = profileRepository.getCurrentNetworkId()
        val manifestConversionResult = convertManifestInstructionsToJSON(manifest)
        if (manifestConversionResult is Result.Success) {
            val instructions = manifestConversionResult.data.instructions
            val addresses = getAddressesNeededToSignTransaction(version, networkId, manifest)
            val accountAddress = addresses.firstOrNull() ?: profileRepository.getAccounts().first().address.address
            val lockFeeInstruction: Instruction = Instruction.CallMethod(
                Value.ComponentAddress(accountAddress), Value.String(MethodName.LockFee.stringValue), arrayOf(
                    Value.Decimal(
                        BigDecimal.valueOf(10)
                    )
                )
            )
            var updatedInstructions = instructions
            when (instructions) {
                is ManifestInstructions.JSONInstructions -> {
                    updatedInstructions =
                        instructions.copy(instructions = arrayOf(lockFeeInstruction) + instructions.instructions)
                }
                is ManifestInstructions.StringInstructions -> {}
            }
            return TransactionManifest(updatedInstructions, manifest.blobs)
        } else {
            return manifest
        }
    }

    private suspend fun submitNotarizedTransaction(
        txID: String,
        notarizedTransaction: CompileNotarizedTransactionIntentResponse
    ): Result<String> {
        val submitResult =
            transactionRepository.submitTransaction(notarizedTransaction.compiledNotarizedIntent.toHexString())
        when (submitResult) {
            is Result.Error -> {
                return Result.Error(
                    TransactionApprovalException(
                        TransactionApprovalFailureCause.SubmitNotarizedTransaction,
                        cause = submitResult.exception
                    )
                )
            }
            is Result.Success -> {
                if (submitResult.data.duplicate) {
                    return Result.Error(TransactionApprovalException(TransactionApprovalFailureCause.InvalidTXDuplicate))
                } else {
                    var transactionStatus = TransactionStatus.pending
                    var tryCount = 0
                    var errorCount = 0
                    val pollStrategy = PollStrategy()
                    while (!transactionStatus.isComplete()) {
                        tryCount++
                        val statusCheckResult = transactionRepository.getTransactionStatus(txID)
                        if (statusCheckResult is Result.Success) {
                            transactionStatus = statusCheckResult.data.status
                        } else {
                            errorCount++
                        }
                        if (tryCount > pollStrategy.maxTries) {
                            return Result.Error(TransactionApprovalException(TransactionApprovalFailureCause.FailedToPollTXStatus))
                        }
                        delay(pollStrategy.delayBetweenTriesMs)
                    }
                    return Result.Success(txID)
                }
            }
        }
    }

    private fun getAddressesNeededToSignTransaction(
        transactionVersion: TransactionVersion,
        networkId: Int,
        manifest: TransactionManifest
    ): List<String> {
        val addressesNeededToSign = mutableListOf<String>()
        val convertedManifest = engine.convertManifest(
            ConvertManifestRequest(
                transactionVersion.intValue.toUByte(),
                networkId.toUByte(),
                ManifestInstructionsKind.JSON,
                manifest
            )
        )
        when (val instructions = convertedManifest.instructions) {
            is ManifestInstructions.JSONInstructions -> {
                instructions.instructions.filterIsInstance<Instruction.CallFunction>().forEach {
                    val isAccountComponent = it.packageAddress.address.packageAddress.contains("account")
                    val isMethodThatRequiresAuth =
                        MethodName.values().map { it.stringValue }.contains(it.functionName.value)
                    if (isAccountComponent && isMethodThatRequiresAuth) {
                        addressesNeededToSign.add(it.packageAddress.address.packageAddress)
                    }
                }
            }
            is ManifestInstructions.StringInstructions -> {

            }

        }
        return addressesNeededToSign.toList()
    }

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

    private fun generateTXID(compiledIntent: ByteArray): String {
        return compiledIntent.sha256().sha256().toHexString()
    }
}
@file:Suppress("TooGenericExceptionCaught", "ReturnCount", "LongMethod")

package com.babylon.wallet.android.domain.transaction

import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatus
import com.babylon.wallet.android.data.gateway.isComplete
import com.babylon.wallet.android.data.gateway.isFailed
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.radixdlt.crypto.toECKeyPair
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.builders.TransactionBuilder
import com.radixdlt.toolkit.models.CallMethodReceiver
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.Value
import com.radixdlt.toolkit.models.request.CompileNotarizedTransactionIntentResponse
import com.radixdlt.toolkit.models.request.ConvertManifestRequest
import com.radixdlt.toolkit.models.request.ConvertManifestResponse
import com.radixdlt.toolkit.models.transaction.ManifestInstructions
import com.radixdlt.toolkit.models.transaction.ManifestInstructionsKind
import com.radixdlt.toolkit.models.transaction.TransactionHeader
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import rdx.works.profile.data.repository.ProfileRepository
import java.math.BigDecimal
import java.security.SecureRandom
import javax.inject.Inject

class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
) {

    private val engine = RadixEngineToolkit

    suspend fun signAndSubmitTransaction(manifest: TransactionManifest, hasLockFee: Boolean): Result<String> {
        val networkId = profileRepository.getCurrentNetworkId().value
        return signAndSubmitTransaction(manifest, networkId, hasLockFee)
    }

    suspend fun signAndSubmitTransaction(manifestData: TransactionManifestData): Result<String> {
        val networkId = profileRepository.getCurrentNetworkId().value
        val manifestConversionResult =
            convertManifestInstructionsToJSON(
                version = manifestData.version,
                TransactionManifest(
                    ManifestInstructions.StringInstructions(manifestData.instructions),
                    manifestData.blobs.toTypedArray()
                )
            )
        val jsonTransactionManifest = when (manifestConversionResult) {
            is Result.Error -> return manifestConversionResult
            is Result.Success -> manifestConversionResult.data
        }
        return signAndSubmitTransaction(jsonTransactionManifest, networkId, false)
    }

    private suspend fun signAndSubmitTransaction(
        jsonTransactionManifest: TransactionManifest,
        networkId: Int,
        hasLockFeeInstruction: Boolean,
    ): Result<String> {
        val addressesNeededToSign =
            getAddressesNeededToSignTransaction(jsonTransactionManifest)
        val notaryAndSigners = getNotaryAndSigners(networkId, addressesNeededToSign)
        when (val transactionHeaderResult = buildTransactionHeader(networkId, notaryAndSigners)) {
            is Result.Error -> return transactionHeaderResult
            is Result.Success -> {
                val accountAddressToLockFee =
                    addressesNeededToSign.firstOrNull() ?: profileRepository.getAccounts().first().entityAddress.address
                val manifestWithTransactionFee = if (hasLockFeeInstruction) {
                    jsonTransactionManifest
                } else {
                    addLockFeeInstructionToManifest(jsonTransactionManifest, accountAddressToLockFee)
                }
                val notarizedTransaction = try {
                    val notarizedTransactionBuilder = TransactionBuilder()
                        .manifest(manifestWithTransactionFee)
                        .header(transactionHeaderResult.data)
                        .sign(
                            notaryAndSigners.signers.map {
                                it.privateKey.toEngineModel()
                            }
                        )
                    notarizedTransactionBuilder.notarize(notaryAndSigners.notarySigner.privateKey.toEngineModel())
                } catch (e: Exception) {
                    return Result.Error(
                        TransactionApprovalException(
                            TransactionApprovalFailure.PrepareNotarizedTransaction,
                            msg = e.message,
                            e = e
                        )
                    )
                }
                val compiledNotarizedTransaction = notarizedTransaction.compile().getOrElse { e ->
                    return Result.Error(
                        TransactionApprovalException(
                            TransactionApprovalFailure.PrepareNotarizedTransaction,
                            msg = e.message,
                            e = e
                        )
                    )
                }
                val txID = notarizedTransaction.transactionId().getOrElse { e ->
                    return Result.Error(
                        TransactionApprovalException(
                            TransactionApprovalFailure.PrepareNotarizedTransaction,
                            msg = e.message,
                            e = e
                        )
                    )
                }
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
                        TransactionVersion.Default.value.toUByte(),
                        networkId.toUByte(),
                        epoch.toULong(),
                        epoch.toULong() + TransactionConfig.EPOCH_WINDOW,
                        generateNonce(),
                        notaryAndSigners.notarySigner.privateKey.toECKeyPair().toEnginePublicKeyModel(),
                        false,
                        TransactionConfig.COST_UNIT_LIMIT,
                        tipPercentage = TransactionConfig.TIP_PERCENTAGE
                    )
                )
            } catch (e: Exception) {
                Result.Error(
                    TransactionApprovalException(
                        TransactionApprovalFailure.BuildTransactionHeader,
                        e.message,
                        e
                    )
                )
            }
        } else {
            return Result.Error(TransactionApprovalException(TransactionApprovalFailure.GetEpoch))
        }
    }

    private suspend fun getNotaryAndSigners(
        networkId: Int,
        addressesNeededToSign: List<String>,
    ): NotaryAndSigners {
        val signers = profileRepository.getSignersForAddresses(networkId, addressesNeededToSign)
        return NotaryAndSigners(signers.first(), signers)
    }

    private suspend fun convertManifestInstructionsToJSON(
        version: Long,
        manifest: TransactionManifest,
    ): Result<ConvertManifestResponse> {
        val networkId = profileRepository.getCurrentNetworkId()
        return try {
            Result.Success(
                engine.convertManifest(
                    ConvertManifestRequest(
                        transactionVersion = version.toUByte(),
                        networkId = networkId.value.toUByte(),
                        manifestInstructionsOutputFormat = ManifestInstructionsKind.JSON,
                        manifest = manifest
                    )
                ).getOrThrow()
            )
        } catch (e: SerializationException) {
            Result.Error(TransactionApprovalException(TransactionApprovalFailure.ConvertManifest, e.message, e))
        }
    }

    fun addLockFeeInstructionToManifest(
        manifest: TransactionManifest,
        addressToLockFee: String,
    ): TransactionManifest {
        val instructions = manifest.instructions
        val lockFeeInstruction: Instruction = Instruction.CallMethod(
            Value.ComponentAddress(addressToLockFee),
            Value.String(MethodName.LockFee.stringValue),
            arrayOf(
                Value.Decimal(
                    BigDecimal.valueOf(TransactionConfig.DEFAULT_LOCK_FEE)
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
        return manifest.copy(updatedInstructions, manifest.blobs)
    }

    private suspend fun submitNotarizedTransaction(
        txID: String,
        notarizedTransaction: CompileNotarizedTransactionIntentResponse,
    ): Result<String> {
        val submitResult =
            transactionRepository.submitTransaction(notarizedTransaction.compiledNotarizedIntent.toHexString())
        return when (submitResult) {
            is Result.Error -> {
                Result.Error(
                    TransactionApprovalException(
                        TransactionApprovalFailure.SubmitNotarizedTransaction,
                        e = submitResult.exception,
                    )
                )
            }
            is Result.Success -> {
                if (submitResult.data.duplicate) {
                    Result.Error(
                        TransactionApprovalException(TransactionApprovalFailure.InvalidTXDuplicate(txID))
                    )
                } else {
                    pollTransactionStatus(txID)
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun pollTransactionStatus(txID: String): Result<String> {
        var transactionStatus = TransactionStatus.pending
        var tryCount = 0
        var errorCount = 0
        val maxTries = 20
        val delayBetweenTriesMs = 2000L
        while (!transactionStatus.isComplete()) {
            tryCount++
            val statusCheckResult = transactionRepository.getTransactionStatus(txID)
            if (statusCheckResult is Result.Success) {
                transactionStatus = statusCheckResult.data.status
            } else {
                errorCount++
            }
            if (tryCount > maxTries) {
                return Result.Error(
                    TransactionApprovalException(TransactionApprovalFailure.FailedToPollTXStatus(txID))
                )
            }
            delay(delayBetweenTriesMs)
        }
        if (transactionStatus.isFailed()) {
            when (transactionStatus) {
                TransactionStatus.committedFailure -> {
                    return Result.Error(
                        TransactionApprovalException(TransactionApprovalFailure.GatewayCommittedFailure(txID))
                    )
                }
                TransactionStatus.rejected -> {
                    return Result.Error(
                        TransactionApprovalException(TransactionApprovalFailure.GatewayRejected(txID))
                    )
                }
                else -> {}
            }
        }
        return Result.Success(txID)
    }

    private fun getAddressesNeededToSignTransaction(
        jsonTransactionManifest: TransactionManifest,
    ): List<String> {
        val addressesNeededToSign = mutableListOf<String>()
        when (val instructions = jsonTransactionManifest.instructions) {
            is ManifestInstructions.JSONInstructions -> {
                instructions.instructions.filterIsInstance<Instruction.CallMethod>().forEach { callMethod ->
                    val componentAddress = when (val callMethodReceiver = callMethod.componentAddress) {
                        is CallMethodReceiver.Component -> null
                        is CallMethodReceiver.ComponentAddress -> {
                            callMethodReceiver.componentAddress.address.componentAddress
                        }
                    }
                    val isAccountComponent = componentAddress?.contains("account") == true
                    val isMethodThatRequiresAuth =
                        MethodName.methodsThatRequireAuth().map { name -> name.stringValue }
                            .contains(callMethod.methodName.value)
                    if (isAccountComponent && isMethodThatRequiresAuth && componentAddress != null) {
                        addressesNeededToSign.add(componentAddress)
                    }
                }
            }
            is ManifestInstructions.StringInstructions -> {
            }
        }
        return addressesNeededToSign.toList()
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

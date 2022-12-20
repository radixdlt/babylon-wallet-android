@file:Suppress("TooGenericExceptionCaught")

package com.babylon.wallet.android.domain.transaction

import RadixEngineToolkit
import builders.TransactionBuilder
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatus
import com.babylon.wallet.android.data.gateway.isComplete
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.common.Result
import com.radixdlt.crypto.toECKeyPair
import com.radixdlt.hex.extensions.toHexString
import kotlinx.coroutines.delay
import models.CallMethodReceiver
import models.Instruction
import models.Value
import models.request.CompileNotarizedTransactionIntentResponse
import models.request.ConvertManifestRequest
import models.request.ConvertManifestResponse
import models.transaction.ManifestInstructions
import models.transaction.ManifestInstructionsKind
import models.transaction.TransactionHeader
import models.transaction.TransactionManifest
import rdx.works.profile.data.repository.ProfileRepository
import java.math.BigDecimal
import java.security.SecureRandom
import javax.inject.Inject

class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository
) {

    private val engine = RadixEngineToolkit

    suspend fun signAndSubmitTransaction(manifest: TransactionManifest): Result<String> {
        val networkId = profileRepository.getCurrentNetworkId().value
        val transactionVersion = TransactionVersion.Default
        val addressesNeededToSign = getAddressesNeededToSignTransaction(transactionVersion, networkId, manifest)
        val notaryAndSigners = getNotaryAndSigners(networkId, addressesNeededToSign)
        when (val transactionHeaderResult = buildTransactionHeader(networkId, notaryAndSigners)) {
            is Result.Error -> return transactionHeaderResult
            is Result.Success -> {
                val accountAddressToLockFee =
                    addressesNeededToSign.firstOrNull() ?: profileRepository.getAccounts().first().entityAddress.address
                val manifestWithTransactionFee = addLockFeeInstructionToManifest(manifest, accountAddressToLockFee)
                val notarizedTransactionBuilder =
                    TransactionBuilder().manifest(manifestWithTransactionFee).header(transactionHeaderResult.data)
                val notarizedTransaction =
                    notarizedTransactionBuilder.notarize(notaryAndSigners.notarySigner.notaryPrivateKey.toEngineModel())
                val compiledNotarizedTransaction = notarizedTransaction.compile()
                val txID = notarizedTransaction.transactionId()
                val submitResult = submitNotarizedTransaction(
                    txID.toHexString(), CompileNotarizedTransactionIntentResponse(compiledNotarizedTransaction)
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
        notaryAndSigners: NotaryAndSigners
    ): Result<TransactionHeader> {
        val epochResult = transactionRepository.getLedgerEpoch()
        if (epochResult is Result.Success) {
            val epoch = epochResult.data
            return Result.Success(
                TransactionHeader(
                    TransactionVersion.Default.intValue.toUByte(),
                    networkId.toUByte(),
                    epoch.toULong(),
                    epoch.toULong() + TransactionConfig.EPOCH_WINDOW,
                    generateNonce(),
                    notaryAndSigners.notarySigner.notaryPrivateKey.toECKeyPair().toEnginePublicKeyModel(),
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
        addressesNeededToSign: List<String>
    ): NotaryAndSigners {
        val signers = profileRepository.getSignersForAddresses(networkId, addressesNeededToSign)
        return NotaryAndSigners(signers.first(), signers)
    }

    private suspend fun convertManifestInstructionsToJSON(
        manifestJson: TransactionManifest
    ): Result<ConvertManifestResponse> {
        val version = TransactionVersion.Default
        val networkId = profileRepository.getCurrentNetworkId()
        return try {
            Result.Success(
                engine.convertManifest(
                    ConvertManifestRequest(
                        transactionVersion = version.intValue.toUByte(),
                        networkId = networkId.value.toUByte(),
                        manifestInstructionsOutputFormat = ManifestInstructionsKind.JSON,
                        manifest = manifestJson
                    )
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun addLockFeeInstructionToManifest(
        manifest: TransactionManifest,
        addressToLockFee: String
    ): TransactionManifest {
        val manifestConversionResult = convertManifestInstructionsToJSON(manifest)
        if (manifestConversionResult is Result.Success) {
            val instructions = manifestConversionResult.data.instructions
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
        return when (submitResult) {
            is Result.Error -> {
                Result.Error(
                    TransactionApprovalException(
                        TransactionApprovalFailureCause.SubmitNotarizedTransaction, cause = submitResult.exception
                    )
                )
            }
            is Result.Success -> {
                if (submitResult.data.duplicate) {
                    Result.Error(
                        TransactionApprovalException(TransactionApprovalFailureCause.InvalidTXDuplicate)
                    )
                } else {
                    pollTransactionStatus(txID)
                }
            }
        }
    }

    private suspend fun pollTransactionStatus(txID: String): Result<String> {
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
                return Result.Error(
                    TransactionApprovalException(TransactionApprovalFailureCause.FailedToPollTXStatus)
                )
            }
            delay(pollStrategy.delayBetweenTriesMs)
        }
        return Result.Success(txID)
    }

    private fun getAddressesNeededToSignTransaction(
        transactionVersion: TransactionVersion,
        networkId: Int,
        manifest: TransactionManifest
    ): List<String> {
        val addressesNeededToSign = mutableListOf<String>()
        val convertedManifest = engine.convertManifest(
            ConvertManifestRequest(
                transactionVersion.intValue.toUByte(), networkId.toUByte(), ManifestInstructionsKind.JSON, manifest
            )
        )
        when (val instructions = convertedManifest.instructions) {
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

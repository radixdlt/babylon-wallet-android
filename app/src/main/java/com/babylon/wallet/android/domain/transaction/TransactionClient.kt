@file:Suppress("TooGenericExceptionCaught", "ReturnCount", "TooManyFunctions")

package com.babylon.wallet.android.domain.transaction

import RadixEngineToolkit
import builders.ManifestBuilder
import builders.TransactionBuilder
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.gateway.generated.model.TransactionLookupIdentifier
import com.babylon.wallet.android.data.gateway.generated.model.TransactionLookupOrigin
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatus
import com.babylon.wallet.android.data.gateway.isComplete
import com.babylon.wallet.android.data.gateway.isFailed
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.KnownAddresses
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.radixdlt.crypto.toECKeyPair
import com.radixdlt.hex.extensions.toHexString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
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
import timber.log.Timber
import java.math.BigDecimal
import java.security.SecureRandom
import javax.inject.Inject

class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
) {

    private val engine = RadixEngineToolkit

    fun isAllowedToUseFaucet(address: String): Flow<Boolean> {
        return preferencesManager.getLastUsedEpochFlow(address).map { lastUsedEpoch ->
            if (lastUsedEpoch == null) {
                true
            } else {
                when (val currentEpoch = transactionRepository.getLedgerEpoch()) {
                    is Result.Error -> false
                    is Result.Success -> {
                        if (currentEpoch.data < lastUsedEpoch) {
                            false
                        } else {
                            val threshold = 1
                            currentEpoch.data - lastUsedEpoch >= threshold
                        }
                    }
                }
            }
        }
    }

    suspend fun getFreeXrd(includeLockFeeInstruction: Boolean, address: String): Result<String> {
        val networkId = profileRepository.getCurrentNetworkId()
        val knownAddresses = KnownAddresses.addressMap[networkId]
        return if (knownAddresses != null) {
            val manifest = buildFaucetManifest(knownAddresses, address, includeLockFeeInstruction)
            when (val epochResult = transactionRepository.getLedgerEpoch()) {
                is Result.Error -> epochResult
                is Result.Success -> {
                    val submitResult = signAndSubmitTransaction(manifest, true)
                    if (submitResult is Result.Success) {
                        preferencesManager.updateEpoch(address, epochResult.data)
                    }
                    return submitResult
                }
            }
        } else {
            Result.Error()
        }
    }

    private fun buildFaucetManifest(
        knownAddresses: KnownAddresses,
        address: String,
        includeLockFeeInstruction: Boolean,
    ): TransactionManifest {
        var manifest = ManifestBuilder().addInstruction(
            Instruction.CallMethod(
                componentAddress = CallMethodReceiver.ComponentAddress(
                    Value.ComponentAddress(knownAddresses.faucetAddress)
                ),
                methodName = Value.String(MethodName.Free.stringValue)
            )
        ).addInstruction(
            Instruction.CallMethod(
                componentAddress = CallMethodReceiver.ComponentAddress(
                    Value.ComponentAddress(address)
                ),
                methodName = Value.String(MethodName.DepositBatch.stringValue),
                arrayOf(Value.Expression("ENTIRE_WORKTOP"))
            )
        ).build()
        if (includeLockFeeInstruction) {
            manifest = addLockFeeInstructionToManifest(manifest, knownAddresses.faucetAddress)
        }
        return manifest
    }

    private suspend fun signAndSubmitTransaction(manifest: TransactionManifest, hasLockFee: Boolean): Result<String> {
        val networkId = profileRepository.getCurrentNetworkId().value
        return signAndSubmitTransaction(manifest, TransactionVersion.Default.value, networkId, hasLockFee)
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
        return signAndSubmitTransaction(jsonTransactionManifest, manifestData.version, networkId, false)
    }

    private suspend fun signAndSubmitTransaction(
        manifest: TransactionManifest,
        version: Long,
        networkId: Int,
        hasLockFeeInstruction: Boolean,
    ): Result<String> {
        val addressesNeededToSign =
            getAddressesNeededToSignTransaction(version, networkId, manifest)
        val notaryAndSigners = getNotaryAndSigners(networkId, addressesNeededToSign)
        when (val transactionHeaderResult = buildTransactionHeader(networkId, notaryAndSigners)) {
            is Result.Error -> return transactionHeaderResult
            is Result.Success -> {
                val accountAddressToLockFee =
                    addressesNeededToSign.firstOrNull() ?: profileRepository.getAccounts().first().entityAddress.address
                val manifestWithTransactionFee = if (hasLockFeeInstruction) manifest else
                    addLockFeeInstructionToManifest(manifest, accountAddressToLockFee)
                val notarizedTransaction = try {
                    val notarizedTransactionBuilder =
                        TransactionBuilder().manifest(manifestWithTransactionFee).header(transactionHeaderResult.data)
                    notarizedTransactionBuilder.notarize(notaryAndSigners.notarySigner.notaryPrivateKey.toEngineModel())
                } catch (e: Exception) {
                    return Result.Error(
                        TransactionApprovalException(
                            TransactionApprovalFailure.PrepareNotarizedTransaction,
                            msg = e.message,
                            e = e
                        )
                    )
                }
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
                        notaryAndSigners.notarySigner.notaryPrivateKey.toECKeyPair().toEnginePublicKeyModel(),
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
                )
            )
        } catch (e: SerializationException) {
            Result.Error(TransactionApprovalException(TransactionApprovalFailure.ConvertManifest, e.message, e))
        }
    }

    private fun addLockFeeInstructionToManifest(
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
        return TransactionManifest(updatedInstructions, manifest.blobs)
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
                    TransactionApprovalException(TransactionApprovalFailure.FailedToPollTXStatus(txID))
                )
            }
            delay(pollStrategy.delayBetweenTriesMs)
        }
        if (transactionStatus.isFailed()) {
            when (transactionStatus) {
                TransactionStatus.committedFailure -> {
                    transactionRepository.getTransactionDetails(
                        TransactionLookupIdentifier(
                            TransactionLookupOrigin.intent,
                            txID
                        )
                    ).onValue {
                        Timber.d("Details: $it")
                    }
                    return Result.Error(
                        TransactionApprovalException(TransactionApprovalFailure.GatewayCommittedFailure(txID))
                    )
                }
                TransactionStatus.rejected -> {
                    transactionRepository.getTransactionDetails(
                        TransactionLookupIdentifier(
                            TransactionLookupOrigin.intent,
                            txID
                        )
                    ).onValue {
                        Timber.d("Details: $it")
                    }
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
        transactionVersion: Long,
        networkId: Int,
        manifest: TransactionManifest,
    ): List<String> {
        val addressesNeededToSign = mutableListOf<String>()
        val convertedManifest = engine.convertManifest(
            ConvertManifestRequest(
                transactionVersion.toUByte(), networkId.toUByte(), ManifestInstructionsKind.JSON, manifest
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

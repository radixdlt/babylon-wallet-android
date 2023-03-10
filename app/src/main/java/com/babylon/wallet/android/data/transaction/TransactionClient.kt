@file:Suppress("TooGenericExceptionCaught", "ReturnCount", "LongMethod", "TooManyFunctions")

package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatus
import com.babylon.wallet.android.data.gateway.isComplete
import com.babylon.wallet.android.data.gateway.isFailed
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.radixdlt.crypto.toECKeyPair
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.builders.TransactionBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.Value
import com.radixdlt.toolkit.models.address.Address
import com.radixdlt.toolkit.models.request.CompileNotarizedTransactionResponse
import com.radixdlt.toolkit.models.request.ConvertManifestRequest
import com.radixdlt.toolkit.models.request.ConvertManifestResponse
import com.radixdlt.toolkit.models.transaction.ManifestInstructions
import com.radixdlt.toolkit.models.transaction.ManifestInstructionsKind
import com.radixdlt.toolkit.models.transaction.TransactionHeader
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.coroutines.delay
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.ProfileDataSource
import java.math.BigDecimal
import java.security.SecureRandom
import javax.inject.Inject

class TransactionClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val profileDataSource: ProfileDataSource,
    private val accountRepository: AccountRepository,
    private val getAccountResourcesUseCase: GetAccountResourcesUseCase,
    private val cache: HttpCache
) {

    private val engine = RadixEngineToolkit

    suspend fun signAndSubmitTransaction(
        manifest: TransactionManifest,
        hasLockFee: Boolean
    ): Result<String> {
        val networkId = profileDataSource.getCurrentNetworkId().value
        return signAndSubmitTransaction(manifest, networkId, hasLockFee)
    }

    suspend fun signAndSubmitTransaction(manifestData: TransactionManifestData): Result<String> {
        val networkId = profileDataSource.getCurrentNetworkId().value
        val manifestConversionResult = convertManifestInstructionsToJSON(
            manifest = TransactionManifest(
                instructions = ManifestInstructions.StringInstructions(manifestData.instructions),
                blobs = manifestData.blobs.toTypedArray()
            )
        )
        val jsonTransactionManifest = when (manifestConversionResult) {
            is Result.Error -> return manifestConversionResult
            is Result.Success -> manifestConversionResult.data
        }
        return signAndSubmitTransaction(
            jsonTransactionManifest = jsonTransactionManifest,
            networkId = networkId,
            hasLockFeeInstruction = false
        )
    }

    suspend fun addLockFeeToTransactionManifestData(
        manifestData: TransactionManifestData
    ): Result<TransactionManifest> {
        val manifestConversionResult = convertManifestInstructionsToJSON(
            manifest = TransactionManifest(
                instructions = ManifestInstructions.StringInstructions(manifestData.instructions),
                blobs = manifestData.blobs.toTypedArray()
            )
        )
        val jsonTransactionManifest = when (manifestConversionResult) {
            is Result.Error -> return manifestConversionResult
            is Result.Success -> manifestConversionResult.data
        }
        val addressesInvolved = getAddressesInvolvedInATransaction(jsonTransactionManifest)
        val accountAddressToLockFee = selectAccountAddressToLockFee(addressesInvolved) ?: return Result.Error(
            TransactionApprovalException(
                DappRequestFailure.TransactionApprovalFailure.FailedToFindAccountWithEnoughFundsToLockFee
            )
        )
        return Result.Success(
            addLockFeeInstructionToManifest(
                manifest = jsonTransactionManifest,
                addressToLockFee = accountAddressToLockFee
            )
        )
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
        networkId: Int,
        hasLockFeeInstruction: Boolean,
    ): Result<String> {
        val addressesInvolved = getAddressesInvolvedInATransaction(jsonTransactionManifest)
        val manifestWithTransactionFee = if (hasLockFeeInstruction) {
            jsonTransactionManifest
        } else {
            val accountAddressToLockFee =
                selectAccountAddressToLockFee(addressesInvolved) ?: return Result.Error(
                    TransactionApprovalException(
                        DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction
                    )
                )
            addLockFeeInstructionToManifest(jsonTransactionManifest, accountAddressToLockFee)
        }
        val addressesNeededToSign = getAddressesNeededToSign(manifestWithTransactionFee)
        val notaryAndSigners = getNotaryAndSigners(networkId, addressesNeededToSign)
            ?: return Result.Error(
                TransactionApprovalException(
                    DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction
                )
            )
        when (val transactionHeaderResult = buildTransactionHeader(networkId, notaryAndSigners)) {
            is Result.Error -> return transactionHeaderResult
            is Result.Success -> {
                val notarizedTransaction = try {
                    val notarizedTransactionBuilder = TransactionBuilder()
                        .manifest(manifestWithTransactionFee)
                        .header(transactionHeaderResult.data)
                        .sign(
                            privateKeys = notaryAndSigners.signers.map { accountSigner ->
                                accountSigner.privateKey.toEngineModel()
                            }.toTypedArray()
                        )
                    notarizedTransactionBuilder.notarize(notaryAndSigners.notarySigner.privateKey.toEngineModel())
                } catch (e: Exception) {
                    return Result.Error(
                        TransactionApprovalException(
                            DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction,
                            msg = e.message,
                            e = e
                        )
                    )
                }
                val compiledNotarizedTransaction = notarizedTransaction.compile().getOrElse { e ->
                    return Result.Error(
                        TransactionApprovalException(
                            DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction,
                            msg = e.message,
                            e = e
                        )
                    )
                }
                val txID = notarizedTransaction.transactionId().getOrElse { e ->
                    return Result.Error(
                        TransactionApprovalException(
                            DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction,
                            msg = e.message,
                            e = e
                        )
                    )
                }
                return submitNotarizedTransaction(
                    txID.toHexString(),
                    CompileNotarizedTransactionResponse(compiledNotarizedTransaction)
                )
            }
        }
    }

    suspend fun selectAccountAddressToLockFee(involvedAddresses: List<String>): String? {
        var selectedAddress: String? = null
        for (address in involvedAddresses) {
            when (val account = getAccountResourcesUseCase(address, isRefreshing = true)) {
                is Result.Error -> null
                is Result.Success -> {
                    if (account.data.hasXrdWithEnoughBalance(TransactionConfig.DEFAULT_LOCK_FEE)) {
                        selectedAddress = account.data.address
                        break
                    }
                }
            }
        }
        return selectedAddress
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
                        notaryPublicKey = notaryAndSigners.notarySigner.privateKey
                            .toECKeyPair()
                            .toEnginePublicKeyModel(),
                        notaryAsSignatory = false,
                        costUnitLimit = TransactionConfig.COST_UNIT_LIMIT,
                        tipPercentage = TransactionConfig.TIP_PERCENTAGE
                    )
                )
            } catch (e: Exception) {
                Result.Error(
                    TransactionApprovalException(
                        DappRequestFailure.TransactionApprovalFailure.BuildTransactionHeader,
                        e.message,
                        e
                    )
                )
            }
        } else {
            return Result.Error(TransactionApprovalException(DappRequestFailure.GetEpoch))
        }
    }

    private suspend fun getNotaryAndSigners(
        networkId: Int,
        addressesNeededToSign: List<String>,
    ): NotaryAndSigners? {
        val signers = accountRepository.getSignersForAddresses(networkId, addressesNeededToSign)
        return if (signers.isEmpty()) {
            null
        } else {
            NotaryAndSigners(signers.first(), signers)
        }
    }

    private suspend fun convertManifestInstructionsToJSON(
        manifest: TransactionManifest
    ): Result<ConvertManifestResponse> {
        val networkId = profileDataSource.getCurrentNetworkId()
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
                TransactionApprovalException(
                    failure = DappRequestFailure.TransactionApprovalFailure.ConvertManifest,
                    msg = e.message,
                    e = e
                )
            )
        }
    }

    private suspend fun convertManifestInstructionsToString(
        manifest: TransactionManifest,
    ): Result<ConvertManifestResponse> {
        val networkId = profileDataSource.getCurrentNetworkId()
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
                TransactionApprovalException(
                    DappRequestFailure.TransactionApprovalFailure.ConvertManifest,
                    e.message,
                    e
                )
            )
        }
    }

    fun addLockFeeInstructionToManifest(
        manifest: TransactionManifest,
        addressToLockFee: String,
    ): TransactionManifest {
        val instructions = manifest.instructions
        val lockFeeInstruction: Instruction = Instruction.CallMethod(
            componentAddress = Value.ComponentAddress(addressToLockFee),
            methodName = Value.String(MethodName.LockFee.stringValue),
            arguments = arrayOf(
                Value.Decimal(
                    BigDecimal.valueOf(TransactionConfig.DEFAULT_LOCK_FEE)
                )
            )
        )
        var updatedInstructions = instructions
        when (instructions) {
            is ManifestInstructions.ParsedInstructions -> {
                updatedInstructions = instructions.copy(
                    instructions = arrayOf(lockFeeInstruction) + instructions.instructions
                )
            }
            is ManifestInstructions.StringInstructions -> {}
        }
        return manifest.copy(updatedInstructions, manifest.blobs)
    }

    private suspend fun submitNotarizedTransaction(
        txID: String,
        notarizedTransaction: CompileNotarizedTransactionResponse,
    ): Result<String> {
        val submitResult = transactionRepository.submitTransaction(
            notarizedTransaction = notarizedTransaction.compiledNotarizedIntent.toHexString()
        )
        return when (submitResult) {
            is Result.Error -> {
                Result.Error(
                    TransactionApprovalException(
                        DappRequestFailure.TransactionApprovalFailure.SubmitNotarizedTransaction,
                        e = submitResult.exception,
                    )
                )
            }
            is Result.Success -> {
                // Invalidate all cached information stored, since a transaction may mutate
                // some resource information
                cache.invalidate()

                if (submitResult.data.duplicate) {
                    Result.Error(
                        TransactionApprovalException(DappRequestFailure.TransactionApprovalFailure.InvalidTXDuplicate(txID))
                    )
                } else {
                    Result.Success(txID)
                }
            }
        }
    }

    @Suppress("MagicNumber")
    suspend fun pollTransactionStatus(txID: String): Result<String> {
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
                    TransactionApprovalException(DappRequestFailure.TransactionApprovalFailure.FailedToPollTXStatus(txID))
                )
            }
            delay(delayBetweenTriesMs)
        }
        if (transactionStatus.isFailed()) {
            when (transactionStatus) {
                TransactionStatus.committedFailure -> {
                    return Result.Error(
                        TransactionApprovalException(
                            DappRequestFailure.TransactionApprovalFailure.GatewayCommittedFailure(txID)
                        )
                    )
                }
                TransactionStatus.rejected -> {
                    return Result.Error(
                        TransactionApprovalException(
                            DappRequestFailure.TransactionApprovalFailure.GatewayRejected(txID)
                        )
                    )
                }
                else -> {}
            }
        }
        return Result.Success(txID)
    }

    fun getAddressesNeededToSign(jsonTransactionManifest: TransactionManifest): List<String> {
        return getAddressesInvolvedInATransaction(jsonTransactionManifest, ::callMethodAuthorizedFilter)
    }

    @Suppress("NestedBlockDepth")
    fun getAddressesInvolvedInATransaction(
        jsonTransactionManifest: TransactionManifest,
        callInstructionFilter: ((String) -> Boolean) = { true }
    ): List<String> {
        val addressesNeededToSign = mutableListOf<String>()
        when (val manifestInstructions = jsonTransactionManifest.instructions) {
            is ManifestInstructions.ParsedInstructions -> {
                manifestInstructions.instructions
                    .forEach { instruction ->
                        when (instruction) {
                            is Instruction.CallMethod -> {
                                instruction.componentAddress.executeIfAccountComponent { accountAddress ->
                                    if (callInstructionFilter(instruction.methodName.value)) {
                                        addressesNeededToSign.add(accountAddress)
                                    }
                                }
                            }
                            is Instruction.SetMetadata -> {
                                (instruction.entityAddress as? Address.ComponentAddress)
                                    ?.executeIfAccountComponent { accountAddress ->
                                        addressesNeededToSign.add(accountAddress)
                                    }
                            }
                            is Instruction.SetMethodAccessRule -> {
                                (instruction.entityAddress as? Address.ComponentAddress)
                                    ?.executeIfAccountComponent { accountAddress ->
                                        addressesNeededToSign.add(accountAddress)
                                    }
                            }
                            is Instruction.SetComponentRoyaltyConfig -> {
                                instruction.componentAddress.executeIfAccountComponent { accountAddress ->
                                    addressesNeededToSign.add(accountAddress)
                                }
                            }
                            is Instruction.ClaimComponentRoyalty -> {
                                instruction.componentAddress.executeIfAccountComponent { accountAddress ->
                                    addressesNeededToSign.add(accountAddress)
                                }
                            }
                            else -> {}
                        }
                    }
            }
            is ManifestInstructions.StringInstructions -> {
            }
        }
        return addressesNeededToSign.distinct().toList()
    }

    private fun callMethodAuthorizedFilter(instructionName: String): Boolean {
        return MethodName
            .methodsThatRequireAuth()
            .map { methodName ->
                methodName.stringValue
            }
            .contains(instructionName)
    }

    private fun Value.ComponentAddress.executeIfAccountComponent(action: (String) -> Unit) {
        if (address.componentAddress.startsWith("account")) {
            action(address.componentAddress)
        }
    }

    private fun Address.ComponentAddress.executeIfAccountComponent(action: (String) -> Unit) {
        if (componentAddress.startsWith("account")) {
            action(componentAddress)
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

@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.MethodName
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.EnumDiscriminator
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.ManifestAstValue
import com.radixdlt.toolkit.models.ValueKind
import com.radixdlt.toolkit.models.request.ConvertManifestRequest
import com.radixdlt.toolkit.models.request.ConvertManifestResponse
import com.radixdlt.toolkit.models.request.KnownEntityAddressesRequest
import com.radixdlt.toolkit.models.transaction.ManifestInstructions
import com.radixdlt.toolkit.models.transaction.ManifestInstructionsKind
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import rdx.works.core.decodeHex
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.FactorInstance
import java.math.BigDecimal

/**
 * Instruction to add free xrd from given address
 */
fun ManifestBuilder.addFreeXrdInstruction(
    faucetComponentAddress: String
): ManifestBuilder {
    return addInstruction(
        Instruction.CallMethod(
            componentAddress = ManifestAstValue.Address(faucetComponentAddress),
            methodName = ManifestAstValue.String(MethodName.Free.stringValue),
            arguments = arrayOf()
        )
    )
}

/**
 * Instruction to Deposit withdrawn tokens
 * @param recipientComponentAddress component we want to deposit on
 */
fun ManifestBuilder.addDepositBatchInstruction(
    recipientComponentAddress: String
): ManifestBuilder {
    return addInstruction(
        Instruction.CallMethod(
            componentAddress = ManifestAstValue.Address(
                value = recipientComponentAddress
            ),
            methodName = ManifestAstValue.String(MethodName.DepositBatch.stringValue),
            arguments = arrayOf(ManifestAstValue.Expression("ENTIRE_WORKTOP"))
        )
    )
}

fun TransactionManifest.getStringInstructions(): String? {
    return when (val instructions = this.instructions) {
        is ManifestInstructions.ParsedInstructions -> null
        is ManifestInstructions.StringInstructions -> instructions.instructions
    }
}

fun ManifestBuilder.addSetMetadataInstructionForOwnerKeys(
    entityAddress: String,
    ownerKeys: List<FactorInstance.PublicKey>
): ManifestBuilder {
    val keysAsEngineValues: Array<ManifestAstValue> = ownerKeys.map { key ->
        ManifestAstValue.Enum(
            variant = publicKeyDiscriminator,
            fields = arrayOf(
                ManifestAstValue.Enum(
                    variant = key.curveKindScryptoDiscriminator(),
                    fields = arrayOf(ManifestAstValue.Bytes(key.compressedData.decodeHex()))
                )
            )
        )
    }.toTypedArray()
    return addInstruction(
        Instruction.SetMetadata(
            entityAddress = ManifestAstValue.Address(
                value = entityAddress
            ),
            key = ManifestAstValue.String(ExplicitMetadataKey.OWNER_KEYS.key),
            value = ManifestAstValue.Enum(
                variant = metadataEntryDiscriminator,
                fields = arrayOf(ManifestAstValue.Array(ValueKind.Enum, keysAsEngineValues))
            )
        )
    )
}

/**
 * Lock fee instruction
 */
fun ManifestBuilder.addLockFeeInstruction(
    addressToLockFee: String
): ManifestBuilder {
    return addInstruction(lockFeeInstruction(addressToLockFee))
}

fun TransactionManifest.addLockFeeInstructionToManifest(
    addressToLockFee: String
): TransactionManifest {
    val instructions = instructions
    var updatedInstructions = instructions
    when (instructions) {
        is ManifestInstructions.ParsedInstructions -> {
            updatedInstructions =
                instructions.copy(
                    instructions = arrayOf(lockFeeInstruction(addressToLockFee)) + instructions.instructions
                )
        }
        is ManifestInstructions.StringInstructions -> {}
    }
    return copy(updatedInstructions, blobs)
}

fun TransactionManifest.addGuaranteeInstructionToManifest(
    address: String,
    guaranteedAmount: String,
    index: Int
): TransactionManifest {
    val instructions = instructions
    var updatedManifestInstructions = instructions
    when (instructions) {
        is ManifestInstructions.ParsedInstructions -> {
            val guaranteeInstruction = guaranteeInstruction(
                resourceAddress = address,
                guaranteedAmount = guaranteedAmount
            )

            val updatedInstructions = instructions.instructions.toMutableList()
            updatedInstructions.add(index, guaranteeInstruction)

            updatedManifestInstructions = instructions.copy(
                instructions = updatedInstructions.toTypedArray()
            )
        }
        is ManifestInstructions.StringInstructions -> {}
    }
    return copy(updatedManifestInstructions, blobs)
}

fun TransactionManifest.convertManifestInstructionsToString(
    networkId: Int
): Result<ConvertManifestResponse> {
    return try {
        Result.success(
            RadixEngineToolkit.convertManifest(
                ConvertManifestRequest(
                    networkId = networkId.toUByte(),
                    instructionsOutputKind = ManifestInstructionsKind.String,
                    manifest = this
                )
            ).getOrThrow()
        )
    } catch (e: Exception) {
        Result.failure(
            DappRequestException(
                DappRequestFailure.TransactionApprovalFailure.ConvertManifest,
                e.message,
                e
            )
        )
    }
}

private fun lockFeeInstruction(
    addressToLockFee: String
): Instruction {
    return Instruction.CallMethod(
        componentAddress = ManifestAstValue.Address(addressToLockFee),
        methodName = ManifestAstValue.String(MethodName.LockFee.stringValue),
        arguments = arrayOf(
            ManifestAstValue.Decimal(
                BigDecimal.valueOf(TransactionConfig.DEFAULT_LOCK_FEE)
            )
        )
    )
}

private fun guaranteeInstruction(
    resourceAddress: String,
    guaranteedAmount: String
): Instruction {
    return Instruction.AssertWorktopContainsByAmount(
        resourceAddress = ManifestAstValue.Address(resourceAddress),
        amount = ManifestAstValue.Decimal(guaranteedAmount)
    )
}

fun faucetComponentAddress(
    networkId: UByte
): ManifestAstValue.Address {
    val faucetComponentAddress = RadixEngineToolkit.knownEntityAddresses(
        request = KnownEntityAddressesRequest(
            networkId = networkId
        )
    ).getOrThrow().faucetPackageAddress
    return ManifestAstValue.Address(faucetComponentAddress)
}

fun FactorInstance.PublicKey.curveKindScryptoDiscriminator(): EnumDiscriminator.U8 {
    return when (curve) {
        Slip10Curve.SECP_256K1 -> EnumDiscriminator.U8(0x00u)
        Slip10Curve.CURVE_25519 -> EnumDiscriminator.U8(0x01u)
    }
}

private val publicKeyDiscriminator = EnumDiscriminator.U8(0x09u)
private val metadataEntryDiscriminator = EnumDiscriminator.U8(0x01u)

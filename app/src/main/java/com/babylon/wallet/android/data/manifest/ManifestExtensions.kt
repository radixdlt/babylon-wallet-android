package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.data.transaction.MethodName
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.ManifestAstValue
import com.radixdlt.toolkit.models.request.KnownEntityAddressesRequest
import com.radixdlt.toolkit.models.transaction.ManifestInstructions
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import rdx.works.profile.derivation.model.NetworkId
import java.math.BigDecimal

/**
 * Instruction to add free xrd from given address
 */
fun ManifestBuilder.addFreeXrdInstruction(
    networkId: NetworkId
): ManifestBuilder {
    return addInstruction(
        Instruction.CallMethod(
            componentAddress = faucetComponentAddress(networkId.value.toUByte()),
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
                address = recipientComponentAddress
            ),
            methodName = ManifestAstValue.String(MethodName.DepositBatch.stringValue),
            arguments = arrayOf(ManifestAstValue.Expression("ENTIRE_WORKTOP"))
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

/**
 * Withdraw instruction from given component address
 * @param withdrawComponentAddress the address we want to withdraw from
 * @param tokenResourceAddress identifier of which token we want to transfer
 * @param amount Amount of tokens we want to withdraw
 */
fun ManifestBuilder.addWithdrawInstruction(
    withdrawComponentAddress: String,
    tokenResourceAddress: String,
    amount: String
): ManifestBuilder {
    return addInstruction(
        Instruction.CallMethod(
            componentAddress = ManifestAstValue.Address(withdrawComponentAddress),
            methodName = ManifestAstValue.String(MethodName.Withdraw.stringValue),
            arrayOf(ManifestAstValue.Decimal(amount), ManifestAstValue.Address(tokenResourceAddress))
        )
    )
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

fun faucetComponentAddress(
    networkId: UByte
): ManifestAstValue.Address {
    val faucetComponentAddress = RadixEngineToolkit.knownEntityAddresses(
        request = KnownEntityAddressesRequest(
            networkId = networkId
        )
    ).getOrThrow().faucetComponentAddress
    return ManifestAstValue.Address(faucetComponentAddress.toString())
}

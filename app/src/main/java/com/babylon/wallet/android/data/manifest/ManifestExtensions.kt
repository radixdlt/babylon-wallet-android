package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.data.transaction.MethodName
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.Value
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
            componentAddress = Value.ComponentAddress.faucetComponentAddress(
                networkId = networkId.value.toUByte()
            ),
            methodName = Value.String(MethodName.Free.stringValue)
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
            componentAddress = Value.ComponentAddress(
                address = recipientComponentAddress
            ),
            methodName = Value.String(MethodName.DepositBatch.stringValue),
            arguments = arrayOf(Value.Expression("ENTIRE_WORKTOP"))
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
            componentAddress = Value.ComponentAddress(withdrawComponentAddress),
            methodName = Value.String(MethodName.WithdrawByAmount.stringValue),
            arrayOf(Value.Decimal(amount), Value.ResourceAddress(tokenResourceAddress))
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
        componentAddress = Value.ComponentAddress(addressToLockFee),
        methodName = Value.String(MethodName.LockFee.stringValue),
        arguments = arrayOf(
            Value.Decimal(
                BigDecimal.valueOf(TransactionConfig.DEFAULT_LOCK_FEE)
            )
        )
    )
}

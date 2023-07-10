@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instruction
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestExpression
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.ManifestValueKind
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.compressedPublicKeyHashBytes
import rdx.works.core.ret.ManifestBuilder
import rdx.works.core.ret.ManifestMethod
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.FactorInstance
import java.math.BigDecimal
import java.util.UUID

/**
 * Instruction to add free xrd from given address
 */
fun ManifestBuilder.addFreeXrdInstruction(
    faucetComponentAddress: String
): ManifestBuilder = callMethod(
    componentAddress = ManifestValue.AddressValue(Address(faucetComponentAddress)),
    methodName = ManifestValue.StringValue(ManifestMethod.Free.value)
)

/**
 * Instruction to Deposit withdrawn tokens
 * @param recipientComponentAddress component we want to deposit on
 */
fun ManifestBuilder.addDepositBatchInstruction(
    recipientComponentAddress: String
): ManifestBuilder = callMethod(
    componentAddress = ManifestValue.AddressValue(Address(recipientComponentAddress)),
    methodName = ManifestValue.StringValue(ManifestMethod.TryDepositBatchOrAbort.value),
    arguments = ManifestValue.ArrayValue(
        elementValueKind = ManifestValueKind.EXPRESSION_VALUE,
        elements = listOf(ManifestValue.ExpressionValue(ManifestExpression.ENTIRE_WORKTOP))
    )
)

/**
 * Lock fee instruction
 */
fun ManifestBuilder.addLockFeeInstruction(
    addressToLockFee: String,
    fee: BigDecimal
): ManifestBuilder = addInstruction(lockFeeInstruction(addressToLockFee, fee))

fun TransactionManifest.addLockFeeInstructionToManifest(
    addressToLockFee: String,
    fee: BigDecimal
): TransactionManifest = TransactionManifest(
    instructions = Instructions.fromInstructions(
        instructions = listOf(lockFeeInstruction(addressToLockFee, fee)) + instructions().instructionsList(),
        networkId = instructions().networkId()
    ),
    blobs = blobs()
)

fun TransactionManifest.addGuaranteeInstructionToManifest(
    address: String,
    guaranteedAmount: String,
    index: Int
): TransactionManifest = TransactionManifest(
    instructions = Instructions.fromInstructions(
        instructions = instructions().instructionsList().toMutableList().apply {
            add(
                index, guaranteeInstruction(
                    resourceAddress = address,
                    guaranteedAmount = guaranteedAmount
                )
            )
        }.toList(),
        networkId = instructions().networkId()
    ),
    blobs = blobs()
)

private fun lockFeeInstruction(
    addressToLockFee: String,
    fee: BigDecimal
): Instruction {
    return Instruction.CallMethod(
        address = Address(addressToLockFee),
        methodName = ManifestMethod.LockFee.value,
        args = ManifestValue.ArrayValue(
            elementValueKind = ManifestValueKind.DECIMAL_VALUE,
            elements = listOf(ManifestValue.DecimalValue(Decimal(fee.toPlainString())))
        )
    )
}

private fun guaranteeInstruction(
    resourceAddress: String,
    guaranteedAmount: String
): Instruction {
    return Instruction.AssertWorktopContains(
        resourceAddress = Address(resourceAddress),
        amount = Decimal(guaranteedAmount)
    )
}

fun TransactionManifest.toTransactionRequest(
    networkId: Int,
    requestId: String = UUID.randomUUID().toString(),
    message: String? = null
) = MessageFromDataChannel.IncomingRequest.TransactionRequest(
    dappId = "",
    requestId = requestId,
    transactionManifestData = TransactionManifestData.from(
        manifest = this,
        networkId = networkId,
        message = message
    ),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(networkId)
)

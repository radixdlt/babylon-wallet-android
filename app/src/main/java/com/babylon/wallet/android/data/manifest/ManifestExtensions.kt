@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instruction
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.ret.ManifestMethod
import java.lang.StringBuilder
import java.math.BigDecimal
import java.util.UUID

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
                index,
                guaranteeInstruction(
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
        args = ManifestValue.TupleValue(
            fields = listOf(
                ManifestValue.DecimalValue(Decimal(fee.toPlainString()))
            )
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

fun TransactionManifest.prepareInternalTransactionRequest(
    networkId: Int,
    requestId: String = UUID.randomUUID().toString(),
    message: String? = null
) = MessageFromDataChannel.IncomingRequest.TransactionRequest(
    // Since we mock this request as a dApp request from the wallet app, the dApp's id is empty. Should never be invoked as we always
    // check if a request is not internal before sending message to the dApp
    dappId = "",
    requestId = requestId,
    transactionManifestData = TransactionManifestData.from(
        manifest = this,
        networkId = networkId,
        message = message
    ),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(networkId)
)

fun TransactionHeader.toPrettyString(): String = StringBuilder()
    .appendLine("[Start Epoch]         => $startEpochInclusive")
    .appendLine("[End Epoch]           => $endEpochExclusive")
    .appendLine("[Network id]          => $networkId")
    .appendLine("[Nonce]               => $nonce")
    .appendLine("[Notary is signatory] => $notaryIsSignatory")
    .appendLine("[Tip %]               => $tipPercentage")
    .toString()

fun TransactionManifest.toPrettyString(): String {
    val blobSeparator = "\n"
    val blobPreamble = "BLOBS\n"
    val blobLabel = "BLOB\n"

    val instructionsFormatted = instructions().asStr()

    val blobsByByteCount = blobs().mapIndexed { index, bytes ->
        "$blobLabel[$index]: #${bytes.size} bytes"
    }.joinToString(blobSeparator)

    val blobsString = if (blobsByByteCount.isNotEmpty()) {
        listOf(blobPreamble, blobsByByteCount).joinToString(separator = blobSeparator)
    } else {
        ""
    }

    return "$instructionsFormatted$blobsString"
}

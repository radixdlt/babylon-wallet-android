@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.utils.toRETDecimalString
import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instruction
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestAddress
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionManifest
import rdx.works.profile.ret.ManifestMethod
import rdx.works.core.toRETDecimal
import java.math.BigDecimal
import java.math.RoundingMode
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

fun TransactionManifest.addAssertions(
    depositing: List<Transferable.Depositing>
): TransactionManifest {
    var startIndex = 0
    var manifest = this

    depositing.forEach {
        when (val assertion = it.guaranteeAssertion) {
            is GuaranteeAssertion.ForAmount -> {
                manifest = manifest.addGuaranteeInstructionToManifest(
                    address = it.transferable.resourceAddress,
                    guaranteedAmount = assertion.amount.toRETDecimalString(),
                    index = assertion.instructionIndex.toInt() + startIndex
                )
                startIndex++
            }

            is GuaranteeAssertion.ForNFT -> {
                // Will be implemented later
            }

            null -> {}
        }
    }

    return manifest
}

private fun lockFeeInstruction(
    addressToLockFee: String,
    fee: BigDecimal
): Instruction {
    return Instruction.CallMethod(
        address = ManifestAddress.Static(Address(addressToLockFee)),
        methodName = ManifestMethod.LockFee.value,
        args = ManifestValue.TupleValue(
            fields = listOf(
                ManifestValue.DecimalValue(fee.toRETDecimal(roundingMode = RoundingMode.HALF_UP))
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
    message: String? = null,
    blockUntilCompleted: Boolean = false,
    transactionType: TransactionType = TransactionType.Generic
) = MessageFromDataChannel.IncomingRequest.TransactionRequest(
    // Since we mock this request as a dApp request from the wallet app, the dApp's id is empty. Should never be invoked as we always
    // check if a request is not internal before sending message to the dApp
    remoteConnectorId = "",
    requestId = requestId,
    transactionManifestData = TransactionManifestData.from(
        manifest = this,
        networkId = networkId,
        message = message
    ),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(networkId, blockUntilCompleted),
    transactionType = transactionType
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

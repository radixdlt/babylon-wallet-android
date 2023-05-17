@file:Suppress("TooGenericExceptionThrown")

package com.babylon.wallet.android.data.transaction

import com.radixdlt.toolkit.models.transaction.ManifestInstructions
import com.radixdlt.toolkit.models.transaction.TransactionManifest

fun TransactionManifest.toPrettyString(): String {
    if (instructions is ManifestInstructions.ParsedInstructions) return ""
    val blobSeparator = "\n"
    val blobPreamble = "BLOBS\n"
    val blobLabel = "BLOB\n"
    val instructionsSeparator = "\n\n"
    val instructionsArgumentSeparator = "\n\t"

    val instructionsFormatted = (instructions as ManifestInstructions.StringInstructions).let { stringInstructions ->
        stringInstructions.instructions.trim().removeSuffix(";").split(";").map { "${it.trim()};" }
            .joinToString(separator = instructionsSeparator) { instruction ->
                instruction.split(" ").filter { it.isNotEmpty() }
                    .joinToString(separator = instructionsArgumentSeparator)
            }
    }

    val blobsByByteCount = blobs?.mapIndexed { index, bytes ->
        "$blobLabel[$index]: #${bytes.size} bytes"
    }?.joinToString(blobSeparator).orEmpty()

    val blobsString = if (blobsByByteCount.isNotEmpty()) {
        listOf(blobPreamble, blobsByByteCount).joinToString(separator = blobSeparator)
    } else {
        ""
    }

    return "$instructionsFormatted$blobsString"
}

fun TransactionManifest.toStringWithoutBlobs(): String {
    if (instructions is ManifestInstructions.ParsedInstructions) return ""
    val instructionsSeparator = "\n\n"
    val instructionsArgumentSeparator = "\n\t"

    val instructionsFormatted = (instructions as ManifestInstructions.StringInstructions).let { stringInstructions ->
        stringInstructions.instructions.trim().removeSuffix(";").split(";").map { "${it.trim()};" }
            .joinToString(separator = instructionsSeparator) { instruction ->
                instruction.split(" ").filter { it.isNotEmpty() }
                    .joinToString(separator = instructionsArgumentSeparator)
            }
    }

    return instructionsFormatted
}

fun TransactionManifest.readInstructions(): String {
    if (instructions is ManifestInstructions.ParsedInstructions) return ""
    val instructionsSeparator = "\n\n"
    val instructionsArgumentSeparator = "\n\t"

    val instructionsFormatted = (instructions as ManifestInstructions.StringInstructions).let { stringInstructions ->
        stringInstructions.instructions.trim().removeSuffix(";").split(";").map { "${it.trim()};" }
            .joinToString(separator = instructionsSeparator) { instruction ->
                instruction.split(" ").filter { it.isNotEmpty() }
                    .joinToString(separator = instructionsArgumentSeparator)
            }
    }

    return instructionsFormatted
}

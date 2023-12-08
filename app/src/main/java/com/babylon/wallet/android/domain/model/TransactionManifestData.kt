package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.transaction.TransactionVersion
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.TransactionManifest

data class TransactionManifestData(
    val instructions: String,
    val version: Long,
    val networkId: Int,
    val blobs: List<ByteArray> = emptyList(),
    val message: String? = null
) {

    fun toTransactionManifest() = runCatching {
        TransactionManifest(
            instructions = Instructions.fromString(
                string = instructions,
                networkId = networkId.toUByte()
            ),
            blobs = blobs
        )
    }

    companion object {
        fun from(
            manifest: TransactionManifest,
            networkId: Int,
            message: String?
        ) = TransactionManifestData(
            instructions = manifest.instructions().asStr(),
            version = TransactionVersion.Default.value,
            networkId = networkId,
            blobs = manifest.blobs(),
            message = message
        )
    }
}

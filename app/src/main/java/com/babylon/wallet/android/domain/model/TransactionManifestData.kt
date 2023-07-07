package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.transaction.TransactionVersion
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.toByteArray
import rdx.works.core.toUByteList

data class TransactionManifestData(
    val instructions: String,
    val version: Long,
    val networkId: Int,
    val blobs: List<ByteArray> = emptyList(),
    val message: String? = null
) {

    fun toTransactionManifest() = with(blobs.map { it.toUByteList() }) {
        TransactionManifest(
            instructions = Instructions.fromString(
                string = instructions,
                blobs = this,
                networkId = networkId.toUByte()
            ),
            blobs = this
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
            blobs = manifest.blobs().map { it.toByteArray() },
            message = message
        )
    }

}

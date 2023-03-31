package com.babylon.wallet.android.domain.model

data class TransactionManifestData(
    val instructions: String,
    val version: Long,
    val networkId: Int,
    val blobs: List<ByteArray> = emptyList(),
    val message: String? = null
)

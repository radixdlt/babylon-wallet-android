package com.babylon.wallet.android.domain.model.metadata

/**
 * The common denominator for all metadata items, either
 * known ones, or new ones defined by dApp developers.
 */
interface MetadataItem {
    val key: String
}

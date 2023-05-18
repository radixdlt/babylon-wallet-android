package com.babylon.wallet.android.domain.model.metadata

/**
 * The common denominator for all metadata items, either
 * known ones, or new ones defined by dApp developers.
 */
interface MetadataItem {
    val key: String

    companion object {
        inline fun <reified T : MetadataItem> MutableList<MetadataItem>.consume(): T? {
            val item = find { it is T } as? T

            if (item != null) {
                remove(item)
            }

            return item
        }
    }
}

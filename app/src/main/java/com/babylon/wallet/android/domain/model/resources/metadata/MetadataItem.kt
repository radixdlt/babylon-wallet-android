package com.babylon.wallet.android.domain.model.resources.metadata

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

/**
 * Generic [MetadataItem] that the [key] is not included in the
 * [ExplicitMetadataKey]s and whose [value] can be represented as a [String]
 */
data class StringMetadataItem(
    override val key: String,
    val value: String
) : MetadataItem

/**
 * More complex types as Maps or Tuples that are previewed as complex.
 */
data class ComplexMetadataItem(
    override val key: String
) : MetadataItem

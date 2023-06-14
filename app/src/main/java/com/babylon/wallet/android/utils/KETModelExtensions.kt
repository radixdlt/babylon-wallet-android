package com.babylon.wallet.android.utils

import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.domain.usecases.transaction.ResourceRequest
import com.radixdlt.toolkit.models.request.MetadataEntry
import com.radixdlt.toolkit.models.request.MetadataValue
import com.radixdlt.toolkit.models.request.NewlyCreated
import com.radixdlt.toolkit.models.request.ResourceManagerSpecifier
import com.radixdlt.toolkit.models.request.ResourceQuantifier

fun ResourceQuantifier.toResourceRequest(newlyCreated: NewlyCreated): ResourceRequest {
    return when (this) {
        is ResourceQuantifier.Amount -> {
            when (val resAddress = resourceAddress) {
                is ResourceManagerSpecifier.Existing -> {
                    ResourceRequest.Existing(
                        resAddress.address
                    )
                }
                is ResourceManagerSpecifier.NewlyCreated -> {
                    ResourceRequest.NewlyCreated(newlyCreated.resources[resAddress.index.toInt()].metadata)
                }
            }
        }
        is ResourceQuantifier.Ids -> {
            when (val resAddress = resourceAddress) {
                is ResourceManagerSpecifier.Existing -> {
                    ResourceRequest.Existing(
                        resAddress.address
                    )
                }
                is ResourceManagerSpecifier.NewlyCreated -> {
                    ResourceRequest.NewlyCreated(newlyCreated.resources[resAddress.index.toInt()].metadata)
                }
            }
        }
    }
}

fun ResourceRequest.NewlyCreated.tokenSymbol(): String? {
    return when (val entry = this.metadata.firstOrNull { it.key == MetadataConstants.KEY_SYMBOL }?.value) {
        is MetadataEntry.Value -> when (val value = entry.value) {
            is MetadataValue.String -> value.value
            else -> null
        }
        else -> null
    }
}

fun ResourceRequest.NewlyCreated.iconUrl(): String? {
    return when (val entry = this.metadata.firstOrNull { it.key == MetadataConstants.KEY_ICON }?.value) {
        is MetadataEntry.Value -> when (val value = entry.value) {
            is MetadataValue.String -> value.value
            else -> null
        }
        else -> null
    }
}

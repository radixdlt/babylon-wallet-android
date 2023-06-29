package com.babylon.wallet.android.utils

import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.domain.usecases.transaction.ResourceRequest
import com.radixdlt.toolkit.models.method.MetadataValue
import com.radixdlt.toolkit.models.method.NewlyCreated
import com.radixdlt.toolkit.models.method.ResourceManagerSpecifier
import com.radixdlt.toolkit.models.method.ResourceQuantifier

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
    return when (val value = this.metadata.firstOrNull { it.key == MetadataConstants.KEY_SYMBOL }?.value) {
        is MetadataValue.String -> value.value
        else -> null
    }
}

fun ResourceRequest.NewlyCreated.iconUrl(): String? {
    return when (val entry = this.metadata.firstOrNull { it.key == MetadataConstants.KEY_ICON }?.value) {
        is MetadataValue.String -> entry.value
        else -> null
    }
}

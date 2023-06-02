package com.babylon.wallet.android.utils

import com.babylon.wallet.android.domain.usecases.transaction.ResourceRequest
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

fun ResourceQuantifier.toAmount(): String {
    return when (this) {
        is ResourceQuantifier.Amount -> {
            amount
        }
        is ResourceQuantifier.Ids -> {
            ""
        }
    }
}

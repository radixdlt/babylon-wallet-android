package com.babylon.wallet.android.domain.model.resources

import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata

data class Pool(
    val address: String,
    val metadata: List<Metadata>,
    val resources: List<Resource.FungibleResource>,
    val associatedDApp: DApp? = null
)

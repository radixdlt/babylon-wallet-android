package com.babylon.wallet.android.data.transaction.model

import rdx.works.profile.data.model.pernetwork.Network

data class FeePayerSearchResult(
    val feePayerAddressFromManifest: String? = null,
    val candidates: List<Network.Account> = emptyList()
) {

    val feePayerExistsInManifest: Boolean
        get() = feePayerAddressFromManifest != null

}

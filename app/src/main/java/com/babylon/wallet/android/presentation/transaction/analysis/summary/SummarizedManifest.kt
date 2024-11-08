package com.babylon.wallet.android.presentation.transaction.analysis.summary

import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.SubintentManifest
import com.radixdlt.sargon.TransactionManifest

sealed interface SummarizedManifest {

    val networkId: NetworkId

    data class Transaction(val manifest: TransactionManifest) : SummarizedManifest {
        override val networkId: NetworkId
            get() = manifest.networkId
    }

    data class Subintent(val manifest: SubintentManifest) : SummarizedManifest {
        override val networkId: NetworkId
            get() = manifest.networkId
    }
}

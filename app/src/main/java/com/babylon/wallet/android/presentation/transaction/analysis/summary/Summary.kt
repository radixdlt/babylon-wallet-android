package com.babylon.wallet.android.presentation.transaction.analysis.summary

import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ManifestSummary
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.SubintentManifest
import com.radixdlt.sargon.TransactionManifest

sealed interface Summary {

    val entitiesRequiringAuth: List<AddressOfAccountOrPersona>
    val networkId: NetworkId

    data class FromExecution(
        val manifest: SummarizedManifest,
        val summary: ExecutionSummary
    ): Summary {

        override val entitiesRequiringAuth: List<AddressOfAccountOrPersona>
            get() = summary.addressesOfAccountsRequiringAuth.map {
                AddressOfAccountOrPersona.Account(it)
            } + summary.addressesOfIdentitiesRequiringAuth.map {
                AddressOfAccountOrPersona.Identity(it)
            }

        override val networkId: NetworkId
            get() = manifest.networkId
    }

    data class FromStaticAnalysis(
        val manifest: SummarizedManifest.Subintent,
        val summary: ManifestSummary
    ): Summary {
        override val entitiesRequiringAuth: List<AddressOfAccountOrPersona>
            get() = summary.addressesOfAccountsRequiringAuth.map {
                AddressOfAccountOrPersona.Account(it)
            } + summary.addressesOfPersonasRequiringAuth.map {
                AddressOfAccountOrPersona.Identity(it)
            }
        override val networkId: NetworkId
            get() = manifest.networkId
    }

}

sealed interface SummarizedManifest {

    val networkId: NetworkId

    data class Transaction(val manifest: TransactionManifest): SummarizedManifest {
        override val networkId: NetworkId
            get() = manifest.networkId
    }

    data class Subintent(val manifest: SubintentManifest): SummarizedManifest {
        override val networkId: NetworkId
            get() = manifest.networkId
    }
}
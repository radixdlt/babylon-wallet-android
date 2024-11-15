package com.babylon.wallet.android.presentation.transaction.analysis.summary

import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ManifestSummary
import com.radixdlt.sargon.NetworkId

sealed interface Summary {

    val entitiesRequiringAuth: List<AddressOfAccountOrPersona>
    val networkId: NetworkId

    data class FromExecution(
        val manifest: SummarizedManifest,
        val summary: ExecutionSummary
    ) : Summary {

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
    ) : Summary {
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

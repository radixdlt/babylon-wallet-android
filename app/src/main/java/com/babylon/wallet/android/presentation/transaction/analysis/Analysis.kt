package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asProfileEntity
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.core.sargon.securityStateAccessControllerAddress

class Analysis private constructor(
    val previewType: PreviewType,
    val summary: Summary,
    val signers: List<ProfileEntity>
) {

    constructor(
        previewType: PreviewType,
        summary: Summary,
        profile: Profile
    ) : this(
        previewType = previewType,
        summary = summary,
        signers = summary.entitiesRequiringAuth.mapNotNull { address ->
            when (address) {
                is AddressOfAccountOrPersona.Account -> profile.activeAccountsOnCurrentNetwork.find {
                    it.address == address.v1
                }?.asProfileEntity()

                is AddressOfAccountOrPersona.Identity -> profile.activePersonasOnCurrentNetwork.find {
                    it.address == address.v1
                }?.asProfileEntity()
            }
        } + (summary.detailedManifestClass as? DetailedManifestClass.AccessControllerRecovery)?.let { classification ->
            classification.acAddresses.mapNotNull { acAddress ->
                profile.activeAccountsOnCurrentNetwork.find { account ->
                    account.securityStateAccessControllerAddress == acAddress
                }?.asProfileEntity() ?: profile.activePersonasOnCurrentNetwork.find { persona ->
                    persona.securityStateAccessControllerAddress == acAddress
                }?.asProfileEntity()
            }
        }.orEmpty()
    )
}

package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asProfileEntity
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork

class Analysis private constructor(
    val previewType: PreviewType,
    val summary: Summary,
    val signers: List<ProfileEntity>
) {

    constructor(
        previewType: PreviewType,
        summary: Summary,
        profile: Profile,
        accountToDelete: Account? = null
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
        }
    )
}

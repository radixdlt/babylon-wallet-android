package com.babylon.wallet.android.domain.model.p2plink

import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.P2PLinkPurpose

data class LinkConnectionPayload(
    val password: String,
    val publicKey: String,
    val purpose: P2PLinkPurpose,
    val existingP2PLink: P2PLink?
)

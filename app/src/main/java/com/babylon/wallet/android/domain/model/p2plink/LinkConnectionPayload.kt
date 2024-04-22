package com.babylon.wallet.android.domain.model.p2plink

import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.RadixConnectPassword
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.P2PLinkPurpose

data class LinkConnectionPayload(
    val password: RadixConnectPassword,
    val publicKey: PublicKey.Ed25519,
    val purpose: P2PLinkPurpose,
    val existingP2PLink: P2PLink?
)

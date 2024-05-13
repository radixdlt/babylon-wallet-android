package com.babylon.wallet.android.domain.model.p2plink

import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.RadixConnectPurpose

data class LinkConnectionPayload(
    val password: RadixConnectPassword,
    val publicKey: PublicKey.Ed25519,
    val purpose: RadixConnectPurpose,
    val existingP2PLink: P2pLink?
)

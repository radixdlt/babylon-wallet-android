package com.babylon.wallet.android.data.repository.p2plink

import rdx.works.profile.data.model.apppreferences.P2PLink

fun List<P2PLink>.findBy(publicKey: String): P2PLink? {
    return firstOrNull { it.isSame(publicKey) }
}

fun P2PLink.isSame(other: P2PLink): Boolean {
    return isSame(other.publicKey)
}

fun P2PLink.isSame(publicKey: String): Boolean {
    return publicKey.equals(publicKey, false)
}
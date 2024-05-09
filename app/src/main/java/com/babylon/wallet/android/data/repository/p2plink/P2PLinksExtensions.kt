package com.babylon.wallet.android.data.repository.p2plink

import rdx.works.profile.data.model.apppreferences.P2PLink

fun List<P2PLink>.findBy(id: String): P2PLink? {
    return firstOrNull { it.isSame(id) }
}

fun P2PLink.isSame(other: P2PLink): Boolean {
    return isSame(other.id)
}

fun P2PLink.isSame(id: String): Boolean {
    return this.id.equals(id, true)
}

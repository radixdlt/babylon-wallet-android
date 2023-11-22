package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.apppreferences.Radix

data class NetworkInfo(
    val network: Radix.Network,
    val epoch: Long
)

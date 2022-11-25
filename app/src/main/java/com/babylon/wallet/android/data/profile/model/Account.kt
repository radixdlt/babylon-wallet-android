package com.babylon.wallet.android.data.profile.model

//TODO this should be removed and replaced by Account class in Profile when doing account screen
data class Account(
    val name: String,
    val address: Address,
    val value: String,
    val currency: String
)

package com.babylon.wallet.android.data.profile

data class Account(
    val name: String,
    val address: Address,
    val value: String,
    val currency: String
)

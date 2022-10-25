package com.babylon.wallet.android.data.profile

data class Profile(
    val address: Address,
    val accounts: List<String>,
    val name: String
)
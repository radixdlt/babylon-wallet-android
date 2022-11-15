package com.babylon.wallet.android.data.profile.model

data class Profile(
    val address: Address,
    val accounts: List<String>,
    val name: String
)

package com.babylon.wallet.android.data.dapp

data class DAppWellKnownResponse(
    val dApps: List<DApp>
)

data class DApp(
    val definitionAddress: String,
    val id: String
)
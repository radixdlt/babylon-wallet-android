package com.babylon.wallet.android.data.dapp.model

//TODO this will be sent back to WebRtc
data class ResponseMethodWalletResponse(
    val accountAddresses: List<AccountAddress>
)

data class AccountAddress(
    val label: String,
    val address: String
)
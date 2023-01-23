package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.model.MessageFromDataChannel

val accountsRequest = MessageFromDataChannel.IncomingRequest.AccountsRequest(
    requestId = "requestId",
    isOngoing = false,
    requiresProofOfOwnership = false,
    numberOfAccounts = 2,
    quantifier = MessageFromDataChannel.AccountNumberQuantifier.Exactly
)

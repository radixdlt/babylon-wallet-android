package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import java.util.UUID

val accountsRequest = MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
    requestId = UUID.randomUUID().toString(),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", ""),
    oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        requiresProofOfOwnership = false,
        numberOfAccounts = 2,
        quantifier = MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.Exactly
    )
)

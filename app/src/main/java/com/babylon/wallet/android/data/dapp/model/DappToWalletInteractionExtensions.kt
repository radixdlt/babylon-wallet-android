package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.model.messages.WalletUnauthorizedRequest
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.radixdlt.sargon.DappToWalletInteractionAccountsRequestItem
import com.radixdlt.sargon.DappToWalletInteractionAuthRequestItem
import com.radixdlt.sargon.DappToWalletInteractionAuthorizedRequestItems
import com.radixdlt.sargon.DappToWalletInteractionItems
import com.radixdlt.sargon.DappToWalletInteractionPersonaDataRequestItem
import com.radixdlt.sargon.DappToWalletInteractionSendTransactionItem
import com.radixdlt.sargon.DappToWalletInteractionSubintentRequestItem
import com.radixdlt.sargon.DappToWalletInteractionUnauthorizedRequestItems
import com.radixdlt.sargon.DappToWalletInteractionUnvalidated
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.RequestedQuantity
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.toList
import rdx.works.core.mapError

fun DappToWalletInteractionUnvalidated.toDomainModel(remoteEntityId: RemoteEntityID) = runCatching {
    val metadata = DappToWalletInteraction.RequestMetadata(
        networkId = metadata.networkId,
        origin = metadata.origin,
        dAppDefinitionAddress = metadata.dappDefinitionAddress,
        isInternal = false
    )
    when (val itemsTemp = items) {
        is DappToWalletInteractionItems.AuthorizedRequest -> {
            itemsTemp.v1.parseAuthorizedRequest(remoteEntityId, interactionId, metadata)
        }

        is DappToWalletInteractionItems.Transaction -> {
            itemsTemp.v1.send.toDomainModel(remoteEntityId, interactionId, metadata)
        }

        is DappToWalletInteractionItems.UnauthorizedRequest -> {
            itemsTemp.v1.parseUnauthorizedRequest(remoteEntityId, interactionId, metadata)
        }

        is DappToWalletInteractionItems.PreAuthorization -> {
            itemsTemp.v1.request.toDomainModel(remoteEntityId, interactionId, metadata)
        }
    }
}.mapError {
    RadixWalletException.IncomingMessageException.MessageParse(it)
}

fun DappToWalletInteractionSendTransactionItem.toDomainModel(
    remoteConnectorId: RemoteEntityID,
    requestId: WalletInteractionId,
    metadata: DappToWalletInteraction.RequestMetadata
) = TransactionRequest(
    remoteEntityId = remoteConnectorId,
    interactionId = requestId,
    unvalidatedManifestData = UnvalidatedManifestData(
        instructions = unvalidatedManifest.transactionManifestString,
        networkId = metadata.networkId,
        plainMessage = message,
        blobs = unvalidatedManifest.blobs.toList().map { it.bytes },
    ),
    requestMetadata = metadata
)

fun DappToWalletInteractionSubintentRequestItem.toDomainModel(
    remoteConnectorId: RemoteEntityID,
    requestId: WalletInteractionId,
    metadata: DappToWalletInteraction.RequestMetadata
) = TransactionRequest(
    remoteEntityId = remoteConnectorId,
    interactionId = requestId,
    unvalidatedManifestData = UnvalidatedManifestData(
        instructions = unvalidatedManifest.subintentManifestString,
        networkId = metadata.networkId,
        plainMessage = message,
        blobs = unvalidatedManifest.blobs.toList().map { it.bytes },
    ),
    requestMetadata = metadata,
    transactionType = TransactionType.PreAuthorized(
        expiration = expiration
    )
)

private fun DappToWalletInteractionUnauthorizedRequestItems.parseUnauthorizedRequest(
    remoteEntityId: RemoteEntityID,
    requestId: WalletInteractionId,
    metadata: DappToWalletInteraction.RequestMetadata
): WalletUnauthorizedRequest {
    return WalletUnauthorizedRequest(
        remoteEntityId = remoteEntityId,
        interactionId = requestId,
        requestMetadata = metadata,
        oneTimeAccountsRequestItem = oneTimeAccounts?.toDomainModel(false),
        oneTimePersonaDataRequestItem = oneTimePersonaData?.toDomainModel(false)
    )
}

fun DappToWalletInteractionPersonaDataRequestItem.toDomainModel(
    isOngoing: Boolean = false
): DappToWalletInteraction.PersonaDataRequestItem {
    return DappToWalletInteraction.PersonaDataRequestItem(
        isRequestingName = isRequestingName == true,
        numberOfRequestedEmailAddresses = numberOfRequestedEmailAddresses?.toDomainModel(),
        numberOfRequestedPhoneNumbers = numberOfRequestedPhoneNumbers?.toDomainModel(),
        isOngoing = isOngoing
    )
}

fun DappToWalletInteractionAccountsRequestItem.toDomainModel(
    isOngoing: Boolean = true
): DappToWalletInteraction.AccountsRequestItem? {
    // correct request but not actionable, return null
    if (numberOfAccounts.quantity.toInt() == 0 &&
        numberOfAccounts.quantifier == RequestedNumberQuantifier.EXACTLY
    ) {
        return null
    }
    return DappToWalletInteraction.AccountsRequestItem(
        isOngoing = isOngoing,
        numberOfValues = numberOfAccounts.toDomainModel(),
        challenge = challenge
    )
}

fun RequestedQuantity.toDomainModel(): DappToWalletInteraction.NumberOfValues {
    return when (quantifier) {
        RequestedNumberQuantifier.EXACTLY -> {
            DappToWalletInteraction.NumberOfValues(
                quantity.toInt(),
                DappToWalletInteraction.NumberOfValues.Quantifier.Exactly
            )
        }

        RequestedNumberQuantifier.AT_LEAST -> {
            DappToWalletInteraction.NumberOfValues(
                quantity.toInt(),
                DappToWalletInteraction.NumberOfValues.Quantifier.AtLeast
            )
        }
    }
}

private fun DappToWalletInteractionAuthorizedRequestItems.parseAuthorizedRequest(
    remoteEntityId: RemoteEntityID,
    interactionId: WalletInteractionId,
    metadata: DappToWalletInteraction.RequestMetadata
): DappToWalletInteraction {
    val authDomainModel = when (val auth = auth) {
        is DappToWalletInteractionAuthRequestItem.LoginWithChallenge -> {
            WalletAuthorizedRequest.AuthRequestItem.LoginRequest.WithChallenge(
                auth.v1.challenge
            )
        }

        DappToWalletInteractionAuthRequestItem.LoginWithoutChallenge -> {
            WalletAuthorizedRequest.AuthRequestItem.LoginRequest.WithoutChallenge
        }

        is DappToWalletInteractionAuthRequestItem.UsePersona -> {
            WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest(
                auth.v1.identityAddress
            )
        }
    }
    return WalletAuthorizedRequest(
        remoteEntityId = remoteEntityId,
        interactionId = interactionId,
        requestMetadata = metadata,
        authRequestItem = authDomainModel,
        oneTimeAccountsRequestItem = oneTimeAccounts?.toDomainModel(isOngoing = false),
        ongoingAccountsRequestItem = ongoingAccounts?.toDomainModel(isOngoing = true),
        oneTimePersonaDataRequestItem = oneTimePersonaData?.toDomainModel(isOngoing = false),
        ongoingPersonaDataRequestItem = ongoingPersonaData?.toDomainModel(isOngoing = true),
        resetRequestItem = reset
    )
}

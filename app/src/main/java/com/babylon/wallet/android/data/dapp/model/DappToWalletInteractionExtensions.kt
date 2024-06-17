package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.IncomingMessage.IncomingRequest.PersonaRequestItem
import com.radixdlt.sargon.DappToWalletInteractionAccountsRequestItem
import com.radixdlt.sargon.DappToWalletInteractionAuthRequestItem
import com.radixdlt.sargon.DappToWalletInteractionAuthorizedRequestItems
import com.radixdlt.sargon.DappToWalletInteractionItems
import com.radixdlt.sargon.DappToWalletInteractionPersonaDataRequestItem
import com.radixdlt.sargon.DappToWalletInteractionSendTransactionItem
import com.radixdlt.sargon.DappToWalletInteractionUnauthorizedRequestItems
import com.radixdlt.sargon.DappToWalletInteractionUnvalidated
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.RequestedQuantity
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.toList
import rdx.works.core.domain.TransactionManifestData

fun DappToWalletInteractionUnvalidated.toDomainModel(remoteEntityId: IncomingMessage.RemoteEntityID): IncomingMessage.IncomingRequest {
    try {
        val metadata = IncomingMessage.IncomingRequest.RequestMetadata(
            networkId = metadata.networkId,
            origin = metadata.origin.toString(),
            dAppDefinitionAddress = metadata.dappDefinitionAddress,
            isInternal = false
        )
        return when (val itemsTemp = items) {
            is DappToWalletInteractionItems.AuthorizedRequest -> {
                itemsTemp.v1.parseAuthorizedRequest(remoteEntityId, interactionId, metadata)
            }

            is DappToWalletInteractionItems.Transaction -> {
                itemsTemp.v1.send.toDomainModel(remoteEntityId, interactionId, metadata)
            }

            is DappToWalletInteractionItems.UnauthorizedRequest -> {
                itemsTemp.v1.parseUnauthorizedRequest(remoteEntityId, interactionId, metadata)
            }
        }
    } catch (e: Exception) {
        throw RadixWalletException.IncomingMessageException.MessageParse(e)
    }
}

fun DappToWalletInteractionSendTransactionItem.toDomainModel(
    remoteConnectorId: IncomingMessage.RemoteEntityID, // from which CE comes the message
    requestId: WalletInteractionId,
    metadata: IncomingMessage.IncomingRequest.RequestMetadata
) = IncomingMessage.IncomingRequest.TransactionRequest(
    remoteEntityId = remoteConnectorId,
    interactionId = requestId,
    transactionManifestData = TransactionManifestData(
        instructions = unvalidatedManifest.transactionManifestString,
        networkId = metadata.networkId,
        message = message?.let { TransactionManifestData.TransactionMessage.Public(it) } ?: TransactionManifestData.TransactionMessage.None,
        blobs = unvalidatedManifest.blobs .toList().map { it.bytes },
        version = version.toLong()
    ),
    requestMetadata = metadata
)

private fun DappToWalletInteractionUnauthorizedRequestItems.parseUnauthorizedRequest(
    remoteEntityId: IncomingMessage.RemoteEntityID,
    requestId: WalletInteractionId,
    metadata: IncomingMessage.IncomingRequest.RequestMetadata
): IncomingMessage.IncomingRequest.UnauthorizedRequest {
    return IncomingMessage.IncomingRequest.UnauthorizedRequest(
        remoteEntityId = remoteEntityId,
        interactionId = requestId,
        requestMetadata = metadata,
        oneTimeAccountsRequestItem = oneTimeAccounts?.toDomainModel(false),
        oneTimePersonaDataRequestItem = oneTimePersonaData?.toDomainModel(false),
    )
}

fun DappToWalletInteractionPersonaDataRequestItem.toDomainModel(isOngoing: Boolean = false): PersonaRequestItem? {
    if (isRequestingName == null && numberOfRequestedPhoneNumbers == null && numberOfRequestedEmailAddresses == null) return null
    return PersonaRequestItem(
        isRequestingName = isRequestingName == true,
        numberOfRequestedEmailAddresses = numberOfRequestedEmailAddresses?.toDomainModel(),
        numberOfRequestedPhoneNumbers = numberOfRequestedPhoneNumbers?.toDomainModel(),
        isOngoing = isOngoing
    )
}

fun DappToWalletInteractionAccountsRequestItem.toDomainModel(
    isOngoing: Boolean = true
): IncomingMessage.IncomingRequest.AccountsRequestItem? {
    // correct request but not actionable, return null
    if (numberOfAccounts.quantity.toInt() == 0 &&
        numberOfAccounts.quantifier == RequestedNumberQuantifier.EXACTLY
    ) {
        return null
    }
    return IncomingMessage.IncomingRequest.AccountsRequestItem(
        isOngoing = isOngoing,
        numberOfValues = numberOfAccounts.toDomainModel(),
        challenge = challenge
    )
}

fun RequestedQuantity.toDomainModel(): IncomingMessage.IncomingRequest.NumberOfValues {
    return when (quantifier) {
        RequestedNumberQuantifier.EXACTLY -> {
            IncomingMessage.IncomingRequest.NumberOfValues(
                quantity.toInt(),
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly
            )
        }

        RequestedNumberQuantifier.AT_LEAST -> {
            IncomingMessage.IncomingRequest.NumberOfValues(
                quantity.toInt(),
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            )
        }
    }
}

private fun DappToWalletInteractionAuthorizedRequestItems.parseAuthorizedRequest(
    remoteEntityId: IncomingMessage.RemoteEntityID,
    interactionId: WalletInteractionId,
    metadata: IncomingMessage.IncomingRequest.RequestMetadata
): IncomingMessage.IncomingRequest {
    val authDomainModel = when (val auth = auth) {
        is DappToWalletInteractionAuthRequestItem.LoginWithChallenge -> {
            IncomingMessage.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithChallenge(
                auth.v1.challenge
            )
        }

        DappToWalletInteractionAuthRequestItem.LoginWithoutChallenge -> {
            IncomingMessage.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge
        }

        is DappToWalletInteractionAuthRequestItem.UsePersona -> {
            IncomingMessage.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(
                auth.v1.identityAddress
            )
        }
    }
    return IncomingMessage.IncomingRequest.AuthorizedRequest(
        remoteEntityId = remoteEntityId,
        interactionId = interactionId,
        requestMetadata = metadata,
        authRequest = authDomainModel,
        oneTimeAccountsRequestItem = oneTimeAccounts?.toDomainModel(isOngoing = false),
        ongoingAccountsRequestItem = ongoingAccounts?.toDomainModel(isOngoing = true),
        oneTimePersonaDataRequestItem = oneTimePersonaData?.toDomainModel(isOngoing = false),
        ongoingPersonaDataRequestItem = ongoingPersonaData?.toDomainModel(isOngoing = true),
        resetRequestItem = reset
    )
}

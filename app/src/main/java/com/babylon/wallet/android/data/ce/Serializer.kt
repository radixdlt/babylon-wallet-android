package com.babylon.wallet.android.data.ce

import com.babylon.wallet.android.data.ce.dapp.model.AuthLoginRequestItem
import com.babylon.wallet.android.data.ce.dapp.model.AuthLoginWithChallengeRequestResponseItem
import com.babylon.wallet.android.data.ce.dapp.model.AuthLoginWithoutChallengeRequestResponseItem
import com.babylon.wallet.android.data.ce.dapp.model.AuthRequestItem
import com.babylon.wallet.android.data.ce.dapp.model.AuthRequestResponseItem
import com.babylon.wallet.android.data.ce.dapp.model.AuthUsePersonaRequestItem
import com.babylon.wallet.android.data.ce.dapp.model.AuthUsePersonaRequestResponseItem
import com.babylon.wallet.android.data.ce.dapp.model.WalletAuthorizedRequestResponseItems
import com.babylon.wallet.android.data.ce.dapp.model.WalletInteractionFailureResponse
import com.babylon.wallet.android.data.ce.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.ce.dapp.model.WalletInteractionResponseItems
import com.babylon.wallet.android.data.ce.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.ce.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.ce.dapp.model.WalletUnauthorizedRequestResponseItems
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val peerdroidSerializersModule = SerializersModule {
    polymorphic(WalletInteractionResponseItems::class) {
        subclass(WalletUnauthorizedRequestResponseItems::class, WalletUnauthorizedRequestResponseItems.serializer())
        subclass(WalletAuthorizedRequestResponseItems::class, WalletAuthorizedRequestResponseItems.serializer())
        subclass(WalletTransactionResponseItems::class, WalletTransactionResponseItems.serializer())
    }
    polymorphic(AuthRequestItem::class) {
        subclass(AuthUsePersonaRequestItem::class, AuthUsePersonaRequestItem.serializer())
        subclass(AuthLoginRequestItem::class, AuthLoginRequestItem.serializer())
    }
    polymorphic(AuthRequestResponseItem::class) {
        subclass(
            AuthLoginWithChallengeRequestResponseItem::class,
            AuthLoginWithChallengeRequestResponseItem.serializer()
        )
        subclass(
            AuthLoginWithoutChallengeRequestResponseItem::class,
            AuthLoginWithoutChallengeRequestResponseItem.serializer()
        )
        subclass(
            AuthUsePersonaRequestResponseItem::class,
            AuthUsePersonaRequestResponseItem.serializer()
        )
    }
    polymorphic(WalletInteractionItems::class) {
        subclass(WalletUnauthorizedRequestItems::class, WalletUnauthorizedRequestItems.serializer())
        subclass(WalletAuthorizedRequestItems::class, WalletAuthorizedRequestItems.serializer())
        subclass(WalletTransactionItems::class, WalletTransactionItems.serializer())
    }
    polymorphic(WalletInteractionResponse::class) {
        subclass(WalletInteractionSuccessResponse::class, WalletInteractionSuccessResponse.serializer())
        subclass(WalletInteractionFailureResponse::class, WalletInteractionFailureResponse.serializer())
    }
    polymorphic(LedgerInteraction::class) {
        subclass(GetDeviceInfoRequest::class, GetDeviceInfoRequest.serializer())
        subclass(DerivePublicKeyRequest::class, DerivePublicKeyRequest.serializer())
        subclass(ImportOlympiaDeviceRequest::class, ImportOlympiaDeviceRequest.serializer())
        subclass(SignTransactionRequest::class, SignTransactionRequest.serializer())
        subclass(SignChallengeRequest::class, SignChallengeRequest.serializer())
    }
    polymorphic(LedgerInteractionResponse::class) {
        subclass(GetDeviceInfoResponse::class, GetDeviceInfoResponse.serializer())
        subclass(DerivePublicKeyResponse::class, DerivePublicKeyResponse.serializer())
        subclass(ImportOlympiaDeviceResponse::class, ImportOlympiaDeviceResponse.serializer())
        subclass(SignTransactionResponse::class, SignTransactionResponse.serializer())
        subclass(SignChallengeResponse::class, SignChallengeResponse.serializer())
        subclass(LedgerInteractionErrorResponse::class, LedgerInteractionErrorResponse.serializer())
    }
}

object ConnectorExtensionInteractionSerializer :
    JsonContentPolymorphicSerializer<ConnectorExtensionInteraction>(ConnectorExtensionInteraction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ConnectorExtensionInteraction> {
        return when {
            element.jsonObject["items"] != null -> WalletInteraction.serializer()
            else -> LedgerInteractionResponse.serializer()
        }
    }
}

val peerdroidRequestJson = Json {
    serializersModule = peerdroidSerializersModule
    classDiscriminator = "discriminator"
}
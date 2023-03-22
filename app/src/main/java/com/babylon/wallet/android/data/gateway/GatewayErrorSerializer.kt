package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.models.EntityNotFoundError
import com.babylon.wallet.android.data.gateway.generated.models.GatewayError
import com.babylon.wallet.android.data.gateway.generated.models.InternalServerError
import com.babylon.wallet.android.data.gateway.generated.models.InvalidEntityError
import com.babylon.wallet.android.data.gateway.generated.models.InvalidRequestError
import com.babylon.wallet.android.data.gateway.generated.models.InvalidTransactionError
import com.babylon.wallet.android.data.gateway.generated.models.NotSyncedUpError
import com.babylon.wallet.android.data.gateway.generated.models.TransactionNotFoundError
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object GatewayErrorSerializer : JsonContentPolymorphicSerializer<GatewayError>(GatewayError::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out GatewayError> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "TransactionNotFoundError" -> TransactionNotFoundError.serializer()
            "InvalidEntityError" -> InvalidEntityError.serializer()
            "EntityNotFoundError" -> EntityNotFoundError.serializer()
            "InvalidRequestError" -> InvalidRequestError.serializer()
            "InvalidTransactionError" -> InvalidTransactionError.serializer()
            "NotSyncedUpError" -> NotSyncedUpError.serializer()
            else -> InternalServerError.serializer()
        }
    }
}

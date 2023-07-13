package com.babylon.wallet.android.presentation.navigation

import android.os.Build
import android.os.Bundle
import androidx.navigation.NavType
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.MessageFromDataChannel

val PersonaRequestItemParameterType =
    object : NavType<MessageFromDataChannel.IncomingRequest.PersonaRequestItem>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): MessageFromDataChannel.IncomingRequest.PersonaRequestItem {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(key, MessageFromDataChannel.IncomingRequest.PersonaRequestItem::class.java)!!
            } else {
                bundle.getParcelable(key)!!
            }
        }

        override fun parseValue(value: String): MessageFromDataChannel.IncomingRequest.PersonaRequestItem {
            return Serializer.kotlinxSerializationJson.decodeFromString(value)
        }

        override fun put(bundle: Bundle, key: String, value: MessageFromDataChannel.IncomingRequest.PersonaRequestItem) {
            bundle.putParcelable(key, value)
        }

    }
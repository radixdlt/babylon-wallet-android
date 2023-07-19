package com.babylon.wallet.android.presentation.navigation

import android.os.Build
import android.os.Bundle
import androidx.navigation.NavType
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.RequiredPersonaFields

val RequiredPersonaFieldsParameterType =
    object : NavType<RequiredPersonaFields>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String): RequiredPersonaFields? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(key, RequiredPersonaFields::class.java)
            } else {
                bundle.getParcelable(key)
            }
        }

        override fun parseValue(value: String): RequiredPersonaFields {
            return Serializer.kotlinxSerializationJson.decodeFromString(value)
        }

        override fun put(bundle: Bundle, key: String, value: RequiredPersonaFields) {
            bundle.putParcelable(key, value)
        }
    }

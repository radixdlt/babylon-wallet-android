package com.babylon.wallet.android.presentation.navigation

import android.os.Build
import android.os.Bundle
import androidx.navigation.NavType
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.RequiredFields

val RequiredFieldsParameterType =
    object : NavType<RequiredFields>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String): RequiredFields? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(key, RequiredFields::class.java)
            } else {
                bundle.getParcelable(key)
            }
        }

        override fun parseValue(value: String): RequiredFields {
            return Serializer.kotlinxSerializationJson.decodeFromString(value)
        }

        override fun put(bundle: Bundle, key: String, value: RequiredFields) {
            bundle.putParcelable(key, value)
        }
    }

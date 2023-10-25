package com.babylon.wallet.android.domain.model.transaction

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.navigation.NavType
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

val TransactionStatusParameterType =
    object : NavType<TransactionStatusFields>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String): TransactionStatusFields? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(key, TransactionStatusFields::class.java)
            } else {
                bundle.getParcelable(key)
            }
        }

        override fun parseValue(value: String): TransactionStatusFields {
            return Serializer.kotlinxSerializationJson.decodeFromString(value)
        }

        override fun put(bundle: Bundle, key: String, value: TransactionStatusFields) {
            bundle.putParcelable(key, value)
        }
    }

@Keep
@Serializable
@Parcelize
data class TransactionStatusFields(
    val status: String,
    val requestId: String,
    val transactionId: String,
    val isInternal: Boolean,
    val error: String?,
    val transactionProcessingTime: String?,
    val walletErrorType: String?,
    val blockUntilComplete: Boolean
) : Parcelable

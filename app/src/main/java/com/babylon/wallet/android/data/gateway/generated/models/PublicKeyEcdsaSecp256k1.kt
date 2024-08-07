/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package com.babylon.wallet.android.data.gateway.generated.models

import com.babylon.wallet.android.data.gateway.generated.models.PublicKey
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyType

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param keyType 
 * @param keyHex The hex-encoded compressed ECDSA Secp256k1 public key (33 bytes)
 */
@Serializable

data class PublicKeyEcdsaSecp256k1 (

    @Contextual @SerialName(value = "key_type")
    override val keyType: PublicKeyType,

    /* The hex-encoded compressed ECDSA Secp256k1 public key (33 bytes) */
    @SerialName(value = "key_hex")
    val keyHex: kotlin.String

) : PublicKey()


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


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param event 
 * @param emitterAddress Bech32m-encoded human readable version of the address.
 * @param resourceAddress Bech32m-encoded human readable version of the address.
 */
@Serializable

data class StreamTransactionsRequestEventFilterItem (

    @SerialName(value = "event")
    val event: StreamTransactionsRequestEventFilterItem.Event,

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "emitter_address")
    val emitterAddress: kotlin.String? = null,

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "resource_address")
    val resourceAddress: kotlin.String? = null

) {

    /**
     * 
     *
     * Values: Deposit,Withdrawal
     */
    @Serializable
    enum class Event(val value: kotlin.String) {
        @SerialName(value = "Deposit") Deposit("Deposit"),
        @SerialName(value = "Withdrawal") Withdrawal("Withdrawal");
    }
}


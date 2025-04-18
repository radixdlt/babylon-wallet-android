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

import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleLocationResponseItem

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param ledgerState 
 * @param resourceAddress Bech32m-encoded human readable version of the address.
 * @param nonFungibleIds 
 */
@Serializable

data class StateNonFungibleLocationResponse (

    @SerialName(value = "ledger_state")
    val ledgerState: LedgerState,

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "resource_address")
    val resourceAddress: kotlin.String,

    @SerialName(value = "non_fungible_ids")
    val nonFungibleIds: kotlin.collections.List<StateNonFungibleLocationResponseItem>

)


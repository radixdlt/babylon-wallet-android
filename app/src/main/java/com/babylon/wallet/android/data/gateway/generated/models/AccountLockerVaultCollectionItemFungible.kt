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

import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItemType

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param type 
 * @param resourceAddress Bech32m-encoded human readable version of the address.
 * @param vaultAddress Bech32m-encoded human readable version of the address.
 * @param lastUpdatedAtStateVersion The most recent state version underlying object was modified at.
 * @param amount String-encoded decimal representing the amount of a related fungible resource.
 */
@Serializable

data class AccountLockerVaultCollectionItemFungible (

    @Contextual @SerialName(value = "type")
    override val type: AccountLockerVaultCollectionItemType,

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "resource_address")
    override val resourceAddress: kotlin.String,

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "vault_address")
    override val vaultAddress: kotlin.String,

    /* The most recent state version underlying object was modified at. */
    @SerialName(value = "last_updated_at_state_version")
    override val lastUpdatedAtStateVersion: kotlin.Long,

    /* String-encoded decimal representing the amount of a related fungible resource. */
    @SerialName(value = "amount")
    val amount: kotlin.String

) : AccountLockerVaultCollectionItem()


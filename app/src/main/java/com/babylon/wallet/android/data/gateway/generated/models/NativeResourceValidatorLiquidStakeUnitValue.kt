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

import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceKind
import com.babylon.wallet.android.data.gateway.generated.models.NativeResourceRedemptionValueItem

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param kind 
 * @param validatorAddress Bech32m-encoded human readable version of the address.
 * @param redemptionResourceCount 
 * @param unitRedemptionValue 
 */
@Serializable

data class NativeResourceValidatorLiquidStakeUnitValue (

    @Contextual @SerialName(value = "kind")
    override val kind: NativeResourceKind,

    /* Bech32m-encoded human readable version of the address. */
    @SerialName(value = "validator_address")
    val validatorAddress: kotlin.String,

    @SerialName(value = "redemption_resource_count")
    val redemptionResourceCount: kotlin.Int,

    @SerialName(value = "unit_redemption_value")
    val unitRedemptionValue: kotlin.collections.List<NativeResourceRedemptionValueItem>

) : NativeResourceDetails()


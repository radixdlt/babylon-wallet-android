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

import com.babylon.wallet.android.data.gateway.generated.models.AccountDefaultDepositRule
import com.babylon.wallet.android.data.gateway.generated.models.AccountDepositPreValidationDecidingFactorsResourceSpecificDetailsItem

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * Deciding factors used to calculate response.
 *
 * @param defaultDepositRule 
 * @param isBadgeAuthorizedDepositor Whether the input badge belongs to the account's set of authorized depositors. This field will only be present if any badge was passed in the request.
 * @param resourceSpecificDetails Returns deciding factors for each resource. Contains only information about resources presented in the request, not all resource preference rules for queried account.
 */
@Serializable

data class AccountDepositPreValidationDecidingFactors (

    @Contextual @SerialName(value = "default_deposit_rule")
    val defaultDepositRule: AccountDefaultDepositRule,

    /* Whether the input badge belongs to the account's set of authorized depositors. This field will only be present if any badge was passed in the request. */
    @SerialName(value = "is_badge_authorized_depositor")
    val isBadgeAuthorizedDepositor: kotlin.Boolean? = null,

    /* Returns deciding factors for each resource. Contains only information about resources presented in the request, not all resource preference rules for queried account. */
    @SerialName(value = "resource_specific_details")
    val resourceSpecificDetails: kotlin.collections.List<AccountDepositPreValidationDecidingFactorsResourceSpecificDetailsItem>? = null

)


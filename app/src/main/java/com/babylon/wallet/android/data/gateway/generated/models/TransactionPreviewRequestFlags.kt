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
 * @param useFreeCredit Whether to use a virtual, preview-only pool of XRD to pay for all execution fees. 
 * @param assumeAllSignatureProofs Whether the virtual signature proofs should be automatically placed in the auth zone. 
 * @param skipEpochCheck Whether to skip the epoch range check (i.e. ignoring the `start_epoch_inclusive` and `end_epoch_exclusive` parameters, if specified).  Note: effectively, without an epoch range, the Radix Engine cannot perform the *intent hash duplicate* detection, which means that this check will be skipped as well. 
 * @param disableAuthChecks Whether to skip the auth checks during execution.  This could be used to e.g.: * Preview protocol update style transactions. * Mint resources for previewing trades with resources you don't own. If doing this, be warned:   * Only resources which were potentially mintable/burnable at creation time     will be mintable/burnable, due to feature flags on the resource.   * Please see the below warning about unexpected results if using this approach.  Warning: this mode of operation is quite a departure from normal operation: * Calculated fees will likely be lower than a standard execution. * This mode can subtly break invariants some dApp code might rely on, or result in unexpected   behaviour, so the resulting execution result might not be valid for your needs. For example,   if I used this flag to mint pool units to preview a redemption (or some dApp interaction which   behind the scenes redeemed them), they'd redeem for less than they're currently worth,   because the blueprint code relies on the total supply of the pool units to calculate their   redemption worth, and you've just inflated the total supply through the mint operation. 
 */
@Serializable

data class TransactionPreviewRequestFlags (

    /* Whether to use a virtual, preview-only pool of XRD to pay for all execution fees.  */
    @SerialName(value = "use_free_credit")
    val useFreeCredit: kotlin.Boolean,

    /* Whether the virtual signature proofs should be automatically placed in the auth zone.  */
    @SerialName(value = "assume_all_signature_proofs")
    val assumeAllSignatureProofs: kotlin.Boolean,

    /* Whether to skip the epoch range check (i.e. ignoring the `start_epoch_inclusive` and `end_epoch_exclusive` parameters, if specified).  Note: effectively, without an epoch range, the Radix Engine cannot perform the *intent hash duplicate* detection, which means that this check will be skipped as well.  */
    @SerialName(value = "skip_epoch_check")
    val skipEpochCheck: kotlin.Boolean,

    /* Whether to skip the auth checks during execution.  This could be used to e.g.: * Preview protocol update style transactions. * Mint resources for previewing trades with resources you don't own. If doing this, be warned:   * Only resources which were potentially mintable/burnable at creation time     will be mintable/burnable, due to feature flags on the resource.   * Please see the below warning about unexpected results if using this approach.  Warning: this mode of operation is quite a departure from normal operation: * Calculated fees will likely be lower than a standard execution. * This mode can subtly break invariants some dApp code might rely on, or result in unexpected   behaviour, so the resulting execution result might not be valid for your needs. For example,   if I used this flag to mint pool units to preview a redemption (or some dApp interaction which   behind the scenes redeemed them), they'd redeem for less than they're currently worth,   because the blueprint code relies on the total supply of the pool units to calculate their   redemption worth, and you've just inflated the total supply through the mint operation.  */
    @SerialName(value = "disable_auth_checks")
    val disableAuthChecks: kotlin.Boolean? = null

)


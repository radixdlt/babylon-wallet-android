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
import com.babylon.wallet.android.data.gateway.generated.models.TransactionIntentStatus
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatus
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusResponseKnownPayloadItem

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param ledgerState 
 * @param status 
 * @param intentStatus 
 * @param intentStatusDescription An additional description to clarify the intent status. 
 * @param knownPayloads 
 * @param committedStateVersion If the intent was committed, this gives the state version when this intent was committed. 
 * @param permanentlyRejectsAtEpoch The epoch number at which the transaction is guaranteed to get permanently rejected by the Network due to exceeded epoch range defined when submitting transaction.
 * @param errorMessage The most relevant error message received, due to a rejection or commit as failure. Please note that presence of an error message doesn't imply that the intent will definitely reject or fail. This could represent a temporary error (such as out of fees), or an error with a payload which doesn't end up being committed. 
 */
@Serializable

data class TransactionStatusResponse (

    @SerialName(value = "ledger_state")
    val ledgerState: LedgerState,

    @Contextual @SerialName(value = "status")
    val status: TransactionStatus,

    @Contextual @SerialName(value = "intent_status")
    val intentStatus: TransactionIntentStatus,

    /* An additional description to clarify the intent status.  */
    @SerialName(value = "intent_status_description")
    val intentStatusDescription: kotlin.String,

    @SerialName(value = "known_payloads")
    val knownPayloads: kotlin.collections.List<TransactionStatusResponseKnownPayloadItem>,

    /* If the intent was committed, this gives the state version when this intent was committed.  */
    @SerialName(value = "committed_state_version")
    val committedStateVersion: kotlin.Long? = null,

    /* The epoch number at which the transaction is guaranteed to get permanently rejected by the Network due to exceeded epoch range defined when submitting transaction. */
    @SerialName(value = "permanently_rejects_at_epoch")
    val permanentlyRejectsAtEpoch: kotlin.Long? = null,

    /* The most relevant error message received, due to a rejection or commit as failure. Please note that presence of an error message doesn't imply that the intent will definitely reject or fail. This could represent a temporary error (such as out of fees), or an error with a payload which doesn't end up being committed.  */
    @SerialName(value = "error_message")
    val errorMessage: kotlin.String? = null

)


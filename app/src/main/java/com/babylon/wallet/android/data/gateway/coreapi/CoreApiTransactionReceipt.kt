package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A bit different from [TransactionReceipt] since [TransactionPreviewResponse] returns the core api object.
 */
@Serializable
data class CoreApiTransactionReceipt (

    /* The status of the transaction. */
    @SerialName(value = "status")
    val status: String,

    /* Fees paid, Only present if the `status` is not `Rejected`. */
    @SerialName(value = "fee_summary")
    val feeSummary: FeeSummary? = null,

    @SerialName(value = "costing_parameters")
    val costingParameters: CostingParameters? = null,

    /* Error message (only present if status is `Failed` or `Rejected`) */
    @SerialName(value = "error_message")
    val errorMessage: kotlin.String? = null

) {

    val isFailed
        get() = status == "Failed" || status == "Rejected"

}
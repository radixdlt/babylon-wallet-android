package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeeSummary(
    @SerialName("execution_cost_units_consumed")
    val execution_cost_units_consumed: Long,
    @SerialName("finalization_cost_units_consumed")
    val finalization_cost_units_consumed: Long,
    @SerialName("xrd_total_execution_cost")
    val xrd_total_execution_cost: String,
    @SerialName("xrd_total_finalization_cost")
    val xrd_total_finalization_cost: String,
    @SerialName("xrd_total_royalty_cost")
    val xrd_total_royalty_cost: String,
    @SerialName("xrd_total_storage_cost")
    val xrd_total_storage_cost: String,
    @SerialName("xrd_total_tipping_cost")
    val xrd_total_tipping_cost: String
)
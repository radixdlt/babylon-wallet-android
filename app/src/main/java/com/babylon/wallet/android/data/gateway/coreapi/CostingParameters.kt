package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CostingParameters(
    @SerialName("execution_cost_unit_price")
    val execution_cost_unit_price: String,
    @SerialName("execution_cost_unit_limit")
    val execution_cost_unit_limit: Long,
    @SerialName("execution_cost_unit_loan")
    val execution_cost_unit_loan: Long,
    @SerialName("finalization_cost_unit_price")
    val finalization_cost_unit_price: String,
    @SerialName("finalization_cost_unit_limit")
    val finalization_cost_unit_limit: Long,
    @SerialName("xrd_usd_price")
    val xrd_usd_price: String,
    @SerialName("xrd_storage_price")
    val xrd_storage_price: String,
    @SerialName("tip_percentage")
    val tip_percentage: Int
)
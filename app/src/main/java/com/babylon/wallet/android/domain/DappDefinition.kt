package com.babylon.wallet.android.domain

data class DappDefinition(
    val dAppDefinitionAddress: String
)

data class DappDefinitions(
    val dAppDefinitions: List<DappDefinition>,
    val callbackPath: String? = null
)
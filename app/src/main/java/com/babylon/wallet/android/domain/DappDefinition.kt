package com.babylon.wallet.android.domain

import com.radixdlt.sargon.AccountAddress

data class DappDefinition(
    val dAppDefinitionAddress: AccountAddress
)

data class DappDefinitions(
    val dAppDefinitions: List<DappDefinition>,
    val callbackPath: String? = null
)

package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.Serializable

@Serializable(with = ConnectorExtensionInteractionSerializer::class)
sealed interface ConnectorExtensionInteraction
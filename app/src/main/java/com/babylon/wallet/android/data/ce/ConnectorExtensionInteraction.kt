package com.babylon.wallet.android.data.ce

import kotlinx.serialization.Serializable

@Serializable(with = ConnectorExtensionInteractionSerializer::class)
sealed interface ConnectorExtensionInteraction
package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NumberOfValues(
    @SerialName("quantifier") val quantifier: Quantifier,
    @SerialName("quantity") val quantity: Int,
) {

    @Serializable
    enum class Quantifier {
        @SerialName("exactly")
        Exactly,

        @SerialName("atLeast")
        AtLeast,
    }
}

fun NumberOfValues.toDomainModel(): MessageFromDataChannel.IncomingRequest.NumberOfValues {
    return MessageFromDataChannel.IncomingRequest.NumberOfValues(quantity, quantifier.toDomainModel())
}

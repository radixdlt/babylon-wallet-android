package com.babylon.wallet.android.domain.model.messages

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import rdx.works.core.sargon.PersonaDataField

@Keep
@Serializable
@Parcelize
data class RequiredPersonaFields(
    val fields: List<RequiredPersonaField> = emptyList()
) : Parcelable

@Keep
@Serializable
@Parcelize
data class RequiredPersonaField(
    val kind: PersonaDataField.Kind,
    val numberOfValues: IncomingMessage.DappToWalletInteraction.NumberOfValues
) : Parcelable

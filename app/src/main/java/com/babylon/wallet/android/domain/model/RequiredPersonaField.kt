package com.babylon.wallet.android.domain.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.pernetwork.PersonaData

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
    val kind: PersonaData.PersonaDataField.Kind,
    val numberOfValues: MessageFromDataChannel.IncomingRequest.NumberOfValues
) : Parcelable

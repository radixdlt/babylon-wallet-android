package com.babylon.wallet.android.domain.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.pernetwork.PersonaData

@Keep
@Serializable
@Parcelize
data class RequiredFields(
    val fields: List<RequiredField> = emptyList()
) : Parcelable

@Keep
@Serializable
@Parcelize
data class RequiredField(
    val kind: PersonaData.PersonaDataField.Kind,
    val numberOfValues: MessageFromDataChannel.IncomingRequest.NumberOfValues
) : Parcelable

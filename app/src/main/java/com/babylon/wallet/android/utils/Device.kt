package com.babylon.wallet.android.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Locale

class Device private constructor(
    val name: String,
    val manufacturer: String,
    val model: String
) {

    private val commercialName: String
        get() = if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

    val displayName: String
        get()= if (name.isBlank()) {
            commercialName
        } else {
            "$name ($commercialName)"
        }

    companion object {

        fun factory(context: Context) = Device(
            name = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) ?: "",
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL
        )

    }

}

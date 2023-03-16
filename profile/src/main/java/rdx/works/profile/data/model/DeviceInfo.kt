package rdx.works.profile.data.model

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Locale

data class DeviceInfo(
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
        get() = if (name.isBlank()) {
            commercialName
        } else {
            "$name ($commercialName)"
        }

    companion object {

        fun factory(context: Context) = DeviceInfo(
            name = Settings.Global.getString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME
            ).orEmpty(),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL
        )
    }
}

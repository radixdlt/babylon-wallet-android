package rdx.works.core.domain

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.Sample
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
        }

    val displayName: String
        get() = if (name.isBlank()) {
            commercialName
        } else {
            "$name $commercialName"
        }

    val isSamsungDevice: Boolean
        get() = manufacturer.contains("samsung", ignoreCase = true)

    companion object {

        fun factory(context: Context) = DeviceInfo(
            name = Settings.Global.getString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME
            ).orEmpty(),
            manufacturer = Build.MANUFACTURER.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            model = Build.MODEL.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        )

        @UsesSampleValues
        val sample: Sample<DeviceInfo>
            get() = object : Sample<DeviceInfo> {
                override fun invoke(): DeviceInfo = DeviceInfo(
                    name = "Sample",
                    manufacturer = "Test",
                    model = "1"
                )

                override fun other(): DeviceInfo = DeviceInfo(
                    name = "Sample XL",
                    manufacturer = "Test",
                    model = "2"
                )
            }
    }
}

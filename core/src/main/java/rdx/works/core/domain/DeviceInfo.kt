package rdx.works.core.domain

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.Sample
import rdx.works.core.DeviceId
import java.util.Locale
import java.util.UUID

data class DeviceInfo(
    val id: UUID,
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

    companion object {

        fun factory(context: Context) = DeviceInfo(
            id = DeviceId.getOrGenerate(context),
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
                    id = UUID.fromString("6b3b43cd-135f-418b-9673-aef82cd016b5"),
                    name = "Sample",
                    manufacturer = "Test",
                    model = "1"
                )

                override fun other(): DeviceInfo = DeviceInfo(
                    id = UUID.fromString("a7a91af4-9734-4114-910d-532f9c9becfb"),
                    name = "Sample XL",
                    manufacturer = "Test",
                    model = "2"
                )
            }
    }
}

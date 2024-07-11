package rdx.works.core.domain

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.Uuid
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.Sample
import kotlinx.serialization.Serializable
import rdx.works.core.TimestampGenerator
import rdx.works.core.UUIDGenerator
import rdx.works.core.serializers.TimestampSerializer
import rdx.works.core.serializers.UuidSerializer
import java.util.Locale
import java.util.UUID

@Serializable
data class DeviceInfo(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = TimestampSerializer::class)
    val date: Timestamp,
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

    val systemVersion: String
        get() = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun toSargonDeviceInfo(): com.radixdlt.sargon.DeviceInfo = com.radixdlt.sargon.DeviceInfo(
        id = id,
        date = date,
        description = displayName,
        systemVersion = systemVersion,
        hostAppVersion = null, // TODO DeviceInfo
        hostVendor = manufacturer
    )

    companion object {

        fun factory(context: Context) = DeviceInfo(
            id = UUIDGenerator.uuid(),
            date = TimestampGenerator(),
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
                    date = Timestamp.parse("2024-05-28T15:01:49.067Z"),
                    name = "Sample",
                    manufacturer = "Test",
                    model = "1"
                )

                override fun other(): DeviceInfo = DeviceInfo(
                    id = UUID.fromString("a7a91af4-9734-4114-910d-532f9c9becfb"),
                    date = Timestamp.parse("2024-05-28T15:02:32.324Z"),
                    name = "Sample XL",
                    manufacturer = "Test",
                    model = "2"
                )
            }
    }
}

package rdx.works.profile.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.radixdlt.sargon.DeviceInfoDescription
import com.radixdlt.sargon.HostId
import com.radixdlt.sargon.HostInfo
import com.radixdlt.sargon.HostOs
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.Uuid
import com.radixdlt.sargon.extensions.android
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.UUIDGenerator
import rdx.works.core.serializers.TimestampSerializer
import rdx.works.core.serializers.UuidSerializer
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

interface HostInfoRepository {

    fun getHostId(): HostId

    fun getHostInfo(): HostInfo
}

class HostInfoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HostInfoRepository {

    override fun getHostId(): HostId {
        val hostId = getStoredHostId(context)

        return hostId ?: generateHostId(context)
    }

    override fun getHostInfo(): HostInfo = HostInfo(
        description = DeviceInfoDescription(
            name = getDeviceName(context),
            model = getDeviceModel()
        ),
        hostOs = HostOs.android(
            vendor = getVendor(),
            version = getAndroidVersion()
        ),
        hostAppVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    )

    /**
     * Entry stored on device preferences. Kept to ensure compatibility with previous versions
     *
     * - In version 1.6.0 DeviceInfo object was introduced in preferences with
     *  -- id: Uuid,
     *  -- date: Timestamp,
     *  -- name: String,
     *  -- manufacturer: String,
     *  -- model: String
     *  the intention was to keep a stable identifier along with some more data.
     *
     *  - From version 1.X.X there is no need to keep the all the rest of the data in the preferences.
     * Thus only the id and date are kept and the rest of the values will be calculated on the fly.
     * So [HostIdEntry] contains actually a subset of critical data being kept in DeviceInfo previously.
     */
    @Serializable
    private data class HostIdEntry(
        @Serializable(with = UuidSerializer::class)
        val id: Uuid,
        @Serializable(with = TimestampSerializer::class)
        val date: Timestamp
    ) {

        fun toHostId() = HostId(
            id = id,
            generatedAt = date
        )

        companion object {
            fun generate() = HostIdEntry(
                id = UUIDGenerator.uuid(),
                date = Timestamp.now()
            ).also {
                Timber.d("Generated host id: ${it.id}")
            }
        }
    }

    private fun getStoredHostId(context: Context): HostId? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(HOST_ID, null)
            ?.let {
                Json {
                    // Ignore previous values for compatibility
                    ignoreUnknownKeys = true
                    isLenient = true
                }.decodeFromString<HostIdEntry>(it).toHostId()
            }
    }

    private fun generateHostId(context: Context): HostId {
        val hostIdEntry = HostIdEntry.generate()

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) {
            putString(HOST_ID, Json.encodeToString(hostIdEntry))
        }

        return hostIdEntry.toHostId()
    }

    private fun getDeviceName(context: Context) = Settings.Global.getString(
        context.contentResolver,
        Settings.Global.DEVICE_NAME
    ).orEmpty()

    private fun getDeviceModel() = Build.MODEL.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    private fun getVendor() = Build.MANUFACTURER.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    private fun getAndroidVersion(): String = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    companion object {
        @VisibleForTesting
        const val PREFS = "device_prefs"

        @VisibleForTesting
        const val HOST_ID = "key_device_info"
    }
}

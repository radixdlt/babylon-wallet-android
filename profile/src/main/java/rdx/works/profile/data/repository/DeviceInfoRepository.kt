package rdx.works.profile.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.domain.DeviceInfo
import timber.log.Timber
import javax.inject.Inject

interface DeviceInfoRepository {

    fun getDeviceInfo(): DeviceInfo
}

class DeviceInfoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceInfoRepository {

    override fun getDeviceInfo(): DeviceInfo {
        val storedDeviceInfo = getStoredDeviceInfo(context)

        if (storedDeviceInfo != null) {
            return storedDeviceInfo.also {
                Timber.d("Device id exists: ${it.id}")
            }
        }

        val generated = DeviceInfo.factory(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) {
            putString(DEVICE_INFO, Json.encodeToString(generated))
        }

        return generated.also {
            Timber.d("Generated device id: ${generated.id}")
        }
    }

    private fun getStoredDeviceInfo(context: Context): DeviceInfo? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(DEVICE_INFO, null)
            ?.let {
                Json.decodeFromString(it)
            }
    }

    companion object {
        private const val PREFS = "device_prefs"
        private const val DEVICE_INFO = "key_device_info"
    }
}

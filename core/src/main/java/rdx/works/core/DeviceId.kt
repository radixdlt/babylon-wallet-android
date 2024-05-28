package rdx.works.core

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

object DeviceId {

    private const val PREFS = "device_prefs"
    private const val DEVICE_ID = "key_ephemeral_device_id"

    private fun getDeviceID(context: Context): UUID? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(DEVICE_ID, null)
            ?.let {
                UUID.fromString(it)
            }
    }

    fun getOrGenerate(context: Context): UUID {
        val current = getDeviceID(context)
        if (current != null) {
            return current
        }

        val uuid = UUIDGenerator.uuid()

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(DEVICE_ID, uuid.toString())
        }

        return uuid
    }
}

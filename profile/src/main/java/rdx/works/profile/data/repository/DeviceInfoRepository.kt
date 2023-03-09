package rdx.works.profile.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import rdx.works.profile.data.model.DeviceInfo
import javax.inject.Inject

interface DeviceInfoRepository {

    fun getDeviceInfo(): DeviceInfo
}

class DeviceInfoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceInfoRepository {

    override fun getDeviceInfo(): DeviceInfo = DeviceInfo.factory(context = context)
}

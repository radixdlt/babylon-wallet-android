package rdx.works.profile.data.repository

import com.radixdlt.sargon.HostId
import com.radixdlt.sargon.HostInfo
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.sargon.os.SargonOsManager
import javax.inject.Inject

interface HostInfoRepository {

    suspend fun getHostId(): Result<HostId>

    suspend fun getHostInfo(): Result<HostInfo>
}

class HostInfoRepositoryImpl @Inject constructor(
    private val sargonOsManager: SargonOsManager
) : HostInfoRepository {

    override suspend fun getHostId(): Result<HostId> {
        val sargonOs = sargonOsManager.sargonOs.firstOrNull() ?: return Result.failure(RuntimeException("Sargon os is not booted"))

        return runCatching { sargonOs.resolveHostId() }
    }

    override suspend fun getHostInfo(): Result<HostInfo> {
        val sargonOs = sargonOsManager.sargonOs.firstOrNull() ?: return Result.failure(RuntimeException("Sargon os is not booted"))

        return Result.success(sargonOs.resolveHostInfo())
    }
}

package rdx.works.profile.data.repository

import com.radixdlt.sargon.HostId
import com.radixdlt.sargon.HostInfo
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

interface HostInfoRepository {

    suspend fun getHostId(): HostId

    suspend fun getHostInfo(): HostInfo
}

class HostInfoRepositoryImpl @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : HostInfoRepository {

    override suspend fun getHostId(): HostId {
        val sargonOs = sargonOsManager.sargonOs

        return withContext(defaultDispatcher) { sargonOs.resolveHostId() }
    }

    override suspend fun getHostInfo(): HostInfo {
        val sargonOs = sargonOsManager.sargonOs

        return withContext(defaultDispatcher) { sargonOs.resolveHostInfo() }
    }
}

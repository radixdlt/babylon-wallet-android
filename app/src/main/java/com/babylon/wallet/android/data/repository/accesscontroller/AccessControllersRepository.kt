package com.babylon.wallet.android.data.repository.accesscontroller

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.paginateAccessControllerItems
import com.babylon.wallet.android.data.repository.accesscontroller.model.AccessControllerRecoveryState
import com.babylon.wallet.android.data.repository.cache.database.accesscontroller.AccessControllerDao
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.radixdlt.sargon.AccessControllerAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.InstantGenerator
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

interface AccessControllersRepository {

    suspend fun getAccessControllerRecoveryStates(
        addresses: Set<AccessControllerAddress>,
        isRefreshing: Boolean
    ): Result<List<AccessControllerRecoveryState>>
}

class AccessControllersRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val accessControllerDao: AccessControllerDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AccessControllersRepository {

    override suspend fun getAccessControllerRecoveryStates(
        addresses: Set<AccessControllerAddress>,
        isRefreshing: Boolean
    ): Result<List<AccessControllerRecoveryState>> = withContext(ioDispatcher) {
        val cachedStates = accessControllerDao.getAccessControllers(
            minValidity = accessControllersValidity(isRefreshing = isRefreshing)
        )

        if (cachedStates.isEmpty()) {
            val resolvedVersion = cachedStates.maxOfOrNull { it.stateVersion ?: -1L }?.takeIf { it > 0L }

            runCatching {
                val response = stateApi.paginateAccessControllerItems(
                    addresses = addresses,
                    stateVersion = resolvedVersion
                )

                val entities = response.toEntities()
                accessControllerDao.insertAccessControllers(entities)

                entities.map {
                    AccessControllerRecoveryState.from(it)
                }
            }
        } else {
            Result.success(cachedStates.map { AccessControllerRecoveryState.from(it) })
        }
    }

    companion object {

        private val accessControllersCacheDuration = 4.hours

        fun accessControllersValidity(isRefreshing: Boolean = false) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else accessControllersCacheDuration.inWholeMilliseconds
    }
}

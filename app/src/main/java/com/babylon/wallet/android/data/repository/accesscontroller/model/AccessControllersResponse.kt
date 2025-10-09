package com.babylon.wallet.android.data.repository.accesscontroller.model

import com.babylon.wallet.android.data.gateway.extensions.recoveryRoleRecoveryAttempt
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.accesscontroller.AccessControllerEntity
import com.radixdlt.sargon.AccessControllerAddress
import com.radixdlt.sargon.extensions.init
import rdx.works.core.InstantGenerator

data class AccessControllersResponse(
    val accessControllers: List<StateEntityDetailsResponseItem>,
    val stateVersion: Long? = null
) {

    fun toEntities(): List<AccessControllerEntity> {
        val synced = InstantGenerator()

        return accessControllers.map { entityDetails ->
            AccessControllerEntity(
                address = AccessControllerAddress.init(entityDetails.address),
                stateVersion = stateVersion,
                allowTimedRecoveryAfter = entityDetails.details?.recoveryRoleRecoveryAttempt?.allowTimedRecoveryAfter,
                synced = synced
            )
        }
    }
}

package com.babylon.wallet.android.data.repository.accesscontroller.model

import com.babylon.wallet.android.data.gateway.coreapi.ScryptoInstant
import com.babylon.wallet.android.data.repository.cache.database.accesscontroller.AccessControllerEntity
import com.radixdlt.sargon.AccessControllerAddress

data class AccessControllerRecoveryState(
    val address: AccessControllerAddress,
    val allowTimedRecoveryAfter: ScryptoInstant?
) {

    val isInTimedRecovery = allowTimedRecoveryAfter != null

    companion object {

        fun from(entity: AccessControllerEntity) = AccessControllerRecoveryState(
            address = entity.address,
            allowTimedRecoveryAfter = entity.allowTimedRecoveryAfter
        )
    }
}

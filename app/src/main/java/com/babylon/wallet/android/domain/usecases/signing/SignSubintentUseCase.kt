package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.data.dapp.model.SubintentExpiration
import com.radixdlt.sargon.DappToWalletInteractionSubintentHeader
import com.radixdlt.sargon.IntentDiscriminator
import com.radixdlt.sargon.RoleKind
import com.radixdlt.sargon.SignedSubintent
import com.radixdlt.sargon.SubintentManifest
import com.radixdlt.sargon.extensions.random
import com.radixdlt.sargon.os.SargonOsManager
import javax.inject.Inject

class SignSubintentUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager
) {

    suspend operator fun invoke(
        manifest: SubintentManifest,
        message: String?,
        expiration: SubintentExpiration,
        header: DappToWalletInteractionSubintentHeader?
    ): Result<SignedSubintent> = createSubintent(
        manifest = manifest,
        message = message,
        expiration = expiration,
        header = header
    ).mapCatching { subintent ->
        sargonOsManager.sargonOs.signSubintent(
            transactionIntent = subintent,
            roleKind = RoleKind.PRIMARY
        )
    }

    private suspend fun createSubintent(
        manifest: SubintentManifest,
        message: String?,
        expiration: SubintentExpiration,
        header: DappToWalletInteractionSubintentHeader?
    ) = runCatching {
        sargonOsManager.sargonOs.createSubintent(
            intentDiscriminator = IntentDiscriminator.random(),
            subintentManifest = manifest,
            expiration = expiration.toDAppInteraction(),
            message = message,
            header = header
        )
    }
}

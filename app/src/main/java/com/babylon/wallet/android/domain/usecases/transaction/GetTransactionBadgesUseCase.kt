package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.Badge
import com.radixdlt.ret.Address
import javax.inject.Inject

class GetTransactionBadgesUseCase @Inject constructor(
    private val dappMetadataRepository: DAppRepository
) {

    suspend operator fun invoke(
        accountProofs: List<Address>
    ): List<Badge> {
        val dAppsWithMetadata = dappMetadataRepository.getDAppsMetadata(
            needMostRecentData = false,
            definitionAddresses = accountProofs.map { it.addressString() }
        ).getOrNull().orEmpty()

        return dAppsWithMetadata.map { dApp ->
            Badge(
                address = dApp.dAppAddress,
                nameMetadataItem = dApp.name?.let { NameMetadataItem(it) },
                iconMetadataItem = dApp.iconUrl?.let { IconUrlMetadataItem(it) }
            )
        }
    }
}

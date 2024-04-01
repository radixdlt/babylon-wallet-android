package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NonFungibleResourceAddress
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.stakesClaim
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.Resource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.ret.transaction.TransactionManifestData
import rdx.works.profile.sargon.toDecimal192
import java.math.BigDecimal
import javax.inject.Inject

private typealias SargonStakeClaim = com.radixdlt.sargon.StakeClaim

class SendClaimRequestUseCase @Inject constructor(
    val incomingRequestRepository: IncomingRequestRepository
) {

    suspend operator fun invoke(
        account: Network.Account,
        claims: List<StakeClaim>,
        epoch: Long
    ) {
        runCatching {
            TransactionManifest.stakesClaim(
                accountAddress = AccountAddress.init(account.address),
                stakeClaims = claims.mapNotNull { claim ->
                    val nfts = claim.nonFungibleResource.items.filter { it.isReadyToClaim(epoch) }
                    if (nfts.isEmpty()) return@mapNotNull null

                    SargonStakeClaim(
                        resourceAddress = NonFungibleResourceAddress.init(claim.resourceAddress.string),
                        validatorAddress = claim.validatorAddress,
                        ids = nfts.map { it.localId },
                        amount = nfts.sumOf { it.claimAmountXrd ?: BigDecimal.ZERO }.toDecimal192(),
                    )
                }
            )
        }.mapCatching { manifest ->
            TransactionManifestData.from(manifest).prepareInternalTransactionRequest()
        }.onSuccess { request ->
            incomingRequestRepository.add(request)
        }
    }

    suspend operator fun invoke(
        account: Network.Account,
        claim: StakeClaim,
        nft: Resource.NonFungibleResource.Item,
        epoch: Long
    ) {
        if (!nft.isReadyToClaim(epoch)) return

        runCatching {
            TransactionManifest.stakesClaim(
                accountAddress = AccountAddress.init(account.address),
                stakeClaims = listOf(
                    SargonStakeClaim(
                        resourceAddress = NonFungibleResourceAddress.init(claim.resourceAddress.string),
                        validatorAddress = claim.validatorAddress,
                        ids = listOf(nft.localId),
                        amount = (nft.claimAmountXrd ?: BigDecimal.ZERO).toDecimal192(),
                    )
                )
            )
        }.mapCatching { manifest ->
            TransactionManifestData.from(manifest).prepareInternalTransactionRequest()
        }.onSuccess { request ->
            incomingRequestRepository.add(request)
        }
    }
}

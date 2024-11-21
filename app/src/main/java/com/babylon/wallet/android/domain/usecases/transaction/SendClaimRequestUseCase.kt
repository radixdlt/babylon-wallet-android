package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.usecases.interaction.PrepareInternalTransactionUseCase
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.NonFungibleResourceAddress
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.stakesClaim
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.sumOf
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.Resource
import javax.inject.Inject

private typealias SargonStakeClaim = com.radixdlt.sargon.StakeClaim

class SendClaimRequestUseCase @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val prepareInternalTransactionUseCase: PrepareInternalTransactionUseCase
) {

    suspend operator fun invoke(
        account: Account,
        claims: List<StakeClaim>,
        epoch: Long
    ) {
        runCatching {
            TransactionManifest.stakesClaim(
                accountAddress = account.address,
                stakeClaims = claims.mapNotNull { claim ->
                    val nfts = claim.nonFungibleResource.items.filter { it.isReadyToClaim(epoch) }
                    if (nfts.isEmpty()) return@mapNotNull null

                    SargonStakeClaim(
                        resourceAddress = NonFungibleResourceAddress.init(claim.resourceAddress.string),
                        validatorAddress = claim.validatorAddress,
                        ids = nfts.map { it.localId },
                        amount = nfts.sumOf { it.claimAmountXrd.orZero() },
                    )
                }
            )
        }.mapCatching { manifest ->
            prepareInternalTransactionUseCase(UnvalidatedManifestData.from(manifest))
        }.onSuccess { request ->
            incomingRequestRepository.add(request)
        }
    }

    suspend operator fun invoke(
        account: Account,
        claim: StakeClaim,
        nft: Resource.NonFungibleResource.Item,
        epoch: Long
    ) {
        if (!nft.isReadyToClaim(epoch)) return

        runCatching {
            TransactionManifest.stakesClaim(
                accountAddress = account.address,
                stakeClaims = listOf(
                    SargonStakeClaim(
                        resourceAddress = NonFungibleResourceAddress.init(claim.resourceAddress.string),
                        validatorAddress = claim.validatorAddress,
                        ids = listOf(nft.localId),
                        amount = nft.claimAmountXrd.orZero(),
                    )
                )
            )
        }.mapCatching { manifest ->
            prepareInternalTransactionUseCase(UnvalidatedManifestData.from(manifest))
        }.onSuccess { request ->
            incomingRequestRepository.add(request)
        }
    }
}

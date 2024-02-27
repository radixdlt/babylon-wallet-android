package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.resources.Resource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.ret.ManifestPoet
import java.math.BigDecimal
import javax.inject.Inject

class SendClaimRequestUseCase @Inject constructor(
    val incomingRequestRepository: IncomingRequestRepository
) {

    suspend operator fun invoke(
        account: Network.Account,
        claims: List<StakeClaim>,
        epoch: Long
    ) {
        ManifestPoet
            .buildClaim(
                fromAccount = account,
                claims = claims.mapNotNull { claim ->
                    val nfts = claim.nonFungibleResource.items.filter { it.isReadyToClaim(epoch) }
                    if (nfts.isEmpty()) return@mapNotNull null

                    ManifestPoet.Claim(
                        resourceAddress = claim.resourceAddress,
                        validatorAddress = claim.validatorAddress,
                        claimNFTs = nfts.associate { it.localId.code to (it.claimAmountXrd ?: BigDecimal.ZERO) }
                    )
                }
            ).mapCatching { manifest ->
                manifest.prepareInternalTransactionRequest()
            }
            .onSuccess { request ->
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

        ManifestPoet
            .buildClaim(
                fromAccount = account,
                claims = listOf(
                    ManifestPoet.Claim(
                        resourceAddress = claim.resourceAddress,
                        validatorAddress = claim.validatorAddress,
                        claimNFTs = mapOf(nft.localId.code to (nft.claimAmountXrd ?: BigDecimal.ZERO))
                    )
                )
            ).mapCatching { manifest ->
                manifest.prepareInternalTransactionRequest()
            }
            .onSuccess { request ->
                incomingRequestRepository.add(request)
            }
    }
}

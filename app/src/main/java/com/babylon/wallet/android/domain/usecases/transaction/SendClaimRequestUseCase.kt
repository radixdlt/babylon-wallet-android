package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.radixdlt.ret.Address
import com.radixdlt.ret.ManifestBuilder
import com.radixdlt.ret.ManifestBuilderBucket
import rdx.works.core.toRETDecimal
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.derivation.model.NetworkId
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class SendClaimRequestUseCase @Inject constructor(
    val incomingRequestRepository: IncomingRequestRepository
) {

    suspend operator fun invoke(
        account: Network.Account,
        claims: List<StakeClaim>,
        epoch: Long
    ) {
        var builder = ManifestBuilder()
        var bucketCounter = 0

        claims.forEach { claim ->
            val claimNFTs = claim.nonFungibleResource.items.filter { it.isReadyToClaim(epoch) }
            if (claimNFTs.isEmpty()) return@forEach

            val claimBucket = ManifestBuilderBucket("bucket$bucketCounter").also {
                bucketCounter += 1
            }

            val depositBucket = ManifestBuilderBucket("bucket$bucketCounter").also {
                bucketCounter += 1
            }

            builder = builder.attachClaimInstructions(
                account = account,
                claim = claim,
                claimNFTs = claimNFTs,
                bucket = claimBucket
            ).attachDepositInstructions(
                account = account,
                claimNFTs = claimNFTs,
                bucket = depositBucket
            )
        }

        val request = builder
            .build(account.networkID.toUByte())
            .prepareInternalTransactionRequest(networkId = account.networkID)

        incomingRequestRepository.add(request)
    }

    suspend operator fun invoke(
        account: Network.Account,
        claim: StakeClaim,
        nft: Resource.NonFungibleResource.Item,
        epoch: Long
    ) {
        if (!nft.isReadyToClaim(epoch)) return

        val request = ManifestBuilder()
            .attachClaimInstructions(
                account = account,
                claim = claim,
                claimNFTs = listOf(nft),
                bucket = ManifestBuilderBucket("bucket0")
            )
            .attachDepositInstructions(
                account = account,
                claimNFTs = listOf(nft),
                bucket = ManifestBuilderBucket("bucket1")
            )
            .build(account.networkID.toUByte())
            .prepareInternalTransactionRequest(networkId = account.networkID)

        incomingRequestRepository.add(request)
    }

    private fun ManifestBuilder.attachClaimInstructions(
        account: Network.Account,
        claim: StakeClaim,
        claimNFTs: List<Resource.NonFungibleResource.Item>,
        bucket: ManifestBuilderBucket
    ) = accountWithdrawNonFungibles(
        address = Address(account.address),
        resourceAddress = Address(claim.resourceAddress),
        ids = claimNFTs.map { it.localId.toRetId() }
    ).takeAllFromWorktop(
        resourceAddress = Address(claim.resourceAddress),
        intoBucket = bucket
    ).validatorClaimXrd(
        address = Address(claim.validatorAddress),
        bucket = bucket
    )

    private fun ManifestBuilder.attachDepositInstructions(
        account: Network.Account,
        claimNFTs: List<Resource.NonFungibleResource.Item>,
        bucket: ManifestBuilderBucket
    ): ManifestBuilder {
        val totalClaimValue = claimNFTs.sumOf { it.claimAmountXrd ?: BigDecimal.ZERO }
        val xrdAddress = XrdResource.address(networkId = NetworkId.from(account.networkID))

        return takeFromWorktop(
            resourceAddress = Address(xrdAddress),
            amount = totalClaimValue.toRETDecimal(roundingMode = RoundingMode.HALF_DOWN),
            intoBucket = bucket
        ).accountDeposit(
            address = Address(account.address),
            bucket = bucket
        )
    }
}

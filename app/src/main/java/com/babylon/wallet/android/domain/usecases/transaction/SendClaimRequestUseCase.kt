package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.resources.Resource
import com.radixdlt.ret.Address
import com.radixdlt.ret.ManifestBuilder
import com.radixdlt.ret.ManifestBuilderBucket
import com.radixdlt.ret.NonFungibleLocalId
import rdx.works.profile.data.model.pernetwork.Network
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
            val claimIds = claim.nonFungibleResource.items.filter { it.isReadyToClaim(epoch) }.map {
                it.localId.toRetId()
            }
            if (claimIds.isEmpty()) return@forEach

            val bucket = ManifestBuilderBucket("bucket$bucketCounter").also {
                bucketCounter += 1
            }

            builder = builder.attachInstructions(
                account = account,
                claim = claim,
                claimIds = claimIds,
                bucket = bucket
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
            .attachInstructions(
                account = account,
                claim = claim,
                claimIds = listOf(nft.localId.toRetId()),
                bucket = ManifestBuilderBucket("bucket0")
            )
            .build(account.networkID.toUByte())
            .prepareInternalTransactionRequest(networkId = account.networkID)

        incomingRequestRepository.add(request)
    }

    private fun ManifestBuilder.attachInstructions(
        account: Network.Account,
        claim: StakeClaim,
        claimIds: List<NonFungibleLocalId>,
        bucket: ManifestBuilderBucket
    ) = accountWithdrawNonFungibles(
        address = Address(account.address),
        resourceAddress = Address(claim.resourceAddress),
        ids = claimIds
    ).takeAllFromWorktop(
        resourceAddress = Address(claim.resourceAddress),
        intoBucket = bucket
    ).validatorClaimXrd(
        address = Address(claim.validatorAddress),
        bucket = bucket
    ).accountDepositEntireWorktop(
        accountAddress = Address(account.address)
    )


}

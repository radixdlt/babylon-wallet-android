package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.radixdlt.ret.Address
import com.radixdlt.ret.ManifestBuilder
import com.radixdlt.ret.ManifestBuilderBucket
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class SendClaimRequestUseCase @Inject constructor(
    val incomingRequestRepository: IncomingRequestRepository
) {

    suspend operator fun invoke(
        account: Network.Account,
        claim: StakeClaim,
        epoch: Long
    ) {
        val validatorAddress = claim.validatorAddress
        val request = ManifestBuilder()
            .accountWithdrawNonFungibles(
                address = Address(account.address),
                resourceAddress = Address(claim.resourceAddress),
                ids = claim.nonFungibleResource.items.filter { it.isReadyToClaim(epoch) }.map {
                    it.localId.toRetId()
                }
            )
            .takeAllFromWorktop(
                resourceAddress = Address(claim.resourceAddress),
                intoBucket = ManifestBuilderBucket("bucket0")
            )
            .validatorClaimXrd(
                address = Address(validatorAddress),
                bucket = ManifestBuilderBucket("bucket0")
            )
            .accountDepositEntireWorktop(
                accountAddress = Address(account.address)
            )
            .build(account.networkID.toUByte())
            .prepareInternalTransactionRequest(networkId = account.networkID)

        incomingRequestRepository.add(request)
    }
}

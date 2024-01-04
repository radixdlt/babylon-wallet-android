package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.TransactionType
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork

@Suppress
("LongMethod")
suspend fun TransactionType.UnstakeTransaction.resolve(
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    validators: List<ValidatorDetail>
): PreviewType {
    var finalValidators = listOf<ValidatorDetail>()
    val fromAccounts = mutableListOf<AccountWithTransferableResources>()
    val toAccounts = mutableListOf<AccountWithTransferableResources>()
    unstakes.groupBy { unstake ->
        unstake.fromAccount.addressString()
    }.forEach { unstakesPerAccount ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(unstakesPerAccount.key) ?: error("No account found")
        val withdrawingLsu = unstakesPerAccount.value.groupBy {
            it.validatorAddress.addressString()
        }.map { unstakesPerValidator ->
            val lsuResource =
                resources.find {
                    it.resourceAddress == unstakesPerValidator.value.first().stakeUnitAddress.addressString()
                } as? Resource.FungibleResource ?: error("No resource found")
            val validator = validators.find { it.address == unstakesPerValidator.key } ?: error("No validator found")
            finalValidators = (finalValidators + validator).distinctBy { it.address }
            Transferable.Withdrawing(
                transferable = TransferableResource.LsuAmount(
                    unstakesPerValidator.value.sumOf { it.stakeUnitAmount.asStr().toBigDecimal() },
                    lsuResource,
                    validator,
                    unstakesPerValidator.value.sumOf { it.claimNftData.claimAmount.asStr().toBigDecimal() },
                )
            )
        }
        fromAccounts.add(
            AccountWithTransferableResources.Owned(
                account = ownedAccount,
                resources = withdrawingLsu
            )
        )

        unstakesPerAccount.value.groupBy { it.validatorAddress.addressString() }.map { unstakePerValidator ->
            val validator = validators.find { it.address == unstakePerValidator.key } ?: error("No validator found")
            val depositingNfts = unstakePerValidator.value.groupBy { it.claimNftResource.addressString() }.map { unstakesPerNftResource ->
                val nftResource =
                    resources.find { it.resourceAddress == unstakesPerNftResource.key } as? Resource.NonFungibleResource
                        ?: error("No resource found")
                val stakeClaimNftItems = unstakesPerNftResource.value.map { unstake ->
                    Resource.NonFungibleResource.Item(
                        collectionAddress = unstakesPerNftResource.key,
                        localId = Resource.NonFungibleResource.Item.ID.from(unstake.claimNftLocalId)
                    )
                }
                Transferable.Depositing(
                    transferable = TransferableResource.StakeClaimNft(
                        nftResource.copy(items = stakeClaimNftItems),
                        unstakesPerNftResource.value.associate {
                            it.claimNftLocalId.toString() to it.claimNftData.claimAmount.asStr().toBigDecimal()
                        },
                        validator,
                        false
                    )
                )
            }
            toAccounts.add(
                AccountWithTransferableResources.Owned(
                    account = ownedAccount,
                    resources = depositingNfts
                )
            )
        }
    }
    return PreviewType.Unstake(from = fromAccounts, to = toAccounts, validators = finalValidators)
}

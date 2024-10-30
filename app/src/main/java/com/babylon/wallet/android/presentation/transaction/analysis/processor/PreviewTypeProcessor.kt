package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.RadixWalletException.ResourceCouldNotBeResolvedInTransaction
import com.babylon.wallet.android.presentation.model.Amount
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.model.TransferableX
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.FungibleResourceIndicator
import com.radixdlt.sargon.NewlyCreatedResource
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceIndicator
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.extensions.Accounts
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.ids
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import javax.inject.Inject

@Suppress("LongParameterList")
class PreviewTypeAnalyzer @Inject constructor(
    private val generalTransferProcessor: GeneralTransferProcessor,
    private val transferProcessor: TransferProcessor,
    private val poolContributionProcessor: PoolContributionProcessor,
    private val accountDepositSettingsProcessor: AccountDepositSettingsProcessor,
    private val poolRedemptionProcessor: PoolRedemptionProcessor,
    private val validatorStakeProcessor: ValidatorStakeProcessor,
    private val validatorClaimProcessor: ValidatorClaimProcessor,
    private val validatorUnstakeProcessor: ValidatorUnstakeProcessor
) {
    suspend fun analyze(summary: ExecutionSummary): PreviewType {
        val manifestClass = summary.detailedClassification.firstOrNull { it.isConforming } ?: return PreviewType.NonConforming

        return when (manifestClass) {
            is DetailedManifestClass.General -> generalTransferProcessor.process(summary, manifestClass)
            is DetailedManifestClass.Transfer -> transferProcessor.process(summary, manifestClass)
            is DetailedManifestClass.PoolContribution -> poolContributionProcessor.process(summary, manifestClass)
            is DetailedManifestClass.AccountDepositSettingsUpdate -> accountDepositSettingsProcessor.process(summary, manifestClass)
            is DetailedManifestClass.PoolRedemption -> poolRedemptionProcessor.process(summary, manifestClass)
            is DetailedManifestClass.ValidatorStake -> validatorStakeProcessor.process(summary, manifestClass)
            is DetailedManifestClass.ValidatorClaim -> validatorClaimProcessor.process(summary, manifestClass)
            is DetailedManifestClass.ValidatorUnstake -> validatorUnstakeProcessor.process(summary, manifestClass)
        }
    }

    private val DetailedManifestClass.isConforming: Boolean
        get() = when (this) {
            is DetailedManifestClass.AccountDepositSettingsUpdate -> true
            is DetailedManifestClass.General -> true
            is DetailedManifestClass.PoolContribution -> true
            is DetailedManifestClass.PoolRedemption -> true
            is DetailedManifestClass.Transfer -> true
            is DetailedManifestClass.ValidatorClaim -> true
            is DetailedManifestClass.ValidatorStake -> true
            is DetailedManifestClass.ValidatorUnstake -> true
            else -> false
        }
}

interface PreviewTypeProcessor<C : DetailedManifestClass> {

    suspend fun process(summary: ExecutionSummary, classification: C): PreviewType

    private fun involvedAccounts(summary: ExecutionSummary, profile: Profile): Accounts {
        val involvedAccountAddresses = (summary.withdrawals.keys + summary.deposits.keys)

        val accountsToSearch = profile.activeAccountsOnCurrentNetwork.asIdentifiable();
        return involvedAccountAddresses.mapNotNull { address ->
            accountsToSearch.getBy(address)
        }.asIdentifiable()
    }

    private fun newlyCreatedMetadata(newlyCreated: NewlyCreatedResource): List<Metadata> {
        val metadata = mutableListOf<Metadata>()

        newlyCreated.name?.let {
            metadata.add(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.NAME.key,
                    value = it,
                    valueType = MetadataType.String
                )
            )
        }

        newlyCreated.symbol?.let {
            metadata.add(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.SYMBOL.key,
                    value = it,
                    valueType = MetadataType.String
                )
            )
        }

        newlyCreated.description?.let {
            metadata.add(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.DESCRIPTION.key,
                    value = it,
                    valueType = MetadataType.String
                )
            )
        }

        newlyCreated.iconUrl?.let {
            metadata.add(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.ICON_URL.key,
                    value = it,
                    valueType = MetadataType.Url
                )
            )
        }

        val tags = newlyCreated.tags.map {
            Metadata.Primitive(
                key = ExplicitMetadataKey.TAGS.key,
                value = it,
                valueType = MetadataType.String
            )
        }
        if (tags.isNotEmpty()) {
            metadata.add(
                Metadata.Collection(
                    key = ExplicitMetadataKey.TAGS.key,
                    values = tags
                )
            )
        }

        return metadata
    }

    private fun ResourceIndicator.amount(): Amount = when (this) {
        is ResourceIndicator.Fungible -> when (val fungibleIndicator = indicator) {
            is FungibleResourceIndicator.Guaranteed -> FungibleAmount.Exact(fungibleIndicator.decimal)
            is FungibleResourceIndicator.Predicted -> FungibleAmount.Predicted(
                amount = fungibleIndicator.predictedDecimal.value,
                instructionIndex = fungibleIndicator.predictedDecimal.instructionIndex.toLong(),
                guaranteeOffset = 0.toDecimal192(), // TODO ask that
            )
        }
        // TODO ask that
        is ResourceIndicator.NonFungible -> NonFungibleAmount.NotExact(
            lowerBound = NonFungibleAmount.NotExact.LowerBound.NonZero,
            upperBound = NonFungibleAmount.NotExact.UpperBound.Unbounded
        )
    }

    private fun resolveAsset(
        resourceIndicator: ResourceIndicator,
        summary: ExecutionSummary,
        assets: List<Asset>
    ): Pair<Asset, Boolean> = when (resourceIndicator) {
        is ResourceIndicator.Fungible -> {
            val newEntityMetadata = summary.newEntities.metadata[resourceIndicator.address]
            if (newEntityMetadata != null) {
                Token(
                    resource = Resource.FungibleResource(
                        address = resourceIndicator.resourceAddress,
                        ownedAmount = 0.toDecimal192(), // This amount is irrelevant
                        metadata = newlyCreatedMetadata(newEntityMetadata)
                    )
                ) to true
            } else {
                val asset = assets.find {
                    it.resource.address == resourceIndicator.address
                } as? Asset.Fungible ?: throw ResourceCouldNotBeResolvedInTransaction(
                    ResourceOrNonFungible.Resource(resourceIndicator.resourceAddress)
                )

                asset to false
            }
        }

        is ResourceIndicator.NonFungible -> {
            val ids = resourceIndicator.indicator.ids

            val newEntityMetadata = summary.newEntities.metadata[resourceIndicator.address]
            if (newEntityMetadata != null) {
                NonFungibleCollection(
                    collection = Resource.NonFungibleResource(
                        address = resourceIndicator.resourceAddress,
                        amount = 0, // This amount is irrelevant
                        metadata = newlyCreatedMetadata(newEntityMetadata),
                        items = ids.map {
                            Resource.NonFungibleResource.Item(
                                collectionAddress = resourceIndicator.resourceAddress,
                                localId = it
                            )
                        }
                    )
                ) to true
            } else {
                val nonFungibleAsset = assets.find {
                    it.resource.address == resourceIndicator.address
                } as? Asset.NonFungible ?: throw ResourceCouldNotBeResolvedInTransaction(
                    ResourceOrNonFungible.Resource(resourceIndicator.resourceAddress)
                )

                val onLedgerNFTs = nonFungibleAsset.resource.items.associateBy { it.localId }
                val newlyCreatedNFTs = summary.newlyCreatedNonFungibles.mapNotNull {
                    if (it.resourceAddress == resourceIndicator.resourceAddress) {
                        it.nonFungibleLocalId
                    } else {
                        null
                    }
                }

                val items = ids.map { id ->
                    if (id in newlyCreatedNFTs) {
                        Resource.NonFungibleResource.Item(
                            collectionAddress = resourceIndicator.resourceAddress,
                            localId = id
                        )
                    } else {
                        onLedgerNFTs[id] ?: throw ResourceCouldNotBeResolvedInTransaction(
                            ResourceOrNonFungible.NonFungible(
                                NonFungibleGlobalId(
                                    resourceAddress = resourceIndicator.resourceAddress,
                                    nonFungibleLocalId = id
                                )
                            )
                        )
                    }
                }

                when (nonFungibleAsset) {
                    is NonFungibleCollection -> nonFungibleAsset.copy(
                        collection = nonFungibleAsset.collection.copy(items = items)
                    )

                    is StakeClaim -> nonFungibleAsset.copy(
                        nonFungibleResource = nonFungibleAsset.nonFungibleResource.copy(items = items)
                    )
                } to false
            }
        }
    }

    fun resolveTransferable(
        resourceIndicator: ResourceIndicator,
        summary: ExecutionSummary,
        assets: List<Asset>
    ): TransferableX {
        val (asset, isNewlyCreated) = resolveAsset(
            resourceIndicator = resourceIndicator,
            summary = summary,
            assets = assets
        )

        return when (asset) {
            is Token -> TransferableX.FungibleType.Token(
                asset = asset,
                amount = resourceIndicator.amount() as FungibleAmount,
                isNewlyCreated = isNewlyCreated
            )

            is LiquidStakeUnit -> TransferableX.FungibleType.LSU(
                asset = asset,
                amount = resourceIndicator.amount() as FungibleAmount,
                xrdWorth = 0.toDecimal192(),
                isNewlyCreated = isNewlyCreated
            )

            is PoolUnit -> TransferableX.FungibleType.PoolUnit(
                asset = asset,
                amount = resourceIndicator.amount() as FungibleAmount,
                isNewlyCreated = isNewlyCreated,
                contributionPerResource = emptyMap()
            )

            is NonFungibleCollection -> TransferableX.NonFungibleType.NFTCollection(
                asset = asset,
                amount = resourceIndicator.amount() as NonFungibleAmount,
                isNewlyCreated = isNewlyCreated,
            )

            is StakeClaim -> TransferableX.NonFungibleType.StakeClaim(
                asset = asset,
                amount = resourceIndicator.amount() as NonFungibleAmount,
                xrdWorthPerNftItem = emptyMap(),
                isNewlyCreated = isNewlyCreated
            )
        }
    }

    fun Map<AccountAddress, List<ResourceIndicator>>.resolveAccounts(
        profileAccounts: Accounts,
        summary: ExecutionSummary,
        assets: List<Asset>,
    ) = map { entry ->
        val transferables = entry.value.map { indicator ->
            resolveTransferable(
                resourceIndicator = indicator,
                summary = summary,
                assets = assets
            )
        }

        val profileAccount = profileAccounts.getBy(entry.key)
        if (profileAccount != null) {
            AccountWithTransferableResources.Owned(
                account = profileAccount,
                resources = transferables
            )
        } else {
            AccountWithTransferableResources.Other(
                address = entry.key,
                resources = transferables
            )
        }
    }

    fun resolveWithdrawsAndDeposits(
        summary: ExecutionSummary,
        assets: List<Asset>,
        profile: Profile
    ): Pair<List<AccountWithTransferableResources>, List<AccountWithTransferableResources>> {
        val involvedAccounts = involvedAccounts(summary, profile)

        val withdrawsPerAccount = summary.withdrawals.resolveAccounts(
            profileAccounts = involvedAccounts,
            summary = summary,
            assets = assets,
        )

        val depositsPerAccount = summary.deposits.resolveAccounts(
            profileAccounts = involvedAccounts,
            summary = summary,
            assets = assets,
        )

        return withdrawsPerAccount to depositsPerAccount
    }
}

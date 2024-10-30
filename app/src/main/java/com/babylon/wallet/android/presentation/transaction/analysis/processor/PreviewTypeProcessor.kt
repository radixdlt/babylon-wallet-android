package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.RadixWalletException.ResourceCouldNotBeResolvedInTransaction
import com.babylon.wallet.android.presentation.model.Amount
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.model.TransferableX
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.FungibleResourceIndicator
import com.radixdlt.sargon.NewlyCreatedResource
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceIndicator
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.ResourceSpecifier
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
import rdx.works.core.domain.resources.Badge
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
            is DetailedManifestClass.PoolRedemption -> poolRedemptionProcessor.process(summary, manifestClass)
            is DetailedManifestClass.ValidatorStake -> validatorStakeProcessor.process(summary, manifestClass)
            is DetailedManifestClass.ValidatorClaim -> validatorClaimProcessor.process(summary, manifestClass)
            is DetailedManifestClass.ValidatorUnstake -> validatorUnstakeProcessor.process(summary, manifestClass)
            is DetailedManifestClass.AccountDepositSettingsUpdate -> accountDepositSettingsProcessor.process(summary, manifestClass)
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

    private fun ExecutionSummary.involvedProfileAccounts(profile: Profile): Accounts {
        val involvedAccountAddresses = (withdrawals.keys + deposits.keys)

        val accountsToSearch = profile.activeAccountsOnCurrentNetwork.asIdentifiable();
        return involvedAccountAddresses.mapNotNull { address ->
            accountsToSearch.getBy(address)
        }.asIdentifiable()
    }

    private fun NewlyCreatedResource.toMetadata(): List<Metadata> {
        val metadata = mutableListOf<Metadata>()

        name?.let {
            metadata.add(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.NAME.key,
                    value = it,
                    valueType = MetadataType.String
                )
            )
        }

        symbol?.let {
            metadata.add(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.SYMBOL.key,
                    value = it,
                    valueType = MetadataType.String
                )
            )
        }

        description?.let {
            metadata.add(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.DESCRIPTION.key,
                    value = it,
                    valueType = MetadataType.String
                )
            )
        }

        iconUrl?.let {
            metadata.add(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.ICON_URL.key,
                    value = it,
                    valueType = MetadataType.Url
                )
            )
        }

        val tags = tags.map {
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

    private fun ResourceIndicator.amount(defaultGuaranteeOffset: Decimal192): Amount = when (this) {
        is ResourceIndicator.Fungible -> when (val fungibleIndicator = indicator) {
            is FungibleResourceIndicator.Guaranteed -> FungibleAmount.Exact(fungibleIndicator.decimal)
            is FungibleResourceIndicator.Predicted -> FungibleAmount.Predicted(
                amount = fungibleIndicator.predictedDecimal.value,
                instructionIndex = fungibleIndicator.predictedDecimal.instructionIndex.toLong(),
                guaranteeOffset = defaultGuaranteeOffset
            )
        }
        // TODO ask if we need to keep predicted instruction index
        is ResourceIndicator.NonFungible -> NonFungibleAmount.Exact(
            ids = indicator.ids
        )
    }

    fun ExecutionSummary.resolveBadges(onLedgerAssets: List<Asset>): List<Badge> {
        val proofAddresses = presentedProofs.associateBy { it.address }

        return onLedgerAssets.filter { asset ->
            asset.resource.address in proofAddresses.keys
        }.mapNotNull { asset ->
            val specifier = proofAddresses[asset.resource.address] ?: return@mapNotNull null

            val badgeResource = when (specifier) {
                is ResourceSpecifier.Fungible -> {
                    // In this case we need to attach the amount of the specifier to the resource since it is not resolved by GW
                    (asset.resource as? Resource.FungibleResource)?.copy(ownedAmount = specifier.amount) ?: return@mapNotNull null
                }

                is ResourceSpecifier.NonFungible -> asset.resource
            }

            Badge(resource = badgeResource)
        }
    }

    private fun ExecutionSummary.resolveAsset(
        resourceIndicator: ResourceIndicator,
        onLedgerAssets: List<Asset>
    ): Pair<Asset, Boolean> = when (resourceIndicator) {
        is ResourceIndicator.Fungible -> {
            val newEntityMetadata = newEntities.metadata[resourceIndicator.address]
            if (newEntityMetadata != null) {
                Token(
                    resource = Resource.FungibleResource(
                        address = resourceIndicator.resourceAddress,
                        ownedAmount = 0.toDecimal192(), // This amount is irrelevant
                        metadata = newEntityMetadata.toMetadata()
                    )
                ) to true
            } else {
                val asset = onLedgerAssets.find {
                    it.resource.address == resourceIndicator.address
                } as? Asset.Fungible ?: throw ResourceCouldNotBeResolvedInTransaction(
                    ResourceOrNonFungible.Resource(resourceIndicator.resourceAddress)
                )

                asset to false
            }
        }

        is ResourceIndicator.NonFungible -> {
            val ids = resourceIndicator.indicator.ids

            val newEntityMetadata = newEntities.metadata[resourceIndicator.address]
            if (newEntityMetadata != null) {
                NonFungibleCollection(
                    collection = Resource.NonFungibleResource(
                        address = resourceIndicator.resourceAddress,
                        amount = 0, // This amount is irrelevant
                        metadata = newEntityMetadata.toMetadata(),
                        items = ids.map {
                            Resource.NonFungibleResource.Item(
                                collectionAddress = resourceIndicator.resourceAddress,
                                localId = it
                            )
                        }
                    )
                ) to true
            } else {
                val nonFungibleAsset = onLedgerAssets.find {
                    it.resource.address == resourceIndicator.address
                } as? Asset.NonFungible ?: throw ResourceCouldNotBeResolvedInTransaction(
                    ResourceOrNonFungible.Resource(resourceIndicator.resourceAddress)
                )

                val onLedgerNFTs = nonFungibleAsset.resource.items.associateBy { it.localId }
                val newlyCreatedNFTs = newlyCreatedNonFungibles.mapNotNull {
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

    private fun ExecutionSummary.resolveTransferable(
        resourceIndicator: ResourceIndicator,
        onLedgerAssets: List<Asset>,
        defaultDepositGuarantee: Decimal192
    ): TransferableX {
        val (asset, isNewlyCreated) = resolveAsset(
            resourceIndicator = resourceIndicator,
            onLedgerAssets = onLedgerAssets
        )

        return when (asset) {
            is Token -> TransferableX.FungibleType.Token(
                asset = asset,
                amount = resourceIndicator.amount(defaultDepositGuarantee) as FungibleAmount,
                isNewlyCreated = isNewlyCreated
            )

            is LiquidStakeUnit -> TransferableX.FungibleType.LSU(
                asset = asset,
                amount = resourceIndicator.amount(defaultDepositGuarantee) as FungibleAmount,
                xrdWorth = 0.toDecimal192(),
                isNewlyCreated = isNewlyCreated
            )

            is PoolUnit -> TransferableX.FungibleType.PoolUnit(
                asset = asset,
                amount = resourceIndicator.amount(defaultDepositGuarantee) as FungibleAmount,
                isNewlyCreated = isNewlyCreated,
                contributionPerResource = emptyMap()
            )

            is NonFungibleCollection -> TransferableX.NonFungibleType.NFTCollection(
                asset = asset,
                amount = resourceIndicator.amount(defaultDepositGuarantee) as NonFungibleAmount,
                isNewlyCreated = isNewlyCreated,
            )

            is StakeClaim -> TransferableX.NonFungibleType.StakeClaim(
                asset = asset,
                amount = resourceIndicator.amount(defaultDepositGuarantee) as NonFungibleAmount,
                xrdWorthPerNftItem = emptyMap(),
                isNewlyCreated = isNewlyCreated
            )
        }
    }

    private fun ExecutionSummary.resolveAccounts(
        profileAccounts: Accounts,
        resourceIndicators: Map<AccountAddress, List<ResourceIndicator>>,
        onLedgerAssets: List<Asset>,
        defaultDepositGuarantee: Decimal192
    ) = resourceIndicators.map { entry ->
        val transferables = entry.value.map { indicator ->
            resolveTransferable(
                resourceIndicator = indicator,
                onLedgerAssets = onLedgerAssets,
                defaultDepositGuarantee = defaultDepositGuarantee
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

    fun ExecutionSummary.resolveWithdrawsAndDeposits(
        onLedgerAssets: List<Asset>,
        profile: Profile
    ): Pair<List<AccountWithTransferableResources>, List<AccountWithTransferableResources>> {
        val involvedAccounts = involvedProfileAccounts(profile)
        val defaultDepositGuarantee = profile.appPreferences.transaction.defaultDepositGuarantee;

        val withdrawsPerAccount = resolveAccounts(
            profileAccounts = involvedAccounts,
            resourceIndicators = withdrawals,
            onLedgerAssets = onLedgerAssets,
            defaultDepositGuarantee = 0.toDecimal192()
        )

        val depositsPerAccount = resolveAccounts(
            profileAccounts = involvedAccounts,
            resourceIndicators = deposits,
            onLedgerAssets = onLedgerAssets,
            defaultDepositGuarantee = defaultDepositGuarantee
        )

        return withdrawsPerAccount to depositsPerAccount
    }
}

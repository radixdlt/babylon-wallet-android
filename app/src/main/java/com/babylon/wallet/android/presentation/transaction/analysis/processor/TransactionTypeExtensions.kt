@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.FungibleResourceIndicator
import com.radixdlt.ret.MetadataValue
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.NonFungibleResourceIndicator
import com.radixdlt.ret.PublicKey
import com.radixdlt.ret.PublicKeyHash
import com.radixdlt.ret.ResourceIndicator
import com.radixdlt.ret.ResourceOrNonFungible
import com.radixdlt.ret.ResourceSpecifier
import com.radixdlt.ret.nonFungibleLocalIdAsStr
import com.radixdlt.ret.nonFungibleLocalIdFromStr
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.sumOf
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Resource.NonFungibleResource.Item
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.toHexString
import rdx.works.profile.data.model.pernetwork.Network

fun ExecutionSummary.involvedFungibleAddresses(excludeNewlyCreated: Boolean = true): Set<ResourceAddress> {
    val withdrawIndicators = accountWithdraws.values.flatten().filterIsInstance<ResourceIndicator.Fungible>()
    val depositIndicators = accountDeposits.values.flatten().filterIsInstance<ResourceIndicator.Fungible>()
    return (withdrawIndicators + depositIndicators)
        .filterNot {
            excludeNewlyCreated && it.isNewlyCreated(this)
        }.map {
            ResourceAddress.init(it.resourceAddress.addressString())
        }.toSet()
}

fun ExecutionSummary.involvedNonFungibleIds(
    excludeNewlyCreated: Boolean = true
): Map<ResourceAddress, Set<com.radixdlt.sargon.NonFungibleLocalId>> {
    val withdrawIndicators = accountWithdraws.values.flatten().filterIsInstance<ResourceIndicator.NonFungible>()
    val depositIndicators = accountDeposits.values.flatten().filterIsInstance<ResourceIndicator.NonFungible>()
    return (withdrawIndicators + depositIndicators).filterNot {
        excludeNewlyCreated && it.isNewlyCreated(this)
    }.fold(mutableMapOf(), operation = { acc, indicator ->
        val indicatorAddress = ResourceAddress.init(indicator.resourceAddress.addressString())
        acc.apply {
            if (containsKey(indicatorAddress)) {
                this[indicatorAddress] = this[indicatorAddress].orEmpty() + indicator.nonFungibleLocalIds
            } else {
                this[indicatorAddress] = indicator.nonFungibleLocalIds.toSet()
            }
        }
    })
}

val ResourceIndicator.resourceAddress: ResourceAddress
    get() = when (this) {
        is ResourceIndicator.Fungible -> ResourceAddress.init(resourceAddress.addressString())
        is ResourceIndicator.NonFungible -> ResourceAddress.init(resourceAddress.addressString())
    }

val ResourceIndicator.amount: Decimal192
    get() = when (this) {
        is ResourceIndicator.Fungible -> {
            when (val specificIndicator = indicator) {
                is FungibleResourceIndicator.Guaranteed -> {
                    specificIndicator.amount.asStr().toDecimal192()
                }

                is FungibleResourceIndicator.Predicted -> specificIndicator.predictedAmount.value.asStr().toDecimal192()
            }
        }

        is ResourceIndicator.NonFungible -> {
            nonFungibleLocalIds.size.toDecimal192()
        }
    }

fun ResourceIndicator.guaranteeType(defaultGuarantee: Double) = when (this) {
    is ResourceIndicator.Fungible -> when (val indicator = indicator) {
        is FungibleResourceIndicator.Guaranteed -> GuaranteeType.Guaranteed
        is FungibleResourceIndicator.Predicted -> GuaranteeType.Predicted(
            instructionIndex = indicator.predictedAmount.instructionIndex.toLong(),
            guaranteeOffset = defaultGuarantee
        )
    }

    is ResourceIndicator.NonFungible -> when (val indicator = indicator) {
        is NonFungibleResourceIndicator.ByAll -> GuaranteeType.Predicted(
            instructionIndex = indicator.predictedIds.instructionIndex.toLong(),
            guaranteeOffset = defaultGuarantee
        )

        is NonFungibleResourceIndicator.ByAmount -> GuaranteeType.Predicted(
            instructionIndex = indicator.predictedIds.instructionIndex.toLong(),
            guaranteeOffset = defaultGuarantee
        )

        is NonFungibleResourceIndicator.ByIds -> GuaranteeType.Guaranteed
    }
}

val ResourceSpecifier.resourceAddress: String
    get() = when (this) {
        is ResourceSpecifier.Amount -> resourceAddress.addressString()
        is ResourceSpecifier.Ids -> resourceAddress.addressString()
    }

val ResourceOrNonFungible.resourceAddress: String
    get() = when (this) {
        is ResourceOrNonFungible.NonFungible -> value.resourceAddress().addressString()
        is ResourceOrNonFungible.Resource -> value.addressString()
    }

fun ResourceIndicator.toTransferableAsset(
    assets: List<Asset>,
    aggregateAmount: Decimal192? = null
): TransferableAsset = when (this) {
    is ResourceIndicator.Fungible -> toTransferableAsset(assets, aggregateAmount)
    is ResourceIndicator.NonFungible -> toTransferableAsset(assets)
}

@Suppress("CyclomaticComplexMethod")
private fun ResourceIndicator.Fungible.toTransferableAsset(
    assets: List<Asset>,
    aggregateAmount: Decimal192? = null
): TransferableAsset.Fungible = when (val asset = assets.find { it.resource.address.string == resourceAddress.addressString() }) {
    is PoolUnit -> {
        val assetWithAmount = asset.copy(
            stake = asset.stake.copy(ownedAmount = aggregateAmount ?: amount),
            pool = asset.pool
        )
        TransferableAsset.Fungible.PoolUnitAsset(
            amount = aggregateAmount ?: amount,
            unit = assetWithAmount,
            contributionPerResource = assetWithAmount.pool?.resources?.associate {
                it.address to (assetWithAmount.resourceRedemptionValue(it).orZero())
            }.orEmpty(),
            isNewlyCreated = false
        )
    }

    is LiquidStakeUnit -> {
        val assetWithAmount = asset.copy(fungibleResource = asset.fungibleResource.copy(ownedAmount = aggregateAmount ?: amount))
        TransferableAsset.Fungible.LSUAsset(
            amount = aggregateAmount ?: amount,
            lsu = assetWithAmount,
            xrdWorth = assetWithAmount.stakeValueInXRD(asset.validator.totalXrdStake).orZero(),
            isNewlyCreated = false
        )
    }

    is Token -> {
        val resourceWithAmount = asset.resource.copy(ownedAmount = aggregateAmount ?: amount)
        TransferableAsset.Fungible.Token(
            amount = aggregateAmount ?: amount,
            resource = resourceWithAmount,
            isNewlyCreated = false
        )
    }

    else -> {
        val resourceWithAmount = Resource.FungibleResource(
            address = ResourceAddress.init(resourceAddress.addressString()),
            ownedAmount = aggregateAmount ?: amount
        )
        TransferableAsset.Fungible.Token(
            amount = aggregateAmount ?: amount,
            resource = resourceWithAmount,
            isNewlyCreated = false
        )
    }
}

private fun ResourceIndicator.NonFungible.toTransferableAsset(
    assets: List<Asset>
): TransferableAsset.NonFungible = when (val asset = assets.find { it.resource.address.string == resourceAddress.addressString() }) {
    is StakeClaim -> {
        val items = nonFungibleLocalIds.map { localId ->
            asset.nonFungibleResource.items.find { item ->
                item.localId == localId
            } ?: Item(
                collectionAddress = ResourceAddress.init(resourceAddress.addressString()),
                localId = localId
            )
        }
        val assetWithItems = asset.copy(nonFungibleResource = asset.nonFungibleResource.copy(items = items))

        TransferableAsset.NonFungible.StakeClaimAssets(
            claim = assetWithItems,
            xrdWorthPerNftItem = items.associate { it.localId to it.claimAmountXrd.orZero() },
            isNewlyCreated = false
        )
    }

    is NonFungibleCollection -> {
        val items = nonFungibleLocalIds.map { localId ->
            asset.collection.items.find { item ->
                item.localId == localId
            } ?: Item(
                collectionAddress = ResourceAddress.init(resourceAddress.addressString()),
                localId = localId
            )
        }

        TransferableAsset.NonFungible.NFTAssets(
            resource = asset.collection.copy(items = items),
            isNewlyCreated = false
        )
    }

    else -> {
        val items = nonFungibleLocalIds.map { localId ->
            Item(
                collectionAddress = ResourceAddress.init(resourceAddress.addressString()),
                localId = localId
            )
        }

        TransferableAsset.NonFungible.NFTAssets(
            resource = Resource.NonFungibleResource(
                address = ResourceAddress.init(resourceAddress.addressString()),
                amount = items.size.toLong(),
                items = items
            ),
            isNewlyCreated = false
        )
    }
}

fun ResourceIndicator.isNewlyCreated(summary: ExecutionSummary) = summary.newEntities.resourceAddresses.any {
    it.addressString() == resourceAddress.string
}

fun ResourceIndicator.newlyCreatedMetadata(summary: ExecutionSummary) = summary.newEntities.metadata[resourceAddress.string].orEmpty()

fun ResourceIndicator.toNewlyCreatedTransferableAsset(
    metadata: Map<String, MetadataValue?>,
    aggregateAmount: Decimal192? = null
): TransferableAsset {
    val metadataItems = metadata.toMetadata()

    return when (this) {
        is ResourceIndicator.Fungible -> TransferableAsset.Fungible.Token(
            amount = aggregateAmount ?: amount,
            resource = Resource.FungibleResource(
                address = ResourceAddress.init(resourceAddress.addressString()),
                ownedAmount = aggregateAmount ?: amount,
                metadata = metadataItems
            ),
            isNewlyCreated = true
        )

        is ResourceIndicator.NonFungible -> {
            val items = nonFungibleLocalIds.map { localId ->
                Item(
                    collectionAddress = ResourceAddress.init(resourceAddress.addressString()),
                    localId = localId
                )
            }

            TransferableAsset.NonFungible.NFTAssets(
                resource = Resource.NonFungibleResource(
                    address = ResourceAddress.init(resourceAddress.addressString()),
                    amount = items.size.toLong(),
                    items = items,
                    metadata = metadataItems
                ),
                isNewlyCreated = true
            )
        }
    }
}

private val ResourceIndicator.Fungible.amount: Decimal192
    get() = when (val specificIndicator = indicator) {
        is FungibleResourceIndicator.Guaranteed -> specificIndicator.amount.asStr().toDecimal192()
        is FungibleResourceIndicator.Predicted -> specificIndicator.predictedAmount.value.asStr().toDecimal192()
    }

val ResourceIndicator.NonFungible.nonFungibleLocalIds: List<com.radixdlt.sargon.NonFungibleLocalId>
    get() = when (val indicator = indicator) {
        is NonFungibleResourceIndicator.ByAll -> indicator.predictedIds.value
        is NonFungibleResourceIndicator.ByAmount -> indicator.predictedIds.value
        is NonFungibleResourceIndicator.ByIds -> indicator.ids
    }.let { retIds ->
        retIds.map { com.radixdlt.sargon.NonFungibleLocalId.init(it.asStr()) }
    }

fun Map<String, MetadataValue?>.toMetadata(): List<Metadata> = mapNotNull { it.toMetadata() }

@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun Map.Entry<String, MetadataValue?>.toMetadata(): Metadata? = when (val typed = value) {
    is MetadataValue.BoolValue -> Metadata.Primitive(
        key = key,
        value = typed.value.toString(),
        valueType = MetadataType.Bool
    )

    is MetadataValue.BoolArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it.toString(),
                valueType = MetadataType.Bool
            )
        }
    )

    is MetadataValue.DecimalValue -> Metadata.Primitive(
        key = key,
        value = typed.value.asStr(),
        valueType = MetadataType.Decimal
    )

    is MetadataValue.DecimalArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it.asStr(),
                valueType = MetadataType.Decimal
            )
        }
    )

    is MetadataValue.GlobalAddressValue -> Metadata.Primitive(
        key = key,
        value = typed.value.addressString(),
        valueType = MetadataType.Address
    )

    is MetadataValue.GlobalAddressArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it.addressString(),
                valueType = MetadataType.Address
            )
        },
    )

    is MetadataValue.I32Value -> Metadata.Primitive(
        key = key,
        value = typed.value.toString(),
        valueType = MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.INT)
    )

    is MetadataValue.I32ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it.toString(),
                valueType = MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.INT)
            )
        }
    )

    is MetadataValue.I64Value -> Metadata.Primitive(
        key = key,
        value = typed.value.toString(),
        valueType = MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.LONG)
    )

    is MetadataValue.I64ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it.toString(),
                valueType = MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.LONG)
            )
        }
    )

    is MetadataValue.U8Value -> Metadata.Primitive(
        key = key,
        value = typed.value.toString(),
        valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.INT)
    )

    is MetadataValue.U8ArrayValue -> Metadata.Primitive(
        key = key,
        value = typed.value.toHexString(),
        valueType = MetadataType.Bytes
    )

    is MetadataValue.U32Value -> Metadata.Primitive(
        key = key,
        value = typed.value.toString(),
        valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.INT)
    )

    is MetadataValue.U32ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it.toString(),
                valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.INT)
            )
        }
    )

    is MetadataValue.U64Value -> Metadata.Primitive(
        key = key,
        value = typed.value.toString(),
        valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.LONG)
    )

    is MetadataValue.U64ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it.toString(),
                valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.LONG)
            )
        },
    )

    is MetadataValue.InstantValue -> Metadata.Primitive(
        key = key,
        value = typed.value.toString(),
        valueType = MetadataType.Instant
    )

    is MetadataValue.InstantArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it.toString(),
                valueType = MetadataType.Instant
            )
        }
    )

    is MetadataValue.NonFungibleGlobalIdValue -> Metadata.Primitive(
        key = key,
        value = "${typed.value.resourceAddress().addressString()}:${typed.value.localId().asStr()}",
        valueType = MetadataType.NonFungibleGlobalId
    )

    is MetadataValue.NonFungibleGlobalIdArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = "${it.resourceAddress().addressString()}:${it.localId().asStr()}",
                valueType = MetadataType.NonFungibleGlobalId
            )
        }
    )

    is MetadataValue.NonFungibleLocalIdValue -> Metadata.Primitive(
        key = key,
        value = typed.value.asStr(),
        valueType = MetadataType.NonFungibleLocalId
    )

    is MetadataValue.NonFungibleLocalIdArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it.asStr(),
                valueType = MetadataType.NonFungibleLocalId
            )
        }
    )

    is MetadataValue.OriginValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Url
    )

    is MetadataValue.OriginArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Url
            )
        }
    )

    is MetadataValue.StringValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.String
    )

    is MetadataValue.StringArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.String
            )
        }
    )

    is MetadataValue.UrlValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Url
    )

    is MetadataValue.UrlArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Url
            )
        }
    )

    is MetadataValue.PublicKeyValue -> when (val publicKey = typed.value) {
        is PublicKey.Secp256k1 -> Metadata.Primitive(
            key = key,
            value = publicKey.value.toHexString(),
            valueType = MetadataType.PublicKeyEcdsaSecp256k1
        )

        is PublicKey.Ed25519 -> Metadata.Primitive(
            key = key,
            value = publicKey.value.toHexString(),
            valueType = MetadataType.PublicKeyEddsaEd25519
        )
    }

    is MetadataValue.PublicKeyArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map { publicKey ->
            when (publicKey) {
                is PublicKey.Secp256k1 -> Metadata.Primitive(
                    key = key,
                    value = publicKey.value.toHexString(),
                    valueType = MetadataType.PublicKeyEcdsaSecp256k1
                )

                is PublicKey.Ed25519 -> Metadata.Primitive(
                    key = key,
                    value = publicKey.value.toHexString(),
                    valueType = MetadataType.PublicKeyEddsaEd25519
                )
            }
        }
    )

    is MetadataValue.PublicKeyHashValue -> when (val publicKeyHash = typed.value) {
        is PublicKeyHash.Secp256k1 -> Metadata.Primitive(
            key = key,
            value = publicKeyHash.value.toHexString(),
            valueType = MetadataType.PublicKeyHashEcdsaSecp256k1
        )

        is PublicKeyHash.Ed25519 -> Metadata.Primitive(
            key = key,
            value = publicKeyHash.value.toHexString(),
            valueType = MetadataType.PublicKeyHashEddsaEd25519
        )
    }

    is MetadataValue.PublicKeyHashArrayValue -> Metadata.Collection(
        key = key,
        values = typed.value.map { publicKeyHash ->
            when (publicKeyHash) {
                is PublicKeyHash.Secp256k1 -> Metadata.Primitive(
                    key = key,
                    value = publicKeyHash.value.toHexString(),
                    valueType = MetadataType.PublicKeyHashEcdsaSecp256k1
                )

                is PublicKeyHash.Ed25519 -> Metadata.Primitive(
                    key = key,
                    value = publicKeyHash.value.toHexString(),
                    valueType = MetadataType.PublicKeyHashEddsaEd25519
                )
            }
        }
    )

    else -> null
}

fun List<Transferable>.toAccountWithTransferableResources(
    accountAddress: AccountAddress,
    ownedAccounts: List<Network.Account>
): AccountWithTransferableResources {
    val ownedAccount = ownedAccounts.find { it.address == accountAddress.string }
    return if (ownedAccount != null) {
        AccountWithTransferableResources.Owned(ownedAccount, this)
    } else {
        AccountWithTransferableResources.Other(accountAddress, this)
    }
}

fun ExecutionSummary.involvedOwnedAccounts(ownedAccounts: List<Network.Account>): List<Network.Account> {
    val involvedAccountAddresses = accountWithdraws.keys + accountDeposits.keys
    return ownedAccounts.filter {
        involvedAccountAddresses.contains(it.address)
    }
}

fun ExecutionSummary.toWithdrawingAccountsWithTransferableAssets(
    involvedAssets: List<Asset>,
    allOwnedAccounts: List<Network.Account>,
    aggregateByResourceAddress: Boolean = false // for now I turned aggregation off to not introduce any bugs
): List<AccountWithTransferableResources> {
    return accountWithdraws.map { withdrawEntry ->
        if (aggregateByResourceAddress) {
            withdrawEntry.value.groupBy { it.resourceAddress }.map { indicatorsPerResource ->
                val resources = indicatorsPerResource.value
                when (val first = resources.first()) {
                    is ResourceIndicator.Fungible -> {
                        val aggregateAmount = resources.map { it.amount }.sumOf { it }
                        if (first.isNewlyCreated(summary = this)) {
                            first.toNewlyCreatedTransferableAsset(first.newlyCreatedMetadata(summary = this), aggregateAmount)
                        } else {
                            first.toTransferableAsset(involvedAssets, aggregateAmount)
                        }
                    }

                    is ResourceIndicator.NonFungible -> {
                        val nonFungibleLocalIds = resources.filterIsInstance<ResourceIndicator.NonFungible>()
                            .fold(setOf<NonFungibleLocalId>(), operation = { acc, resource ->
                                acc + resource.nonFungibleLocalIds.map { nonFungibleLocalIdFromStr(it.string) }.toSet()
                            })
                        val resource = first.copy(indicator = NonFungibleResourceIndicator.ByIds(nonFungibleLocalIds.toList()))
                        if (resource.isNewlyCreated(summary = this)) {
                            resource.toNewlyCreatedTransferableAsset(first.newlyCreatedMetadata(summary = this))
                        } else {
                            resource.toTransferableAsset(involvedAssets)
                        }
                    }
                }
            }
        } else {
            withdrawEntry.value.map { resource ->
                if (resource.isNewlyCreated(summary = this)) {
                    resource.toNewlyCreatedTransferableAsset(resource.newlyCreatedMetadata(summary = this))
                } else {
                    resource.toTransferableAsset(involvedAssets)
                }
            }
        }.map {
            Transferable.Withdrawing(it)
        }.toAccountWithTransferableResources(
            AccountAddress.init(withdrawEntry.key),
            allOwnedAccounts
        )
    }.sortedWith(AccountWithTransferableResources.Companion.Sorter(allOwnedAccounts))
}

fun ExecutionSummary.resolveDepositingAsset(
    resourceIndicator: ResourceIndicator,
    involvedAssets: List<Asset>,
    defaultDepositGuarantee: Double,
    aggregateAmount: Decimal192? = null
): Transferable.Depositing {
    val asset = if (resourceIndicator.isNewlyCreated(summary = this)) {
        resourceIndicator.toNewlyCreatedTransferableAsset(resourceIndicator.newlyCreatedMetadata(summary = this))
    } else {
        resourceIndicator.toTransferableAsset(involvedAssets, aggregateAmount)
    }

    return Transferable.Depositing(
        transferable = asset,
        guaranteeType = resourceIndicator.guaranteeType(defaultDepositGuarantee)
    )
}

fun ExecutionSummary.toDepositingAccountsWithTransferableAssets(
    involvedAssets: List<Asset>,
    allOwnedAccounts: List<Network.Account>,
    defaultGuarantee: Double
) = accountDeposits.map { withdrawEntry ->
    withdrawEntry.value.map { resource ->
        resolveDepositingAsset(resource, involvedAssets, defaultGuarantee)
    }.toAccountWithTransferableResources(
        AccountAddress.init(withdrawEntry.key),
        allOwnedAccounts
    )
}.sortedWith(AccountWithTransferableResources.Companion.Sorter(allOwnedAccounts))

private fun NonFungibleLocalId.asStr() = nonFungibleLocalIdAsStr(this)

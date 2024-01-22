@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.NonFungibleCollection
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.findFungible
import com.babylon.wallet.android.domain.model.resources.findNonFungible
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.radixdlt.ret.DetailedManifestClass
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
import rdx.works.core.ret.asStr
import rdx.works.core.toHexString
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

val ExecutionSummary.involvedFungibleAddresses
    get() = accountWithdraws.values.flatten().involvedFungibleAddresses.toSet() +
        accountDeposits.values.flatten().involvedFungibleAddresses.toSet()

val ExecutionSummary.involvedNonFungibleIds
    get() = accountWithdraws.values.flatten().involvedNonFungibleIds +
        accountDeposits.values.flatten().involvedNonFungibleIds

private val List<ResourceIndicator>.involvedFungibleAddresses
    get() = mapNotNull { (it as? ResourceIndicator.Fungible)?.resourceAddress?.addressString() }

private val List<ResourceIndicator>.involvedNonFungibleIds
    get() = mapNotNull { it as? ResourceIndicator.NonFungible }.associate {
        it.resourceAddress.addressString() to it.localIds
    }

val ResourceIndicator.resourceAddress: String
    get() = when (this) {
        is ResourceIndicator.Fungible -> resourceAddress.addressString()
        is ResourceIndicator.NonFungible -> resourceAddress.addressString()
    }

val ResourceIndicator.NonFungible.localIds: List<String>
    get() = indicator.nonFungibleLocalIds.map { nonFungibleLocalIdAsStr(it) }

val ResourceIndicator.amount: BigDecimal
    get() = when (this) {
        is ResourceIndicator.Fungible -> {
            when (val specificIndicator = indicator) {
                is FungibleResourceIndicator.Guaranteed -> {
                    specificIndicator.amount.asStr().toBigDecimal()
                }

                is FungibleResourceIndicator.Predicted -> specificIndicator.predictedAmount.value.asStr().toBigDecimal()
            }
        }

        is ResourceIndicator.NonFungible -> {
            BigDecimal(indicator.nonFungibleLocalIds.size)
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

val DetailedManifestClass.involvedValidatorAddresses: Set<String>
    get() = when (this) {
        is DetailedManifestClass.ValidatorClaim -> validatorAddresses.map { it.addressString() }.toSet()

        is DetailedManifestClass.ValidatorUnstake -> validatorAddresses.map { it.addressString() }.toSet()

        is DetailedManifestClass.ValidatorStake -> validatorAddresses.map { it.addressString() }.toSet()
        else -> emptySet()
    }

data class StakeClaimAddressData(
    val resourceAddress: String,
    val localId: Set<String>
)

val ExecutionSummary.involvedStakeClaims: Set<StakeClaimAddressData>
    get() {
        if (detailedClassification.isEmpty()) return emptySet()
        return when (detailedClassification.first()) {
            is DetailedManifestClass.ValidatorClaim -> {
                accountWithdraws.values.flatten().filterIsInstance<ResourceIndicator.NonFungible>().map { nonFungibleIndicator ->
                    StakeClaimAddressData(
                        resourceAddress = nonFungibleIndicator.resourceAddress.addressString(),
                        localId = nonFungibleIndicator.indicator.nonFungibleLocalIds.map { nonFungibleLocalIdAsStr(it) }.toSet()
                    )
                }.toSet()
            }

            else -> emptySet()
        }
    }

val ExecutionSummary.involvedResourceAddresses: Set<String>
    get() = accountDeposits.values.map { resourceIndicator ->
        resourceIndicator.map { it.resourceAddress }
    }.flatten().toSet() union accountWithdraws.values.map { resourceIndicator ->
        resourceIndicator.map {
            when (it) {
                is ResourceIndicator.Fungible -> it.resourceAddress.addressString()
                is ResourceIndicator.NonFungible -> it.resourceAddress.addressString()
            }
        }
    }.flatten().toSet()

fun ResourceIndicator.toTransferableAsset(
    assets: List<Asset>
): TransferableAsset {
    val asset = assets.find { it.resource.resourceAddress == resourceAddress }

    return when (this) {
        is ResourceIndicator.Fungible -> when (asset) {
            is PoolUnit -> {
                val assetWithAmount = asset.copy(
                    stake = asset.stake.copy(ownedAmount = amount),
                    pool = asset.pool?.copy(associatedDApp = null) // TODO FIX THIS
                )
                TransferableAsset.Fungible.PoolUnitAsset(
                    amount = amount,
                    unit = assetWithAmount,
                    contributionPerResource = assetWithAmount.pool?.resources?.associate {
                        it.resourceAddress to (assetWithAmount.resourceRedemptionValue(it) ?: BigDecimal.ZERO)
                    }.orEmpty(),
                    isNewlyCreated = false
                )
            }

            is LiquidStakeUnit -> {
                val assetWithAmount = asset.copy(fungibleResource = asset.fungibleResource.copy(ownedAmount = amount))
                TransferableAsset.Fungible.LSUAsset(
                    amount = amount,
                    lsu = assetWithAmount,
                    xrdWorth = assetWithAmount.stakeValueInXRD(asset.validator.totalXrdStake) ?: BigDecimal.ZERO,
                    isNewlyCreated = false
                )
            }

            is Token -> {
                val resourceWithAmount = asset.resource.copy(ownedAmount = amount)
                TransferableAsset.Fungible.Token(
                    amount = amount,
                    resource = resourceWithAmount,
                    isNewlyCreated = false
                )
            }

            else -> {
                val resourceWithAmount = Resource.FungibleResource(
                    resourceAddress = resourceAddress.addressString(),
                    ownedAmount = amount
                )
                TransferableAsset.Fungible.Token(
                    amount = amount,
                    resource = resourceWithAmount,
                    isNewlyCreated = false
                )
            }
        }

        is ResourceIndicator.NonFungible -> when (asset) {
            is StakeClaim -> {
                val items = indicator.nonFungibleLocalIds.map {
                    val localId = Resource.NonFungibleResource.Item.ID.from(it)

                    asset.nonFungibleResource.items.find { item ->
                        item.localId == localId
                    } ?: Resource.NonFungibleResource.Item(
                        collectionAddress = resourceAddress.addressString(),
                        localId = localId
                    )
                }
                val assetWithItems = asset.copy(nonFungibleResource = asset.nonFungibleResource.copy(items = items))

                TransferableAsset.NonFungible.StakeClaimAssets(
                    claim = assetWithItems,
                    xrdWorthPerNftItem = items.associate { it.localId.displayable to (it.claimAmountXrd ?: BigDecimal.ZERO) },
                    isNewlyCreated = false
                )
            }

            is NonFungibleCollection -> {
                val items = indicator.nonFungibleLocalIds.map {
                    val localId = Resource.NonFungibleResource.Item.ID.from(it)

                    asset.collection.items.find { item ->
                        item.localId == localId
                    } ?: Resource.NonFungibleResource.Item(
                        collectionAddress = resourceAddress.addressString(),
                        localId = localId
                    )
                }

                TransferableAsset.NonFungible.NFTAssets(
                    resource = asset.collection.copy(items = items),
                    isNewlyCreated = false
                )
            }

            else -> {
                val items = indicator.nonFungibleLocalIds.map { localId ->
                    Resource.NonFungibleResource.Item(
                        collectionAddress = resourceAddress.addressString(),
                        localId = Resource.NonFungibleResource.Item.ID.from(localId)
                    )
                }

                TransferableAsset.NonFungible.NFTAssets(
                    resource = Resource.NonFungibleResource(
                        resourceAddress = resourceAddress.addressString(),
                        amount = items.size.toLong(),
                        items = items
                    ),
                    isNewlyCreated = false
                )
            }
        }
    }
}

fun ResourceIndicator.isNewlyCreated(summary: ExecutionSummary) = summary.newEntities.resourceAddresses.any {
    it.addressString() == resourceAddress
}

fun ResourceIndicator.newlyCreatedMetadata(summary: ExecutionSummary) = summary.newEntities.metadata[resourceAddress].orEmpty()

fun ResourceIndicator.toNewlyCreatedTransferableAsset(
    metadata: Map<String, MetadataValue?>
): TransferableAsset {
    val metadataItems = metadata.toMetadata()

    return when (this) {
        is ResourceIndicator.Fungible -> TransferableAsset.Fungible.Token(
            amount = amount,
            resource = Resource.FungibleResource(
                resourceAddress = resourceAddress.addressString(),
                ownedAmount = amount,
                metadata = metadataItems
            ),
            isNewlyCreated = true
        )

        is ResourceIndicator.NonFungible -> {
            val items = indicator.nonFungibleLocalIds.map { localId ->
                Resource.NonFungibleResource.Item(
                    collectionAddress = resourceAddress.addressString(),
                    localId = Resource.NonFungibleResource.Item.ID.from(localId)
                )
            }

            TransferableAsset.NonFungible.NFTAssets(
                resource = Resource.NonFungibleResource(
                    resourceAddress = resourceAddress.addressString(),
                    amount = items.size.toLong(),
                    items = items,
                    metadata = metadataItems
                ),
                isNewlyCreated = true
            )
        }
    }
}

fun ResourceIndicator.toTransferableResource(resources: List<Resource>): TransferableAsset {
    val resourceAddress = this.resourceAddress
    return when (this) {
        is ResourceIndicator.Fungible -> {
            val resource = resources.findFungible(resourceAddress) ?: Resource.FungibleResource(
                resourceAddress = resourceAddress,
                ownedAmount = BigDecimal.ZERO
            )
            TransferableAsset.Fungible.Token(
                amount = amount,
                resource = resource,
                isNewlyCreated = false
            )
        }

        is ResourceIndicator.NonFungible -> {
            val items = indicator.nonFungibleLocalIds.map { id ->
                Resource.NonFungibleResource.Item(
                    collectionAddress = resourceAddress,
                    localId = Resource.NonFungibleResource.Item.ID.from(id)
                )
            }

            val collection = resources.findNonFungible(resourceAddress)?.copy(items = items) ?: Resource.NonFungibleResource(
                resourceAddress = resourceAddress,
                amount = items.size.toLong(),
                items = items
            )

            TransferableAsset.NonFungible.NFTAssets(
                resource = collection,
                isNewlyCreated = false
            )
        }
    }
}

private val ResourceIndicator.Fungible.amount: BigDecimal
    get() = when (val specificIndicator = indicator) {
        is FungibleResourceIndicator.Guaranteed -> specificIndicator.amount.asStr().toBigDecimal()
        is FungibleResourceIndicator.Predicted -> specificIndicator.predictedAmount.value.asStr().toBigDecimal()
    }

val NonFungibleResourceIndicator.nonFungibleLocalIds: List<NonFungibleLocalId>
    get() = when (this) {
        is NonFungibleResourceIndicator.ByAll -> predictedIds.value
        is NonFungibleResourceIndicator.ByAmount -> predictedIds.value
        is NonFungibleResourceIndicator.ByIds -> ids
    }

private fun Map<String, MetadataValue?>.toMetadata(): List<Metadata> = mapNotNull { it.toMetadata() }

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
    accountAddress: String,
    ownedAccounts: List<Network.Account>
): AccountWithTransferableResources {
    val ownedAccount = ownedAccounts.find { it.address == accountAddress }
    return if (ownedAccount != null) {
        AccountWithTransferableResources.Owned(ownedAccount, this)
    } else {
        AccountWithTransferableResources.Other(accountAddress, this)
    }
}

fun ExecutionSummary.toWithdrawingAccountsWithTransferableAssets(
    involvedAssets: List<Asset>,
    allOwnedAccounts: List<Network.Account>
) = accountWithdraws.map { withdrawEntry ->
    withdrawEntry.value.map { resource ->
        if (resource.isNewlyCreated(summary = this)) {
            resource.toNewlyCreatedTransferableAsset(resource.newlyCreatedMetadata(summary = this))
        } else {
            resource.toTransferableAsset(involvedAssets)
        }
    }.map {
        Transferable.Withdrawing(it)
    }.toAccountWithTransferableResources(
        withdrawEntry.key,
        allOwnedAccounts
    )
}.sortedWith(AccountWithTransferableResources.Companion.Sorter(allOwnedAccounts))

fun ExecutionSummary.toDepositingAccountsWithTransferableAssets(
    involvedAssets: List<Asset>,
    allOwnedAccounts: List<Network.Account>,
    defaultGuarantee: Double
) = accountDeposits.map { withdrawEntry ->
    withdrawEntry.value.map { resource ->
        val asset = if (resource.isNewlyCreated(summary = this)) {
            resource.toNewlyCreatedTransferableAsset(resource.newlyCreatedMetadata(summary = this))
        } else {
            resource.toTransferableAsset(involvedAssets)
        }

        Transferable.Depositing(
            transferable = asset,
            guaranteeType = resource.guaranteeType(defaultGuarantee)
        )
    }.toAccountWithTransferableResources(
        withdrawEntry.key,
        allOwnedAccounts
    )
}.sortedWith(AccountWithTransferableResources.Companion.Sorter(allOwnedAccounts))

@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.findFungible
import com.babylon.wallet.android.domain.model.resources.findNonFungible
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.domain.usecases.ResolveDAppInTransactionUseCase
import com.radixdlt.ret.Address
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.core.ret.asStr
import rdx.works.core.toHexString
import java.math.BigDecimal

val ResourceIndicator.resourceAddress: String
    get() = when (this) {
        is ResourceIndicator.Fungible -> resourceAddress.addressString()
        is ResourceIndicator.NonFungible -> resourceAddress.addressString()
    }

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

val DetailedManifestClass.AccountDepositSettingsUpdate.involvedResourceAddresses: Set<String>
    get() = authorizedDepositorsAdded.values.map { depositors ->
        depositors.map { it.resourceAddress }
    }.flatten().toSet() union authorizedDepositorsRemoved.values.map { depositors ->
        depositors.map { it.resourceAddress }
    }.flatten().toSet() union resourcePreferencesUpdates.values.map { updates ->
        updates.keys
    }.flatten().toSet()

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

fun ResourceIndicator.toDepositingTransferableResource(
    resources: List<Resource>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>,
    defaultDepositGuarantees: Double
): Transferable.Depositing {
    return when (this) {
        is ResourceIndicator.Fungible -> {
            Transferable.Depositing(
                transferable = toTransferableResource(
                    resources = resources,
                    newlyCreatedMetadata = newlyCreatedMetadata,
                    newlyCreatedEntities = newlyCreatedEntities
                ),
                guaranteeType = indicator.toGuaranteeType(defaultDepositGuarantees)
            )
        }

        is ResourceIndicator.NonFungible -> {
            Transferable.Depositing(
                transferable = toTransferableResource(
                    resources = resources,
                    newlyCreated = newlyCreatedMetadata,
                    newlyCreatedEntities = newlyCreatedEntities
                ),
                guaranteeType = indicator.toGuaranteeType(defaultDepositGuarantees)
            )
        }
    }
}

fun NonFungibleResourceIndicator.toGuaranteeType(defaultDepositGuarantees: Double): GuaranteeType {
    return when (this) {
        is NonFungibleResourceIndicator.ByAll -> GuaranteeType.Predicted(
            instructionIndex = predictedIds.instructionIndex.toLong(),
            guaranteeOffset = defaultDepositGuarantees
        )

        is NonFungibleResourceIndicator.ByAmount -> GuaranteeType.Predicted(
            instructionIndex = predictedIds.instructionIndex.toLong(),
            guaranteeOffset = defaultDepositGuarantees
        )

        is NonFungibleResourceIndicator.ByIds -> GuaranteeType.Guaranteed
    }
}

fun ResourceIndicator.toWithdrawingTransferableResource(
    resources: List<Resource>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>> = emptyMap(),
    newlyCreatedEntities: List<Address>
): Transferable.Withdrawing {
    return when (this) {
        is ResourceIndicator.Fungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(
                resources = resources,
                newlyCreatedMetadata = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities
            )
        )

        is ResourceIndicator.NonFungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(
                resources = resources,
                newlyCreated = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities,
            )
        )
    }
}

fun DetailedManifestClass.isConformingManifestType(): Boolean {
    return when (this) {
        is DetailedManifestClass.AccountDepositSettingsUpdate -> {
            true
        }

        DetailedManifestClass.General -> true
        is DetailedManifestClass.PoolContribution -> true
        is DetailedManifestClass.PoolRedemption -> true
        is DetailedManifestClass.Transfer -> true
        is DetailedManifestClass.ValidatorClaim -> true
        is DetailedManifestClass.ValidatorStake -> true
        is DetailedManifestClass.ValidatorUnstake -> true
        else -> false
    }
}

fun FungibleResourceIndicator.toGuaranteeType(defaultDepositGuarantees: Double): GuaranteeType {
    return when (this) {
        is FungibleResourceIndicator.Guaranteed -> GuaranteeType.Guaranteed
        is FungibleResourceIndicator.Predicted -> GuaranteeType.Predicted(
            instructionIndex = predictedAmount.instructionIndex.toLong(),
            guaranteeOffset = defaultDepositGuarantees
        )
    }
}

suspend fun ExecutionSummary.resolveDApps(
    resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase
) = coroutineScope {
    encounteredEntities.filter { it.isGlobalComponent() }
        .map { address ->
            async {
                resolveDAppInTransactionUseCase.invoke(address.addressString())
            }
        }
        .awaitAll()
        .mapNotNull { it.getOrNull() }
}

private val ResourceIndicator.Fungible.amount: BigDecimal
    get() = when (val specificIndicator = indicator) {
        is FungibleResourceIndicator.Guaranteed -> specificIndicator.amount.asStr().toBigDecimal()
        is FungibleResourceIndicator.Predicted -> specificIndicator.predictedAmount.value.asStr().toBigDecimal()
    }

private fun ResourceIndicator.Fungible.toTransferableResource(
    resources: List<Resource>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>
): TransferableAsset.Fungible.Token {
    val resource = resources.findFungible(
        resourceAddress.addressString()
    ) ?: Resource.FungibleResource.from(
        resourceAddress = resourceAddress, metadata = newlyCreatedMetadata[resourceAddress.addressString()].orEmpty()
    )

    return TransferableAsset.Fungible.Token(
        amount = amount,
        resource = resource,
        isNewlyCreated = resourceAddress.addressString() in newlyCreatedEntities.map { it.addressString() }
    )
}

private fun Resource.FungibleResource.Companion.from(
    resourceAddress: Address,
    metadata: Map<String, MetadataValue?>
): Resource.FungibleResource = Resource.FungibleResource(
    resourceAddress = resourceAddress.addressString(),
    ownedAmount = BigDecimal.ZERO,
    metadata = metadata.toMetadata()
)

private fun ResourceIndicator.NonFungible.toTransferableResource(
    resources: List<Resource>,
    newlyCreated: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>
): TransferableAsset.NonFungible.NFTAssets {
    val items = indicator.nonFungibleLocalIds.map { id ->
        Resource.NonFungibleResource.Item(
            collectionAddress = this.resourceAddress.addressString(),
            localId = Resource.NonFungibleResource.Item.ID.from(id)
        )
    }
    val collection = resources.findNonFungible(resourceAddress.addressString())?.copy(items = items) ?: Resource.NonFungibleResource.from(
        resourceAddress = resourceAddress,
        amount = items.size.toLong(),
        items = items,
        metadata = newlyCreated[resourceAddress.addressString()].orEmpty()
    )

    return TransferableAsset.NonFungible.NFTAssets(
        resource = collection,
        isNewlyCreated = resourceAddress.addressString() in newlyCreatedEntities.map { it.addressString() }
    )
}

private fun Resource.NonFungibleResource.Companion.from(
    resourceAddress: Address,
    amount: Long,
    items: List<Resource.NonFungibleResource.Item>,
    metadata: Map<String, MetadataValue?>
): Resource.NonFungibleResource = Resource.NonFungibleResource(
    resourceAddress = resourceAddress.addressString(),
    amount = amount,
    metadata = metadata.toMetadata(),
    items = items,
)

private val NonFungibleResourceIndicator.nonFungibleLocalIds: List<NonFungibleLocalId>
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

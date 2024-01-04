@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.findFungible
import com.babylon.wallet.android.domain.model.resources.findNonFungible
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.radixdlt.ret.Address
import com.radixdlt.ret.AuthorizedDepositorsChanges
import com.radixdlt.ret.DecimalSource
import com.radixdlt.ret.MetadataValue
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.NonFungibleLocalIdVecSource
import com.radixdlt.ret.PublicKey
import com.radixdlt.ret.PublicKeyHash
import com.radixdlt.ret.ResourceOrNonFungible
import com.radixdlt.ret.ResourceSpecifier
import com.radixdlt.ret.ResourceTracker
import com.radixdlt.ret.TransactionType
import rdx.works.core.ret.asStr
import rdx.works.core.toHexString
import java.math.BigDecimal

typealias RETResources = com.radixdlt.ret.Resources
typealias RETResourcesAmount = com.radixdlt.ret.Resources.Amount
typealias RETResourcesIds = com.radixdlt.ret.Resources.Ids

val ResourceTracker.resourceAddress: String
    get() = when (this) {
        is ResourceTracker.Fungible -> resourceAddress.addressString()
        is ResourceTracker.NonFungible -> resourceAddress.addressString()
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

val AuthorizedDepositorsChanges.resourceAddresses: Set<String>
    get() = added.map { it.resourceAddress }.toSet() union removed.map { it.resourceAddress }.toSet()

val TransactionType.involvedResourceAddresses: Set<String>
    get() = when (this) {
        is TransactionType.GeneralTransaction ->
            accountWithdraws
                .values
                .flatten()
                .map { it.resourceAddress }
                .toSet() union accountDeposits
                .values
                .flatten()
                .map { it.resourceAddress }
                .toSet()

        is TransactionType.SimpleTransfer -> setOf(transferred.resourceAddress)
        is TransactionType.Transfer ->
            transfers
                .map { it.value.keys }
                .flatten()
                .toSet()

        is TransactionType.AccountDepositSettings ->
            resourcePreferenceChanges
                .values
                .map { it.keys }
                .flatten()
                .toSet() union authorizedDepositorsChanges
                .map { it.value.resourceAddresses }
                .flatten()

        is TransactionType.ClaimStakeTransaction ->
            claims.map { claim ->
                claim.claimNftLocalIds.map { id ->
                    NonFungibleGlobalId.fromParts(claim.claimNftResource, id).resourceAddress().addressString()
                }
            }.flatten().toSet()

        is TransactionType.UnstakeTransaction ->
            unstakes.map {
                it.stakeUnitAddress.addressString()
            }.toSet() union unstakes.map {
                NonFungibleGlobalId.fromParts(it.claimNftResource, it.claimNftLocalId).resourceAddress().addressString()
            }.toSet()

        is TransactionType.StakeTransaction -> stakes.map { it.stakeUnitResource.addressString() }.toSet()
    }

val TransactionType.involvedValidatorAddresses: Set<String>
    get() = when (this) {
        is TransactionType.ClaimStakeTransaction ->
            claims.map { it.validatorAddress.addressString() }.toSet()

        is TransactionType.UnstakeTransaction ->
            unstakes.map { it.validatorAddress.addressString() }.toSet()

        is TransactionType.StakeTransaction -> stakes.map { it.validatorAddress.addressString() }.toSet()
        else -> emptySet()
    }

fun RETResources.toTransferableResource(resourceAddress: String, resources: List<Resource>): TransferableResource {
    return when (this) {
        is RETResourcesAmount -> {
            val resource = resources.findFungible(resourceAddress) ?: Resource.FungibleResource(
                resourceAddress = resourceAddress,
                ownedAmount = BigDecimal.ZERO
            )
            TransferableResource.Amount(
                amount = amount.asStr().toBigDecimal(),
                resource = resource,
                isNewlyCreated = false
            )
        }

        is RETResourcesIds -> {
            val items = ids.map { id ->
                Resource.NonFungibleResource.Item(
                    collectionAddress = resourceAddress,
                    localId = Resource.NonFungibleResource.Item.ID.from(id)
                )
            }

            val collection = resources.findNonFungible(resourceAddress)?.copy(items = items) ?: Resource.NonFungibleResource(
                resourceAddress = resourceAddress,
                amount = ids.size.toLong(),
                items = items
            )

            TransferableResource.NFTs(
                resource = collection,
                isNewlyCreated = false
            )
        }
    }
}

fun ResourceTracker.toDepositingTransferableResource(
    resources: List<Resource>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>,
    defaultDepositGuarantees: Double
): Transferable.Depositing {
    return when (this) {
        is ResourceTracker.Fungible -> Transferable.Depositing(
            transferable = toTransferableResource(
                resources = resources,
                newlyCreatedMetadata = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities
            ),
            guaranteeType = amount.toGuaranteeType(defaultDepositGuarantees)
        )

        is ResourceTracker.NonFungible -> {
            Transferable.Depositing(
                transferable = toTransferableResource(
                    resources = resources,
                    newlyCreated = newlyCreatedMetadata,
                    newlyCreatedEntities = newlyCreatedEntities
                ),
                guaranteeType = ids.toGuaranteeType(defaultDepositGuarantees)
            )
        }
    }
}

fun ResourceTracker.toWithdrawingTransferableResource(
    resources: List<Resource>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>> = emptyMap(),
    newlyCreatedEntities: List<Address>
): Transferable.Withdrawing {
    return when (this) {
        is ResourceTracker.Fungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(
                resources = resources,
                newlyCreatedMetadata = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities
            )
        )

        is ResourceTracker.NonFungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(
                resources = resources,
                newlyCreated = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities,
            )
        )
    }
}

private fun ResourceTracker.Fungible.toTransferableResource(
    resources: List<Resource>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>
): TransferableResource.Amount {
    val resource = resources.findFungible(
        resourceAddress.addressString()
    ) ?: Resource.FungibleResource.from(
        resourceAddress = resourceAddress,
        metadata = newlyCreatedMetadata[resourceAddress.addressString()].orEmpty()
    )

    return TransferableResource.Amount(
        amount = amount.valueDecimal,
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

private fun ResourceTracker.NonFungible.toTransferableResource(
    resources: List<Resource>,
    newlyCreated: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>
): TransferableResource.NFTs {
    val items = ids.valueList.map { id ->
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

    return TransferableResource.NFTs(
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

private val DecimalSource.valueDecimal: BigDecimal
    get() = when (this) {
        is DecimalSource.Guaranteed -> value.asStr().toBigDecimal()
        is DecimalSource.Predicted -> value.asStr().toBigDecimal()
    }

private fun DecimalSource.toGuaranteeType(defaultDepositGuarantees: Double): GuaranteeType = when (this) {
    is DecimalSource.Guaranteed -> GuaranteeType.Guaranteed
    is DecimalSource.Predicted -> GuaranteeType.Predicted(
        instructionIndex = instructionIndex.toLong(),
        guaranteeOffset = defaultDepositGuarantees
    )
}

private val NonFungibleLocalIdVecSource.valueList: List<NonFungibleLocalId>
    get() = when (this) {
        is NonFungibleLocalIdVecSource.Guaranteed -> value
        is NonFungibleLocalIdVecSource.Predicted -> value
    }

private fun NonFungibleLocalIdVecSource.toGuaranteeType(defaultDepositGuarantees: Double): GuaranteeType = when (this) {
    is NonFungibleLocalIdVecSource.Guaranteed -> GuaranteeType.Guaranteed
    is NonFungibleLocalIdVecSource.Predicted -> GuaranteeType.Predicted(
        instructionIndex = instructionIndex.toLong(),
        guaranteeOffset = defaultDepositGuarantees
    )
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

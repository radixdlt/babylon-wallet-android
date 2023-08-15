package rdx.works.core.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.ManifestBuilder
import com.radixdlt.ret.ManifestBuilderBucket
import com.radixdlt.ret.MetadataValue
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.PublicKeyHash
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.toByteArray

class BabylonManifestBuilder {
    private var latestBucketIndex: Int = 0
    private var manifestBuilder = ManifestBuilder()

    fun lockFee(): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.faucetLockFee()
        return this
    }

    fun freeXrd(): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.faucetFreeXrd()
        return this
    }

    fun accountTryDepositBatchOrAbort(
        toAddress: Address,
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.accountTryDepositBatchOrAbort(
            accountAddress = toAddress,
            authorizedDepositorBadge = null
        )
        return this
    }

    fun accountTryDepositOrAbort(
        toAddress: Address,
        fromBucket: ManifestBuilderBucket
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.accountTryDepositOrAbort(
            accountAddress = toAddress,
            authorizedDepositorBadge = null,
            bucket = fromBucket
        )
        return this
    }

    fun takeNonFungiblesFromWorktop(
        nonFungible: NonFungibleGlobalId,
        intoBucket: ManifestBuilderBucket
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.takeNonFungiblesFromWorktop(
            resourceAddress = nonFungible.resourceAddress(),
            ids = listOf(
                nonFungible.localId()
            ),
            intoBucket = intoBucket
        )
        return this
    }

    fun takeFromWorktop(
        fungible: Address,
        amount: Decimal,
        intoBucket: ManifestBuilderBucket
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.takeFromWorktop(
            resourceAddress = fungible,
            amount = amount,
            intoBucket = intoBucket
        )
        return this
    }

    fun withdrawFromAccount(
        fromAddress: Address,
        fungible: Address,
        amount: Decimal
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.withdrawFromAccount(
            accountAddress = fromAddress,
            resourceAddress = fungible,
            amount = amount
        )
        return this
    }

    fun withdrawNonFungiblesFromAccount(
        fromAddress: Address,
        nonFungible: NonFungibleGlobalId
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.withdrawNonFungiblesFromAccount(
            accountAddress = fromAddress,
            resourceAddress = nonFungible.resourceAddress(),
            ids = listOf(
                nonFungible.localId()
            )
        )
        return this
    }

    fun setOwnerKeys(
        address: Address,
        ownerKeyHashes: List<PublicKeyHash>
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.setMetadata(
            address = address,
            key = "owner_keys",
            value = MetadataValue.PublicKeyHashArrayValue(
                value = ownerKeyHashes
            )
        )
        return this
    }

    fun newBucket() = ManifestBuilderBucket(name = "bucket $latestBucketIndex").also {
        latestBucketIndex += 1
    }

    fun build(networkId: Int): TransactionManifest = manifestBuilder.build(networkId.toUByte())
}

fun NonFungibleLocalId.asStr() = when (this) {
    is NonFungibleLocalId.Bytes -> "[${value.toByteArray()}]"
    is NonFungibleLocalId.Integer -> "#$value#"
    is NonFungibleLocalId.Str -> "<$value>"
    is NonFungibleLocalId.Ruid -> "{$value}"
}

fun BabylonManifestBuilder.buildSafely(networkId: Int): Result<TransactionManifest> = runCatching { build(networkId) }

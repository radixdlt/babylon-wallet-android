package rdx.works.profile.ret

import com.radixdlt.ret.AccountDefaultDepositRule
import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.ManifestBuilder
import com.radixdlt.ret.ManifestBuilderAddress
import com.radixdlt.ret.ManifestBuilderBucket
import com.radixdlt.ret.ManifestBuilderValue
import com.radixdlt.ret.MetadataValue
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.PublicKeyHash
import com.radixdlt.ret.ResourcePreference
import com.radixdlt.ret.TransactionManifest
import java.math.BigDecimal
import java.math.RoundingMode

@Suppress("TooManyFunctions")
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

    fun accountTryDepositEntireWorktopOrAbort(
        toAddress: String,
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.accountTryDepositEntireWorktopOrAbort(
            accountAddress = Address(toAddress),
            authorizedDepositorBadge = null
        )
        return this
    }

    fun accountTryDepositOrAbort(
        toAddress: String,
        fromBucket: Bucket
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.accountTryDepositOrAbort(
            address = Address(toAddress),
            authorizedDepositorBadge = null,
            bucket = fromBucket.retBucket
        )
        return this
    }

    fun accountDeposit(
        toAddress: String,
        fromBucket: Bucket
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.accountDeposit(
            address = Address(toAddress),
            bucket = fromBucket.retBucket
        )
        return this
    }

    fun takeNonFungiblesFromWorktop(
        nonFungibleGlobalAddress: String,
        intoBucket: Bucket
    ): BabylonManifestBuilder {
        val globalAddress = NonFungibleGlobalId(nonFungibleGlobalAddress)

        manifestBuilder = manifestBuilder.takeNonFungiblesFromWorktop(
            resourceAddress = globalAddress.resourceAddress(),
            ids = listOf(globalAddress.localId()),
            intoBucket = intoBucket.retBucket
        )
        return this
    }

    fun takeFromWorktop(
        fungibleAddress: String,
        amount: BigDecimal,
        intoBucket: Bucket
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.takeFromWorktop(
            resourceAddress = Address(fungibleAddress),
            amount = amount.toRETDecimal(roundingMode = RoundingMode.HALF_UP),
            intoBucket = intoBucket.retBucket
        )
        return this
    }

    fun withdrawFromAccount(
        fromAddress: String,
        fungibleAddress: String,
        amount: BigDecimal
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.accountWithdraw(
            address = Address(fromAddress),
            resourceAddress = Address(fungibleAddress),
            amount = amount.toRETDecimal(roundingMode = RoundingMode.HALF_UP)
        )
        return this
    }

    fun withdrawNonFungiblesFromAccount(
        fromAddress: String,
        nonFungibleGlobalAddress: String
    ): BabylonManifestBuilder {
        val globalId = NonFungibleGlobalId(nonFungibleGlobalAddress)

        manifestBuilder = manifestBuilder.accountWithdrawNonFungibles(
            address = Address(fromAddress),
            resourceAddress = globalId.resourceAddress(),
            ids = listOf(globalId.localId())
        )
        return this
    }

    fun setOwnerKeys(
        address: String,
        ownerKeyHashes: List<PublicKeyHash>
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.metadataSet(
            address = Address(address),
            key = "owner_keys",
            value = MetadataValue.PublicKeyHashArrayValue(
                value = ownerKeyHashes
            )
        )
        return this
    }

    fun setDefaultDepositRule(
        accountAddress: String,
        accountDefaultDepositRule: AccountDefaultDepositRule
    ): BabylonManifestBuilder {
        val value = when (accountDefaultDepositRule) {
            AccountDefaultDepositRule.ACCEPT -> ManifestBuilderValue.EnumValue(0u, emptyList())
            AccountDefaultDepositRule.REJECT -> ManifestBuilderValue.EnumValue(1u, emptyList())
            AccountDefaultDepositRule.ALLOW_EXISTING -> ManifestBuilderValue.EnumValue(2u, emptyList())
        }
        manifestBuilder = manifestBuilder.callMethod(
            address = ManifestBuilderAddress.Static(Address(accountAddress)),
            methodName = "set_default_deposit_rule",
            args = listOf(value)
        )
        return this
    }

    fun addAuthorizedDepositor(
        accountAddress: String,
        depositorAddress: ManifestBuilderValue
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.callMethod(
            address = ManifestBuilderAddress.Static(Address(accountAddress)),
            methodName = "add_authorized_depositor",
            args = listOf(depositorAddress)
        )
        return this
    }

    fun removeAuthorizedDepositor(
        accountAddress: String,
        depositorAddress: ManifestBuilderValue
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.callMethod(
            address = ManifestBuilderAddress.Static(Address(accountAddress)),
            methodName = "remove_authorized_depositor",
            args = listOf(depositorAddress)
        )
        return this
    }

    fun setResourcePreference(
        accountAddress: String,
        resourceAddress: String,
        preference: ResourcePreference
    ): BabylonManifestBuilder {
        val value = when (preference) {
            ResourcePreference.ALLOWED -> ManifestBuilderValue.EnumValue(0u, emptyList())
            ResourcePreference.DISALLOWED -> ManifestBuilderValue.EnumValue(1u, emptyList())
        }
        manifestBuilder = manifestBuilder.callMethod(
            address = ManifestBuilderAddress.Static(Address(accountAddress)),
            methodName = "set_resource_preference",
            args = listOf(
                ManifestBuilderValue.AddressValue(ManifestBuilderAddress.Static(Address(resourceAddress))),
                value
            )
        )
        return this
    }

    fun removeResourcePreference(
        accountAddress: String,
        resourceAddress: String
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.callMethod(
            address = ManifestBuilderAddress.Static(Address(accountAddress)),
            methodName = "remove_resource_preference",
            args = listOf(
                ManifestBuilderValue.AddressValue(ManifestBuilderAddress.Static(Address(resourceAddress)))
            )
        )

        return this
    }

    fun newBucket() = Bucket("bucket $latestBucketIndex").also {
        latestBucketIndex += 1
    }

    fun build(networkId: Int): TransactionManifest = manifestBuilder.build(networkId.toUByte())

    class Bucket(name: String) {
        val retBucket: ManifestBuilderBucket
        init {
            retBucket = ManifestBuilderBucket(name = name)
        }
    }

    @Suppress("MagicNumber")
    private fun BigDecimal.toRETDecimal(roundingMode: RoundingMode): Decimal = Decimal(setScale(18, roundingMode).toPlainString())
}

fun NonFungibleLocalId.asStr() = when (this) {
    is NonFungibleLocalId.Bytes -> "[$value]"
    is NonFungibleLocalId.Integer -> "#$value#"
    is NonFungibleLocalId.Str -> "<$value>"
    is NonFungibleLocalId.Ruid -> "{$value}"
}

fun BabylonManifestBuilder.buildSafely(networkId: Int): Result<TransactionManifest> = runCatching { build(networkId) }

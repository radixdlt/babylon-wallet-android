package rdx.works.profile.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.ManifestBuilder
import com.radixdlt.ret.ManifestBuilderAddress
import com.radixdlt.ret.ManifestBuilderBucket
import com.radixdlt.ret.ManifestBuilderValue
import com.radixdlt.ret.MetadataValue
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.PublicKeyHash
import rdx.works.core.compressedPublicKeyHashBytes
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network.Account.OnLedgerSettings.ThirdPartyDeposits
import rdx.works.profile.ret.transaction.TransactionManifestData
import java.math.BigDecimal
import java.math.RoundingMode

@Suppress("TooManyFunctions")
internal class BabylonManifestBuilder {
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
        entityAddress: String,
        ownerPublicKeys: List<FactorInstance.PublicKey>
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.metadataSet(
            address = Address(entityAddress),
            key = "owner_keys",
            value = MetadataValue.PublicKeyHashArrayValue(
                value = ownerPublicKeys.map { key ->
                    val bytes = key.compressedData.compressedPublicKeyHashBytes()
                    when (key.curve) {
                        Slip10Curve.SECP_256K1 -> PublicKeyHash.Secp256k1(bytes)
                        Slip10Curve.CURVE_25519 -> PublicKeyHash.Secp256k1(bytes)
                    }
                }
            )
        )
        return this
    }

    fun setDefaultDepositRule(
        accountAddress: String,
        accountDefaultDepositRule: ThirdPartyDeposits.DepositRule
    ): BabylonManifestBuilder {
        val value = when (accountDefaultDepositRule) {
            ThirdPartyDeposits.DepositRule.AcceptAll -> ManifestBuilderValue.EnumValue(0u, emptyList())
            ThirdPartyDeposits.DepositRule.DenyAll -> ManifestBuilderValue.EnumValue(1u, emptyList())
            ThirdPartyDeposits.DepositRule.AcceptKnown -> ManifestBuilderValue.EnumValue(2u, emptyList())
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
        depositorAddress: ThirdPartyDeposits.DepositorAddress
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.callMethod(
            address = ManifestBuilderAddress.Static(Address(accountAddress)),
            methodName = "add_authorized_depositor",
            args = listOf(depositorAddress.toRETManifestBuilderValue())
        )
        return this
    }

    fun removeAuthorizedDepositor(
        accountAddress: String,
        depositorAddress: ThirdPartyDeposits.DepositorAddress
    ): BabylonManifestBuilder {
        manifestBuilder = manifestBuilder.callMethod(
            address = ManifestBuilderAddress.Static(Address(accountAddress)),
            methodName = "remove_authorized_depositor",
            args = listOf(depositorAddress.toRETManifestBuilderValue())
        )
        return this
    }

    fun setResourcePreference(
        accountAddress: String,
        resourceAddress: String,
        exceptionRule: ThirdPartyDeposits.DepositAddressExceptionRule
    ): BabylonManifestBuilder {
        val value = when (exceptionRule) {
            ThirdPartyDeposits.DepositAddressExceptionRule.Allow -> ManifestBuilderValue.EnumValue(0u, emptyList())
            ThirdPartyDeposits.DepositAddressExceptionRule.Deny -> ManifestBuilderValue.EnumValue(1u, emptyList())
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

    fun build(networkId: Int): TransactionManifestData = manifestBuilder.build(networkId.toUByte()).let {
        TransactionManifestData.from(manifest = it)
    }

    fun buildSafely(networkId: Int): Result<TransactionManifestData> = runCatching { build(networkId) }

    class Bucket(name: String) {
        val retBucket: ManifestBuilderBucket
        init {
            retBucket = ManifestBuilderBucket(name = name)
        }
    }
}

private fun ThirdPartyDeposits.DepositorAddress.toRETManifestBuilderValue(): ManifestBuilderValue {
    return when (this) {
        is ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID -> {
            val nonFungibleGlobalId = NonFungibleGlobalId(address)
            ManifestBuilderValue.EnumValue(
                0u,
                listOf(
                    ManifestBuilderValue.TupleValue(
                        listOf(
                            ManifestBuilderValue.AddressValue(ManifestBuilderAddress.Static(nonFungibleGlobalId.resourceAddress())),
                            ManifestBuilderValue.NonFungibleLocalIdValue(nonFungibleGlobalId.localId())
                        )
                    )
                )
            )
        }

        is ThirdPartyDeposits.DepositorAddress.ResourceAddress -> {
            val retAddress = Address(address)
            ManifestBuilderValue.EnumValue(
                1u,
                fields = listOf(ManifestBuilderValue.AddressValue(ManifestBuilderAddress.Static(retAddress)))
            )
        }
    }
}

internal fun ManifestBuilder.buildSafely(networkId: Int): Result<TransactionManifestData> = runCatching {
    TransactionManifestData.from(manifest = build(networkId.toUByte()))
}

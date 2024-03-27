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

@Suppress("TooManyFunctions")
internal class BabylonManifestBuilder {
    private var latestBucketIndex: Int = 0
    private var manifestBuilder = ManifestBuilder()

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

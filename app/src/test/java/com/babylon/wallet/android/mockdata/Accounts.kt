package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.NonFungibleResourceAddress
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.MAX_DIVISIBILITY
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Divisibility
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType

val mockResourceAddressXRD = ResourceAddress.sampleMainnet.xrd
val mockResourceAddress1 = ResourceAddress.sampleMainnet.random()
val mockResourceAddress2 = ResourceAddress.sampleMainnet.random()
val mockResourceAddress3 = ResourceAddress.sampleMainnet.random()
val mockResourceAddress4 = ResourceAddress.sampleMainnet.random()
val mockResourceAddress5 = ResourceAddress.sampleMainnet.random()
val mockSomeOtherResource = ResourceAddress.sampleMainnet.random()

val mockLSUAddress1 = ResourceAddress.sampleMainnet.random()
val mockLSUAddress2 = ResourceAddress.sampleMainnet.random()

val mockStakeFungibleResourceAddress1 = ResourceAddress.sampleMainnet.random()
val mockPoolAddress1 = PoolAddress.sampleMainnet()
val mockStakeFungibleResourceAddress2 = ResourceAddress.sampleMainnet.random()
val mockPoolAddress2 = PoolAddress.sampleMainnet.other()

val mockNFTAddressForStakeClaim1 = ResourceAddress.sampleMainnet.candy
val mockNFTAddressForStakeClaim2 = ResourceAddress.init(NonFungibleResourceAddress.sampleMainnet().string)

// Total fiat value of all accounts: 110481686.856 + 874160.26252 = 111355847.119
val mockAccountsWithMockAssets = listOf(
    AccountWithAssets( // Total fiat value of account0: 110415990.053 + 63769.6827967 + 1927.12059 = 110481686.856
        account = Account.sampleMainnet(),
        assets = Assets(
            tokens = listOf( // Total fiat value of tokens: 0.053064186 + 15990 + 110400000 + 0 = 110415990.053
                Token(
                    resource = Resource.FungibleResource(
                        address = mockResourceAddress1,
                        ownedAmount = 0.757.toDecimal192(), // Total fiat value: 0.757 * 0.070098 = 0.053064186
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token1", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        address = mockResourceAddress3,
                        ownedAmount = 1066.toDecimal192(), // Total fiat value: 1066 * 15 = 15990
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token3", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        address = mockResourceAddress4,
                        ownedAmount = 96.toDecimal192(), // Total fiat value: 96 * 1150000 = 110400000
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token4", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        address = mockSomeOtherResource,
                        ownedAmount = 10.toDecimal192(), // Total fiat value: 0 because not included in prices list
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "OtherToken", valueType = MetadataType.String)
                        )
                    )
                )
            ),
            poolUnits = listOf( // Total fiat value of pool units: 1821.55590147 + 61948.1268952 = 63769.6827967
                PoolUnit( // Total fiat value of pools unit: 20.4990492704 + 1801.0568522 = 1821.55590147
                    stake = Resource.FungibleResource(
                        address = mockStakeFungibleResourceAddress1,
                        ownedAmount = 31953.48992.toDecimal192(),
                        currentSupply = 5954986.901239.toDecimal192(),
                        divisibility= Divisibility(Decimal192.MAX_DIVISIBILITY),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "cool pool unit", valueType = MetadataType.String),
                            Metadata.Primitive(key = ExplicitMetadataKey.POOL.key, value = mockPoolAddress1.string, valueType = MetadataType.Address)
                        )
                    ),
                    pool = Pool(
                        address = mockPoolAddress1,
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.POOL_UNIT.key, value = mockStakeFungibleResourceAddress1.string, valueType = MetadataType.Address),
                            Metadata.Collection(
                                key = "pool_resources",
                                values = listOf(
                                    Metadata.Primitive("pool_resources", mockResourceAddress5.string, MetadataType.Address),
                                    Metadata.Primitive("pool_resources", mockResourceAddress1.string, MetadataType.Address),
                                )
                            ),
                        ),
                        resources = listOf(
                            Resource.FungibleResource(
                                address = mockResourceAddress5,
                                ownedAmount = 7640578.24036.toDecimal192(), // Total fiat value: ((31953.48992 * 7640578.24036) / 5954986.901239) * 0.0005 = 20.4990492704
                                metadata = listOf(
                                    Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token5", valueType = MetadataType.String)
                                )
                            ),
                            Resource.FungibleResource(
                                address = mockResourceAddress1,
                                ownedAmount = 4788332.57973.toDecimal192(), // Total fiat value: ((31953.48992 * 4788332.57973) / 5954986.901239) * 0.070098 = 1801.0568522
                                metadata = listOf(
                                    Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token1", valueType = MetadataType.String)
                                )
                            )
                        )
                    )
                ),
                PoolUnit( // Total fiat value of pool unit: 61948.1268952
                    stake = Resource.FungibleResource(
                        address = mockStakeFungibleResourceAddress2,
                        ownedAmount = 31953.48992.toDecimal192(),
                        currentSupply = 5954986.901239.toDecimal192(),
                        divisibility = Divisibility(Decimal192.MAX_DIVISIBILITY),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "cool pool unit", valueType = MetadataType.String),
                            Metadata.Primitive(key = ExplicitMetadataKey.POOL.key, value = mockPoolAddress2.string, valueType = MetadataType.Address)
                        )
                    ),
                    pool = Pool(
                        address = mockPoolAddress2,
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.POOL_UNIT.key, value = mockStakeFungibleResourceAddress2.string, valueType = MetadataType.Address),
                            Metadata.Collection(
                                key = "pool_resources",
                                values = listOf(
                                    Metadata.Primitive("pool_resources", mockResourceAddressXRD.string, MetadataType.Address),
                                )
                            ),
                        ),
                        resources = listOf(
                            Resource.FungibleResource(
                                address = mockResourceAddressXRD,
                                ownedAmount = 7640578.24036.toDecimal192(), // Total fiat value: ((31953.48992 * 7640578.24036) / 5954986.901239) * 1.511 = 61948.1268952
                                metadata = listOf(
                                    Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token5", valueType = MetadataType.String)
                                )
                            )
                        )
                    )
                )
            ),
            liquidStakeUnits = listOf( // Total fiat value of LSUs: 645.66459 + 1281.456 = 1927.12059
                LiquidStakeUnit(
                    fungibleResource = Resource.FungibleResource(
                        address = mockLSUAddress1,
                        ownedAmount = 1266.009.toDecimal192(), // Total fiat value: 1266.009 * 0.51 = 645.66459
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "TokenXRD", valueType = MetadataType.String)
                        )
                    ),
                    validator = Validator.sampleMainnet().copy(totalXrdStake = 99.toDecimal192())
                ),
                LiquidStakeUnit(
                    fungibleResource = Resource.FungibleResource(
                        address = mockLSUAddress2,
                        ownedAmount = 16.toDecimal192(), // Total fiat value: 16 * 80.091 = 1281.456
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "TokenXRD", valueType = MetadataType.String)
                        )
                    ),
                    validator = Validator.sampleMainnet.other().copy(totalXrdStake = 909.toDecimal192())
                )
            )
        )
    ),
    AccountWithAssets( // Total fiat value of account1: 870565.11 + 3595.15252 = 874160.26252
        account = Account.sampleMainnet.other(),
        assets = Assets(
            tokens = listOf( // Total fiat value of tokens: 870550 + 15.11 = 870565.11
                Token(
                    resource = Resource.FungibleResource(
                        address = mockResourceAddress4,
                        ownedAmount = 0.757.toDecimal192(), // Total fiat value: 0.757 * 1150000 = 870550
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token4", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        address = mockResourceAddressXRD,
                        ownedAmount = 10.toDecimal192(), // Total fiat value: 10 * 1.511 = 15.11
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "TokenXRD", valueType = MetadataType.String)
                        )
                    )
                )
            ),
            stakeClaims = listOf( // Total fiat value of stake claims: 2587.82926 + 1007.32326 = 3595.15252
                StakeClaim( // Total fiat value of stake claim 1: 1595.70666 + 992.1226 = 2587.82926
                    nonFungibleResource = Resource.NonFungibleResource(
                        address = mockNFTAddressForStakeClaim1,
                        amount = 2L, // number of items
                        displayAmount = 2L,
                        items = listOf(
                            Resource.NonFungibleResource.Item(
                                collectionAddress = mockNFTAddressForStakeClaim1,
                                localId = NonFungibleLocalId.sample(),
                                metadata = listOf(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.CLAIM_AMOUNT.key,
                                        value = "1056.06", // Total fiat value: 1056.06 * 1.511 = 1595.70666
                                        valueType = MetadataType.Decimal
                                    )
                                )
                            ),
                            Resource.NonFungibleResource.Item(
                                collectionAddress = mockNFTAddressForStakeClaim1,
                                localId = NonFungibleLocalId.sample.other(),
                                metadata = listOf(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.CLAIM_AMOUNT.key,
                                        value = "656.6", // Total fiat value: 656.6 * 1.511 = 992.1226
                                        valueType = MetadataType.Decimal
                                    )
                                )
                            )
                        ),
                        currentSupply = 66598,
                        metadata = listOf(
                            Metadata.Primitive(
                                key = ExplicitMetadataKey.VALIDATOR.key,
                                value = Validator.sampleMainnet().address.string,
                                valueType = MetadataType.Address
                            )
                        )
                    ),
                    validator = Validator.sampleMainnet().copy(
                        totalXrdStake = 27335.0901.toDecimal192(),
                        claimTokenResourceAddress = mockNFTAddressForStakeClaim1
                    )
                ),
                StakeClaim( // Total fiat value of stake claim 2: 1007.32326
                    nonFungibleResource = Resource.NonFungibleResource(
                        address = mockNFTAddressForStakeClaim2,
                        amount = 1L, // number of items
                        displayAmount = 1L,
                        items = listOf(
                            Resource.NonFungibleResource.Item(
                                collectionAddress = mockNFTAddressForStakeClaim2,
                                localId = NonFungibleLocalId.sample(),
                                metadata = listOf(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.CLAIM_AMOUNT.key,
                                        value = "666.66", // Total fiat value: 666.66 * 1.511 = 1007.32326
                                        valueType = MetadataType.Decimal
                                    )
                                )
                            )
                        ),
                        currentSupply = 66598,
                        metadata = listOf(
                            Metadata.Primitive(
                                key = ExplicitMetadataKey.VALIDATOR.key,
                                value = Validator.sampleMainnet.other().address.string,
                                valueType = MetadataType.Address)
                        )
                    ),
                    validator = Validator.sampleMainnet.other().copy(
                        totalXrdStake = 11075.toDecimal192(),
                        claimTokenResourceAddress = mockNFTAddressForStakeClaim2,
                    )
                )
            )
        )
    )
)
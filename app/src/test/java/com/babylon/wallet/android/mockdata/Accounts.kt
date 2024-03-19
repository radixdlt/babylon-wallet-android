package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.assets.ValidatorDetail
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId
import java.math.BigDecimal

fun account(
    name: String = "account-name",
    address: String = "address-$name",
    networkId: NetworkId = Radix.Gateway.default.network.networkId()
) = Network.Account(
    address = address,
    appearanceID = 1,
    displayName = name,
    networkID = networkId.value,
    securityState = SecurityState.Unsecured(
        unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
            transactionSigning = FactorInstance(
                badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                    derivationPath = DerivationPath.forAccount(
                        networkId = NetworkId.Mainnet,
                        accountIndex = 0,
                        keyType = KeyType.TRANSACTION_SIGNING
                    ),
                    publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                ),
                factorSourceId = FactorSource.FactorSourceID.FromHash(
                    kind = FactorSourceKind.DEVICE,
                    body = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5")
                )
            )
        )
    ),
    onLedgerSettings = Network.Account.OnLedgerSettings.init()
)

const val mockResourceAddressXRD = "resource_rdx1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxradxrd"
const val mockResourceAddress1 = "resourceAddress_1"
const val mockResourceAddress2 = "resourceAddress_2"
const val mockResourceAddress3 = "resourceAddress_3"
const val mockResourceAddress4 = "resourceAddress_4"
const val mockResourceAddress5 = "resourceAddress_5"
const val mockSomeOtherResource = "some_other_strange_resource"

const val mockLSUAddress1 = "mocKLSUAddress1"
const val mockLSUAddress2 = "mocKLSUAddress2"

const val mockValidatorAddress0 = "validatorAddress_0"
const val mockValidatorAddress1 = "validatorAddress_1"

const val mockStakeFungibleResourceAddress1 = "stakeFungibleResourceAddress1"
const val mockPoolAddress1 = "poolAddress1"
const val mockStakeFungibleResourceAddress2 = "stakeFungibleResourceAddress2"
const val mockPoolAddress2 = "poolAddress2"

const val mockNFTAddressForStakeClaim1 = "nftAddressForStakeClaim_1"
const val mockNFTAddressForStakeClaim2 = "nftAddressForStakeClaim_2"

// Total fiat value of all accounts: 110481686.856 + 874160.26252 = 111355847.119
val mockAccountsWithMockAssets = listOf(
    AccountWithAssets( // Total fiat value of account0: 110415990.053 + 63769.6827967 + 1927.12059 = 110481686.856
        account = Network.Account(
            address = "account_address_0",
            appearanceID = 0,
            displayName = "account0",
            networkID = 1,
            securityState = unsecuredSecurityState(),
            onLedgerSettings = Network.Account.OnLedgerSettings.init()
        ),
        assets = Assets(
            tokens = listOf( // Total fiat value of tokens: 0.053064186 + 15990 + 110400000 + 0 = 110415990.053
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = mockResourceAddress1,
                        ownedAmount = BigDecimal(0.757), // Total fiat value: 0.757 * 0.070098 = 0.053064186
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token1", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = mockResourceAddress3,
                        ownedAmount = BigDecimal(1066), // Total fiat value: 1066 * 15 = 15990
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token3", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = mockResourceAddress4,
                        ownedAmount = BigDecimal(96), // Total fiat value: 96 * 1150000 = 110400000
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token4", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = mockSomeOtherResource,
                        ownedAmount = BigDecimal(10), // Total fiat value: 0 because not included in prices list
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "OtherToken", valueType = MetadataType.String)
                        )
                    )
                )
            ),
            poolUnits = listOf( // Total fiat value of pool units: 1821.55590147 + 61948.1268952 = 63769.6827967
                PoolUnit( // Total fiat value of pools unit: 20.4990492704 + 1801.0568522 = 1821.55590147
                    stake = Resource.FungibleResource(
                        resourceAddress = mockStakeFungibleResourceAddress1,
                        ownedAmount = BigDecimal(31953.48992),
                        currentSupply = BigDecimal(5954986.901239),
                        divisibility=18,
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "cool pool unit", valueType = MetadataType.String),
                            Metadata.Primitive(key = ExplicitMetadataKey.POOL.key, value = mockPoolAddress1, valueType = MetadataType.Address)
                        )
                    ),
                    pool = Pool(
                        address = mockPoolAddress1,
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.POOL_UNIT.key, value = mockStakeFungibleResourceAddress1, valueType = MetadataType.Address),
                            Metadata.Collection(
                                key = "pool_resources",
                                values = listOf(
                                    Metadata.Primitive("pool_resources", mockResourceAddress5, MetadataType.Address),
                                    Metadata.Primitive("pool_resources", mockResourceAddress1, MetadataType.Address),
                                )
                            ),
                        ),
                        resources = listOf(
                            Resource.FungibleResource(
                                resourceAddress = mockResourceAddress5,
                                ownedAmount = BigDecimal(7640578.24036), // Total fiat value: ((31953.48992 * 7640578.24036) / 5954986.901239) * 0.0005 = 20.4990492704
                                metadata = listOf(
                                    Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token5", valueType = MetadataType.String)
                                )
                            ),
                            Resource.FungibleResource(
                                resourceAddress = mockResourceAddress1,
                                ownedAmount = BigDecimal(4788332.57973), // Total fiat value: ((31953.48992 * 4788332.57973) / 5954986.901239) * 0.070098 = 1801.0568522
                                metadata = listOf(
                                    Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token1", valueType = MetadataType.String)
                                )
                            )
                        )
                    )
                ),
                PoolUnit( // Total fiat value of pool unit: 61948.1268952
                    stake = Resource.FungibleResource(
                        resourceAddress = mockStakeFungibleResourceAddress2,
                        ownedAmount = BigDecimal(31953.48992),
                        currentSupply = BigDecimal(5954986.901239),
                        divisibility=18,
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "cool pool unit", valueType = MetadataType.String),
                            Metadata.Primitive(key = ExplicitMetadataKey.POOL.key, value = mockPoolAddress2, valueType = MetadataType.Address)
                        )
                    ),
                    pool = Pool(
                        address = mockPoolAddress2,
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.POOL_UNIT.key, value = mockStakeFungibleResourceAddress2, valueType = MetadataType.Address),
                            Metadata.Collection(
                                key = "pool_resources",
                                values = listOf(
                                    Metadata.Primitive("pool_resources", mockResourceAddressXRD, MetadataType.Address),
                                )
                            ),
                        ),
                        resources = listOf(
                            Resource.FungibleResource(
                                resourceAddress = mockResourceAddressXRD,
                                ownedAmount = BigDecimal(7640578.24036), // Total fiat value: ((31953.48992 * 7640578.24036) / 5954986.901239) * 1.511 = 61948.1268952
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
                        resourceAddress = mockLSUAddress1,
                        ownedAmount = BigDecimal(1266.009), // Total fiat value: 1266.009 * 0.51 = 645.66459
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "TokenXRD", valueType = MetadataType.String)
                        )
                    ),
                    validator = ValidatorDetail(
                        address = mockValidatorAddress0,
                        totalXrdStake = BigDecimal(99),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Validator0", valueType = MetadataType.String)
                        )
                    )
                ),
                LiquidStakeUnit(
                    fungibleResource = Resource.FungibleResource(
                        resourceAddress = mockLSUAddress2,
                        ownedAmount = BigDecimal(16), // Total fiat value: 16 * 80.091 = 1281.456
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "TokenXRD", valueType = MetadataType.String)
                        )
                    ),
                    validator = ValidatorDetail(
                        address = mockValidatorAddress1,
                        totalXrdStake = BigDecimal(909),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Validator1", valueType = MetadataType.String)
                        )
                    )
                )
            )
        )
    ),
    AccountWithAssets( // Total fiat value of account1: 870565.11 + 3595.15252 = 874160.26252
        account = Network.Account(
            address = "account_address_1",
            appearanceID = 1,
            displayName = "account1",
            networkID = 1,
            securityState = unsecuredSecurityState(),
            onLedgerSettings = Network.Account.OnLedgerSettings.init()
        ),
        assets = Assets(
            tokens = listOf( // Total fiat value of tokens: 870550 + 15.11 = 870565.11
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = mockResourceAddress4,
                        ownedAmount = BigDecimal(0.757), // Total fiat value: 0.757 * 1150000 = 870550
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token4", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = mockResourceAddressXRD,
                        ownedAmount = BigDecimal(10), // Total fiat value: 10 * 1.511 = 15.11
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "TokenXRD", valueType = MetadataType.String)
                        )
                    )
                )
            ),
            stakeClaims = listOf( // Total fiat value of stake claims: 2587.82926 + 1007.32326 = 3595.15252
                StakeClaim( // Total fiat value of stake claim 1: 1595.70666 + 992.1226 = 2587.82926
                    nonFungibleResource = Resource.NonFungibleResource(
                        resourceAddress = mockNFTAddressForStakeClaim1,
                        amount = 2L, // number of items
                        items = listOf(
                            Resource.NonFungibleResource.Item(
                                collectionAddress = mockNFTAddressForStakeClaim1,
                                localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
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
                                localId = Resource.NonFungibleResource.Item.ID.from("#2#"),
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
                            Metadata.Primitive(key = ExplicitMetadataKey.VALIDATOR.key, value = "validator_address1", valueType = MetadataType.Address)
                        )
                    ),
                    validator = ValidatorDetail(
                        address = "validator_address1",
                        totalXrdStake = BigDecimal(27335.0901),
                        stakeUnitResourceAddress = "stakeUnitResourceAddress1",
                        claimTokenResourceAddress = mockNFTAddressForStakeClaim1
                    )
                ),
                StakeClaim( // Total fiat value of stake claim 2: 1007.32326
                    nonFungibleResource = Resource.NonFungibleResource(
                        resourceAddress = mockNFTAddressForStakeClaim2,
                        amount = 1L, // number of items
                        items = listOf(
                            Resource.NonFungibleResource.Item(
                                collectionAddress = mockNFTAddressForStakeClaim2,
                                localId = Resource.NonFungibleResource.Item.ID.Companion.from("#1#"),
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
                            Metadata.Primitive(key = ExplicitMetadataKey.VALIDATOR.key, value = "validator_address2", valueType = MetadataType.Address)
                        )
                    ),
                    validator = ValidatorDetail(
                        address = "validator_address2",
                        totalXrdStake = BigDecimal(11075),
                        stakeUnitResourceAddress = "stakeUnitResourceAddress2",
                        claimTokenResourceAddress = mockNFTAddressForStakeClaim2,
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.CLAIM_NFT.key, value = mockNFTAddressForStakeClaim2, valueType = MetadataType.Address)
                        )
                    )
                )
            )
        )
    )
)

fun unsecuredSecurityState(): SecurityState.Unsecured {
    return SecurityState.Unsecured(
        unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
            transactionSigning = FactorInstance(
                badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                    derivationPath = DerivationPath.forAccount(
                        networkId = NetworkId.Mainnet,
                        accountIndex = 0,
                        keyType = KeyType.TRANSACTION_SIGNING
                    ),
                    publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                ),
                factorSourceId = FactorSource.FactorSourceID.FromHash(
                    kind = FactorSourceKind.DEVICE,
                    body = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5")
                )
            )
        )
    )
}
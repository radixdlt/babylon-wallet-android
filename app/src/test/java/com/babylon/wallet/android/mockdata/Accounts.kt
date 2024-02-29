package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import rdx.works.core.HexCoded32Bytes
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

val mockAccountsWithMockAssets = listOf(
    AccountWithAssets(
        account = Network.Account(
            address = "account_address_0",
            appearanceID = 0,
            displayName = "account0",
            networkID = 0,
            securityState = unsecuredSecurityState(),
            onLedgerSettings = Network.Account.OnLedgerSettings.init()
        ),
        assets = Assets(
            tokens = listOf(
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = "resourceAddress_1",
                        ownedAmount = BigDecimal(0.757),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token1", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = "resourceAddress_3",
                        ownedAmount = BigDecimal(1066),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token3", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = "some_other_strange_resource",
                        ownedAmount = BigDecimal(10),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "OtherToken", valueType = MetadataType.String)
                        )
                    )
                )
            ),
            liquidStakeUnits = listOf(
                LiquidStakeUnit(
                    fungibleResource = Resource.FungibleResource(
                        resourceAddress = "resourceAddress_3",
                        ownedAmount = BigDecimal(1066),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token3", valueType = MetadataType.String)
                        )
                    ),
                    validator = ValidatorDetail(
                        address = "validator_address_0",
                        totalXrdStake = BigDecimal(99),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Validator0", valueType = MetadataType.String)
                        )
                    )
                )
            )
        )
    ),
    AccountWithAssets(
        account = Network.Account(
            address = "account_address_1",
            appearanceID = 1,
            displayName = "account1",
            networkID = 0,
            securityState = unsecuredSecurityState(),
            onLedgerSettings = Network.Account.OnLedgerSettings.init()
        ),
        assets = Assets(
            tokens = listOf(
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = "resourceAddress_4",
                        ownedAmount = BigDecimal(0.757),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token4", valueType = MetadataType.String)
                        )
                    )
                ),
                Token(
                    resource = Resource.FungibleResource(
                        resourceAddress = "resourceAddress_0",
                        ownedAmount = BigDecimal(10),
                        metadata = listOf(
                            Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = "Token0", valueType = MetadataType.String)
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
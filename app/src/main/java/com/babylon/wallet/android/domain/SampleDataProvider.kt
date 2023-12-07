@file:Suppress("MagicNumber", "TooManyFunctions")

package com.babylon.wallet.android.domain

import com.babylon.wallet.android.data.transaction.TransactionVersion
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AssetType
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.InstantGenerator
import rdx.works.core.emptyIdentifiedArrayList
import rdx.works.core.identifiedArrayListOf
import rdx.works.profile.data.model.Header
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.apppreferences.Transaction
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.IdentifiedEntry
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.TestData
import java.math.BigDecimal

class SampleDataProvider {

    val p2pLinksSample = listOf(
        P2PLink(
            displayName = "chrome connection",
            connectionPassword = "chrome_pass"
        ),
        P2PLink(
            displayName = "firefox connection",
            connectionPassword = "firefox_pass"
        )
    )

    val ledgerFactorSourcesSample = listOf(
        LedgerHardwareWalletFactorSource.newSource(
            deviceID = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5"),
            model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
            name = "Nano S",
        ),
        LedgerHardwareWalletFactorSource.newSource(
            deviceID = HexCoded32Bytes("5f07ec336e9e7891bff00404c817201e73c097b6b1e1b3a26bc205e0010196f5"),
            model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_X,
            name = "Nano X",
        )
    )

    val transactionRequest = MessageFromDataChannel.IncomingRequest.TransactionRequest(
        remoteConnectorId = "b49d643908be5b79b1d233c0b21c1c9dd31a8376ab7caee242af42f6ff1c3bcc",
        requestId = "7294770e-5aec-4e49-ada0-e6a2213fc8c8",
        transactionManifestData = TransactionManifestData(
            instructions = "CREATE_FUNGIBLE_RESOURCE_WITH_INITIAL_SUPPLY",
            version = TransactionVersion.Default.value,
            networkId = Radix.Gateway.default.network.id,
            message = "Hello"
        ),
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(Radix.Gateway.default.network.id)
    )

    val transferableDepositing = Transferable.Depositing(
        transferable = TransferableResource.Amount(
            amount = BigDecimal(69),
            resource = Resource.FungibleResource(
                resourceAddress = "resource_tdx_e_1tkawacgvcw7z9xztccgjrged25c7nqtnd4nllh750s2ny64m0cltmg",
                ownedAmount = null,
                nameMetadataItem = NameMetadataItem(name = "XXX"),
                symbolMetadataItem = SymbolMetadataItem(symbol = "XXX"),
                descriptionMetadataItem = DescriptionMetadataItem(description = "a very xxx token"),
                tagsMetadataItem = null,
                currentSupply = BigDecimal("69696969696969.666999666999666999")
            ),
            isNewlyCreated = true
        )
    )

    val accountWithTransferableResourcesOwned = AccountWithTransferableResources.Owned(
        account = Network.Account(
            address = "account_tdx_e_12yeuyl924ml5v9qks4s3cegpm6gl355r96cd9d5z99qtlxvwq7y3sz",
            appearanceID = 0,
            displayName = "666",
            networkID = 14,
            securityState = SecurityState.Unsecured(
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    entityIndex = 0,
                    transactionSigning = FactorInstance(
                        badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                            derivationPath = DerivationPath.forAccount(
                                networkId = Radix.Gateway.default.network.networkId(),
                                accountIndex = 0,
                                keyType = KeyType.TRANSACTION_SIGNING
                            ),
                            publicKey = FactorInstance.PublicKey.curve25519PublicKey(
                                "c294ecdd8752e2197ad0027fe4557d464362df12b587537234f0b106237462f5"
                            )
                        ),
                        factorSourceId = FactorSource.FactorSourceID.FromHash(
                            kind = FactorSourceKind.DEVICE,
                            body = HexCoded32Bytes("ba6a7bd3e91b2a83e21f05c22eaddecd12e75ab01c492e9d4e62d6445600c142")
                        )
                    )
                )
            ),
            onLedgerSettings = Network.Account.OnLedgerSettings(
                thirdPartyDeposits = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
                    depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll,
                    assetsExceptionList = emptyList(),
                    depositorsAllowList = emptyList()
                )
            )
        ),
        resources = listOf(transferableDepositing)
    )

    fun babylonDeviceFactorSource() = DeviceFactorSource(
        id = FactorSource.FactorSourceID.FromHash(
            kind = FactorSourceKind.DEVICE,
            body = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
        ),
        common = FactorSource.Common(
            cryptoParameters = FactorSource.Common.CryptoParameters.babylon,
            addedOn = InstantGenerator(),
            lastUsedOn = InstantGenerator(),
            flags = listOf()
        ),
        hint = DeviceFactorSource.Hint(
            model = "Model",
            name = "Name",
            mnemonicWordCount = 24
        )
    )

    fun olympiaDeviceFactorSource() = DeviceFactorSource(
        id = FactorSource.FactorSourceID.FromHash(
            kind = FactorSourceKind.DEVICE,
            body = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
        ),
        common = FactorSource.Common(
            cryptoParameters = FactorSource.Common.CryptoParameters.olympiaBackwardsCompatible,
            addedOn = InstantGenerator(),
            lastUsedOn = InstantGenerator(),
            flags = listOf()
        ),
        hint = DeviceFactorSource.Hint(
            model = "Model",
            name = "Name",
            mnemonicWordCount = 12
        )
    )

    fun sampleAccount(
        address: String = "fj3489fj348f",
        name: String = "my account",
        factorSourceId: FactorSource.FactorSourceID.FromHash = FactorSource.FactorSourceID.FromHash(
            kind = FactorSourceKind.DEVICE,
            body = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
        ),
        appearanceId: Int = 0
    ): Network.Account {
        return Network.Account(
            address = address,
            appearanceID = appearanceId,
            displayName = name,
            networkID = Radix.Gateway.default.network.id,
            securityState = SecurityState.Unsecured(
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    entityIndex = 0,
                    transactionSigning = FactorInstance(
                        badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                            derivationPath = DerivationPath.forAccount(
                                networkId = Radix.Gateway.default.network.networkId(),
                                accountIndex = 0,
                                keyType = KeyType.TRANSACTION_SIGNING
                            ),
                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                        ),
                        factorSourceId = factorSourceId
                    )
                )
            ),
            onLedgerSettings = Network.Account.OnLedgerSettings.init()
        )
    }

    fun randomAddress(): String {
        val characters = "abcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder()
        repeat((0 until 26).count()) {
            sb.append(characters.random())
        }
        return sb.toString()
    }

    fun samplePersona(personaAddress: String = "1", personaName: String = "Test Persona"): Network.Persona {
        return Network.Persona(
            address = personaAddress,
            displayName = personaName,
            networkID = NetworkId.Nebunet.value,
            securityState = SecurityState.Unsecured(
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    entityIndex = 0,
                    transactionSigning = FactorInstance(
                        badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                            derivationPath = DerivationPath.forIdentity(
                                networkId = NetworkId.Nebunet,
                                identityIndex = 0,
                                keyType = KeyType.TRANSACTION_SIGNING
                            ),
                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                        ),
                        factorSourceId = FactorSource.FactorSourceID.FromHash(
                            kind = FactorSourceKind.DEVICE,
                            body = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
                        ),
                    )
                )
            ),
            personaData = PersonaData(
                name = IdentifiedEntry.Companion.init(
                    PersonaData.PersonaDataField.Name(
                        variant = PersonaData.PersonaDataField.Name.Variant.Western,
                        given = "John",
                        family = "",
                        nickname = ""
                    ),
                    "1"
                ),
                emailAddresses = listOf(IdentifiedEntry.init(PersonaData.PersonaDataField.Email("test@test.pl"), "2"))
            )
        )
    }

    fun sampleAccountWithResources(
        address: String = randomAddress(),
        withFungibleTokens: List<Resource.FungibleResource> = sampleFungibleResources()
    ): AccountWithAssets {
        return AccountWithAssets(
            account = sampleAccount(address = address),
            assets = Assets(
                fungibles = withFungibleTokens,
                nonFungibles = emptyList(),
                poolUnits = emptyList(),
                validatorsWithStakes = emptyList()
            )
        )
    }

    fun sampleProfile(
        mnemonicWithPassphrase: MnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
            bip39Passphrase = ""
        )
    ): Profile {
        val network = network(Radix.Gateway.default.network.id)
        val networkId = checkNotNull(network.knownNetworkId)
        val profile = Profile(
            header = Header.init(
                id = "9958f568-8c9b-476a-beeb-017d1f843266",
                deviceInfo = TestData.deviceInfo,
                creationDate = InstantGenerator(),
                numberOfNetworks = 0
            ),
            appPreferences = AppPreferences(
                transaction = Transaction.default,
                display = Display.default,
                security = Security.default,
                gateways = Gateways(Radix.Gateway.default.url, listOf(Radix.Gateway.default)),
                p2pLinks = emptyList()
            ),
            factorSources = identifiedArrayListOf(
                DeviceFactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase)
            ),
            networks = emptyList()
        )
        val firstAccount = Network.Account.initAccountWithDeviceFactorSource(
            entityIndex = 0,
            displayName = "first account",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = (profile.factorSources.first() as DeviceFactorSource),
            networkId = networkId,
            appearanceID = 0
        )
        return profile.copy(networks = listOf(network.copy(accounts = identifiedArrayListOf(firstAccount))))
    }

    fun network(networkId: Int): Network {
        return Network(networkId, emptyIdentifiedArrayList(), emptyIdentifiedArrayList(), emptyList())
    }

    fun sampleFungibleResources(
        amount: Pair<BigDecimal, String> = BigDecimal.valueOf(100000) to XrdResource.SYMBOL
    ): List<Resource.FungibleResource> {
        val result = mutableListOf<Resource.FungibleResource>()
        return result.apply {
            repeat(3) {
                add(
                    Resource.FungibleResource(
                        resourceAddress = randomAddress(),
                        ownedAmount = amount.first,
                        nameMetadataItem = NameMetadataItem(name = "XXX"),
                        symbolMetadataItem = SymbolMetadataItem(symbol = "XXX"),
                        descriptionMetadataItem = DescriptionMetadataItem(description = "a very xxx token"),
                        tagsMetadataItem = null,
                        currentSupply = BigDecimal("69696969696969.666999666999666999")
                    )
                )
            }
        }
    }

    fun nonFungibleResource(name: String): Resource.NonFungibleResource {
        return Resource.NonFungibleResource(
            resourceAddress = randomAddress(),
            amount = 1,
            nameMetadataItem = NameMetadataItem(name),
            descriptionMetadataItem = null,
            iconMetadataItem = null,
            items = emptyList()
        )
    }

    fun samplePoolUnit(): PoolUnit {
        val poolUnit = sampleFungibleResources().first()
        return PoolUnit(poolUnit, Pool(address = "pool_tdx_abc", poolUnitAddress = poolUnit.resourceAddress, sampleFungibleResources()))
    }

    fun sampleLSUUnit(): LiquidStakeUnit {
        return LiquidStakeUnit(
            sampleFungibleResources().first()
        )
    }

    fun sampleDAppWithResources(): DAppWithResources {
        return DAppWithResources(
            dApp = DApp(
                dAppAddress = "account_tdx_b_1qdcgrj7mz09cz3htn0y7qtcze7tq59s76p2h98puqtpst7jh4u"
            ),
            resources = DAppResources(
                fungibleResources = emptyList(),
                nonFungibleResources = emptyList()
            )
        )
    }

    fun sampleAssetException(): AssetType.AssetException {
        return AssetType.AssetException(
            assetException = Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException(
                address = randomAddress(),
                exceptionRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Allow
            )
        )
    }

    fun sampleDepositorResourceAddress(): AssetType.Depositor {
        return AssetType.Depositor(
            depositorAddress = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress.ResourceAddress(
                randomAddress()
            )
        )
    }

    fun sampleDepositorNftAddress(): AssetType.Depositor {
        return AssetType.Depositor(
            depositorAddress = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID(randomAddress())
        )
    }
}

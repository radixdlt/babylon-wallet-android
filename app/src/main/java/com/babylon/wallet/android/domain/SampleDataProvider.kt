@file:Suppress("MagicNumber")

package com.babylon.wallet.android.domain

import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.ValidatorsWithStakeResources
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import rdx.works.core.InstantGenerator
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
            deviceID = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5"),
            model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
            name = "Nano S",
        ),
        LedgerHardwareWalletFactorSource.newSource(
            deviceID = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff00404c817201e73c097b6b1e1b3a26bc205e0010196f5"),
            model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_X,
            name = "Nano X",
        )
    )

    fun babylonDeviceFactorSource() = DeviceFactorSource(
        id = FactorSource.FactorSourceID.FromHash(
            kind = FactorSourceKind.DEVICE,
            body = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
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
            body = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
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
            body = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
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
                            body = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
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
    ): AccountWithResources {
        return AccountWithResources(
            account = sampleAccount(address = address),
            resources = Resources(
                fungibleResources = withFungibleTokens,
                nonFungibleResources = emptyList(),
                poolUnits = emptyList(),
                validatorsWithStakeResources = ValidatorsWithStakeResources()
            )
        )
    }

    fun sampleProfile(
        mnemonicWithPassphrase: MnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
            bip39Passphrase = ""
        ),
        sampleNetwork: (networkId: Int) -> Network? = { null }
    ): Profile {
        return Profile(
            header = Header.init(
                id = "9958f568-8c9b-476a-beeb-017d1f843266",
                deviceName = "Galaxy A53 5G (Samsung SM-A536B)",
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
            factorSources = listOf(
                DeviceFactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase)
            ),
            networks = sampleNetwork(Radix.Gateway.default.network.id)?.let { listOf(it) } ?: emptyList()
        )
    }

    fun sampleFungibleResources(
        amount: Pair<BigDecimal, String> = BigDecimal.valueOf(100000) to "XRD"
    ): List<Resource.FungibleResource> {
        val result = mutableListOf<Resource.FungibleResource>()
        return result.apply {
            repeat(3) {
                add(
                    Resource.FungibleResource(
                        resourceAddress = randomAddress(),
                        amount = amount.first,
                        nameMetadataItem = NameMetadataItem("cool XRD"),
                        symbolMetadataItem = SymbolMetadataItem("XRD")
                    )
                )
            }
        }
    }

    fun samplePoolUnit(): Resource.PoolUnitResource {
        return Resource.PoolUnitResource(sampleFungibleResources().first(), sampleFungibleResources())
    }

    fun sampleLSUUnit(): Resource.LiquidStakeUnitResource {
        return Resource.LiquidStakeUnitResource(
            sampleFungibleResources().first()
        )
    }

    fun sampleDAppWithResources(): DAppWithMetadataAndAssociatedResources {
        return DAppWithMetadataAndAssociatedResources(
            dAppWithMetadata = DAppWithMetadata(
                dAppAddress = "account_tdx_b_1qdcgrj7mz09cz3htn0y7qtcze7tq59s76p2h98puqtpst7jh4u"
            ),
            resources = DAppResources(
                fungibleResources = emptyList(),
                nonFungibleResources = emptyList()
            )
        )
    }
}

@file:Suppress("MagicNumber")

package com.babylon.wallet.android.domain

import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.ManifestAstValue
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.Header
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId
import java.math.BigDecimal

class SampleDataProvider {

    fun sampleAccount(
        address: String = "fj3489fj348f",
        name: String = "my account",
        factorSourceId: FactorSource.FactorSourceID.FromHash = FactorSource.FactorSourceID.FromHash(
            kind = FactorSourceKind.DEVICE,
            body = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
        )
    ): Network.Account {
        return Network.Account(
            address = address,
            appearanceID = 123,
            displayName = name,
            networkID = Radix.Gateway.default.network.id,
            securityState = SecurityState.Unsecured(
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    transactionSigning = FactorInstance(
                        derivationPath = DerivationPath.forAccount(
                            networkId = Radix.Gateway.default.network.networkId(),
                            accountIndex = 0,
                            keyType = KeyType.TRANSACTION_SIGNING
                        ),
                        factorSourceId = factorSourceId,
                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                    )
                )
            )
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
            fields = listOf(
                Network.Persona.Field(Network.Persona.Field.ID.EmailAddress, "test@test.pl"),
                Network.Persona.Field(Network.Persona.Field.ID.GivenName, "John")
            ),
            securityState = SecurityState.Unsecured(
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    transactionSigning = FactorInstance(
                        derivationPath = DerivationPath.forIdentity(
                            networkId = NetworkId.Nebunet,
                            identityIndex = 0,
                            keyType = KeyType.TRANSACTION_SIGNING
                        ),
                        factorSourceId = FactorSource.FactorSourceID.FromHash(
                            kind = FactorSourceKind.DEVICE,
                            body = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5")
                        ),
                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                    )
                )
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
                nonFungibleResources = emptyList()
            )
        )
    }

    fun sampleProfile(
        mnemonicWithPassphrase: MnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
            bip39Passphrase = ""
        )
    ): Profile {
        return Profile(
            header = Header.init(
                id = "9958f568-8c9b-476a-beeb-017d1f843266",
                deviceName = "Galaxy A53 5G (Samsung SM-A536B)",
                creationDate = InstantGenerator(),
                numberOfNetworks = 0
            ),
            appPreferences = AppPreferences(
                display = Display.default,
                security = Security.default,
                gateways = Gateways(Radix.Gateway.default.url, listOf(Radix.Gateway.default)),
                p2pLinks = emptyList()
            ),
            factorSources = listOf(
                DeviceFactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase)
            ),
            networks = emptyList()
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

    fun sampleManifest(): TransactionManifest {
        return ManifestBuilder()
            .callMethod(
                ManifestAstValue.Address("component_tdx_b_1qftacppvmr9ezmekxqpq58en0nk954x0a7jv2zz0hc7qdxyth4"),
                "free",
            )
            .callMethod(
                ManifestAstValue.Address("account_tdx_b_1qdcgrj7mz09cz3htn0y7qtcze7tq59s76p2h98puqtpst7jh4u"),
                "deposit_batch",
                ManifestAstValue.Expression("ENTIRE_WORKTOP")
            )
            .build()
    }
}

@file:Suppress("MagicNumber")

package com.babylon.wallet.android.domain

import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleMetadataContainer
import com.babylon.wallet.android.domain.model.NonFungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import com.babylon.wallet.android.domain.model.SimpleOwnedFungibleToken
import com.babylon.wallet.android.presentation.model.toTokenUiModel
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.ManifestAstValue
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import rdx.works.profile.data.model.Header
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import java.math.BigDecimal
import java.time.Instant

class SampleDataProvider {

    fun sampleAccount(address: String = "fj3489fj348f"): Network.Account {
        return Network.Account(
            address = address,
            appearanceID = 123,
            displayName = "my account",
            networkID = 999,
            securityState = SecurityState.Unsecured(
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    genesisFactorInstance = FactorInstance(
                        derivationPath = DerivationPath.forAccount("m/1'/1'/1'/1'/1'/1'"),
                        factorSourceId = FactorSource.ID("IDIDDIIDD"),
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
            networkID = 11,
            fields = listOf(
                Network.Persona.Field("1", Network.Persona.Field.Kind.EmailAddress, "test@test.pl"),
                Network.Persona.Field("2", Network.Persona.Field.Kind.GivenName, "John")
            ),
            securityState = SecurityState.Unsecured(
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    genesisFactorInstance = FactorInstance(
                        derivationPath = DerivationPath.forIdentity("few"),
                        factorSourceId = FactorSource.ID("IDIDDIIDD"),
                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                    )
                )
            )
        )
    }

    fun sampleAccountResource(
        address: String = randomAddress(),
        withFungibleTokens: List<OwnedFungibleToken> = sampleFungibleTokens(address)
    ): AccountResources {
        return AccountResources(
            address = address,
            displayName = "My account",
            fungibleTokens = withFungibleTokens,
            appearanceID = 1
        )
    }

    fun sampleProfile(): Profile {
        return Profile(
            header = Header(
                id = "9958f568-8c9b-476a-beeb-017d1f843266",
                creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)",
                lastModified = Instant.now(),
                creationDate = Instant.now(),
                snapshotVersion = 1
            ),
            appPreferences = AppPreferences(
                display = Display.default,
                security = Security.default,
                gateways = Gateways(Radix.Gateway.default.url, listOf(Radix.Gateway.default)),
                p2pLinks = emptyList()
            ),
            factorSources = listOf(
                FactorSource.babylon(
                    mnemonicWithPassphrase = MnemonicWithPassphrase(
                        mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
                        bip39Passphrase = ""
                    )
                )
            ),
            networks = emptyList()
        )
    }

    fun sampleFungibleTokens(
        ownerAddress: String = randomAddress(),
        amount: Pair<BigDecimal, String> = BigDecimal.valueOf(100000) to "XRD"
    ): List<OwnedFungibleToken> {
        val result = mutableListOf<OwnedFungibleToken>()
        return result.apply {
            repeat(3) {
                val tokenAddress = randomAddress()
                add(
                    OwnedFungibleToken(
                        AccountAddress(ownerAddress),
                        amount.first,
                        tokenAddress,
                        FungibleToken(
                            tokenAddress,
                            metadata = mapOf("symbol" to amount.second)
                        )
                    )
                )
            }
        }
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

    val mockTokenUiList = sampleFungibleTokens().map { ownedFungibleToken ->
        ownedFungibleToken.toTokenUiModel()
    }

    fun sampleSimpleFungibleTokens(address: String = randomAddress()): List<SimpleOwnedFungibleToken> {
        val tokenAddress = randomAddress()
        return listOf(
            SimpleOwnedFungibleToken(
                AccountAddress(address),
                BigDecimal.valueOf(100000),
                tokenAddress,
            ),
            SimpleOwnedFungibleToken(
                AccountAddress(address),
                BigDecimal.valueOf(100000),
                tokenAddress,
            ),
            SimpleOwnedFungibleToken(
                AccountAddress(address),
                BigDecimal.valueOf(100000),
                tokenAddress,
            )
        )
    }

    val mockNftUiList = listOf(
        OwnedNonFungibleToken(
            owner = AccountAddress(
                address = "owner address",
                label = "NBA"
            ),
            amount = 10L,
            tokenResourceAddress = "token resource address",
            token = NonFungibleToken(
                address = "non fungible token address",
                nonFungibleIdContainer = NonFungibleTokenIdContainer(
                    ids = listOf("id1", "id2", "id3"),
                    nextCursor = "next cursor",
                    previousCursor = "previous cursor"
                ),
                metadataContainer = NonFungibleMetadataContainer(
                    metadata = emptyMap(),
                    nextCursor = "meta next cursor",
                    previousCursor = "meta previous cursor"
                )
            )
        ),
        OwnedNonFungibleToken(
            owner = AccountAddress(
                address = "owner address",
                label = "Space"
            ),
            amount = 10L,
            tokenResourceAddress = "token resource address",
            token = NonFungibleToken(
                address = "non fungible token address",
                nonFungibleIdContainer = NonFungibleTokenIdContainer(
                    ids = listOf("id1", "id2", "id3"),
                    nextCursor = "next cursor",
                    previousCursor = "previous cursor"
                ),
                metadataContainer = NonFungibleMetadataContainer(
                    metadata = emptyMap(),
                    nextCursor = "meta next cursor",
                    previousCursor = "meta previous cursor"
                )
            )
        )
    )
}

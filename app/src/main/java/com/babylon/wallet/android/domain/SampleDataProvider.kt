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
import com.radixdlt.toolkit.models.Value
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.FactorSourceReference
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.SecurityState
import java.math.BigDecimal

class SampleDataProvider {

    fun randomTokenAddress(): String {
        val characters = "abcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder()
        repeat((0 until 26).count()) {
            sb.append(characters.random())
        }
        return sb.toString()
    }

    fun samplePersona(personaId: String = "1", personaName: String = "Test Persona"): OnNetwork.Persona {
        return OnNetwork.Persona(
            address = personaId,
            derivationPath = "m/1'/1'/1'/1'/1'/1'",
            displayName = personaName,
            index = 0,
            networkID = 10,
            fields = listOf(
                OnNetwork.Persona.Field("1", OnNetwork.Persona.Field.Kind.Email, "test@test.pl"),
                OnNetwork.Persona.Field("2", OnNetwork.Persona.Field.Kind.FirstName, "John")
            ),
            securityState = SecurityState.Unsecured(
                discriminator = "dsics",
                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                    genesisFactorInstance = FactorInstance(
                        derivationPath = DerivationPath("few", "disc"),
                        factorInstanceID = "IDIDDIIDD",
                        factorSourceReference = FactorSourceReference(
                            factorSourceID = "f32f3",
                            factorSourceKind = "kind"
                        ),
                        initializationDate = "Date1",
                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                    )
                )
            )
        )
    }

    fun sampleAccountResource(address: String = randomTokenAddress()): AccountResources {
        return AccountResources(
            address = address,
            displayName = "My account",
            currencySymbol = "$",
            value = "10",
            fungibleTokens = sampleFungibleTokens(address),
            appearanceID = 1
        )
    }

    fun sampleProfile(): Profile {
        return Profile(
            appPreferences = AppPreferences(
                display = Display.default,
                networkAndGateway = NetworkAndGateway.hammunet,
                p2pClients = emptyList()
            ),
            factorSources = FactorSources(
                curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources = emptyList(),
                secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources = emptyList()
            ),
            onNetwork = emptyList(),
            version = 1
        )
    }

    fun sampleFungibleTokens(ownerAddress: String = randomTokenAddress()): List<OwnedFungibleToken> {
        val result = mutableListOf<OwnedFungibleToken>()
        return result.apply {
            repeat(3) {
                val tokenAddress = randomTokenAddress()
                add(
                    OwnedFungibleToken(
                        AccountAddress(ownerAddress),
                        BigDecimal.valueOf(100000),
                        tokenAddress,
                        FungibleToken(
                            tokenAddress,
                            totalSupply = BigDecimal.valueOf(10000000000),
                            totalMinted = BigDecimal.valueOf(1000000),
                            totalBurnt = BigDecimal.valueOf(100),
                            metadata = mapOf("symbol" to "XRD")
                        )
                    )
                )
            }
        }
    }

    fun sampleManifest(): TransactionManifest {
        return ManifestBuilder()
            .callMethod(
                Value.ComponentAddress("component_tdx_b_1qftacppvmr9ezmekxqpq58en0nk954x0a7jv2zz0hc7qdxyth4"),
                "free",
            )
            .callMethod(
                Value.ComponentAddress("account_tdx_b_1qdcgrj7mz09cz3htn0y7qtcze7tq59s76p2h98puqtpst7jh4u"),
                "deposit_batch",
                Value.Expression("ENTIRE_WORKTOP")
            )
            .build()
    }

    val mockTokenUiList = sampleFungibleTokens().map { ownedFungibleToken ->
        ownedFungibleToken.toTokenUiModel()
    }

    fun sampleSimpleFungibleTokens(address: String = randomTokenAddress()): List<SimpleOwnedFungibleToken> {
        val tokenAddress = randomTokenAddress()
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
            amount = BigDecimal(1.007),
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
            amount = BigDecimal(1.007),
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

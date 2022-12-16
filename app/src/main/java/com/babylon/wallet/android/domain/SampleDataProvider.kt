@file:Suppress("MagicNumber")

package com.babylon.wallet.android.domain

import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.domain.model.SimpleOwnedFungibleToken
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.factorsources.FactorSources
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

    fun sampleAccountResource(address: String = randomTokenAddress()): AccountResources {
        return AccountResources(
            address = address,
            displayName = "My account",
            currencySymbol = "$",
            value = "10",
            fungibleTokens = sampleFungibleTokens(address)
        )
    }

    fun sampleProfileSnapshot(): ProfileSnapshot {
        return ProfileSnapshot(
            appPreferences = AppPreferences(
                display = Display.default,
                networkAndGateway = NetworkAndGateway.hammunet,
                p2pClients = emptyList()
            ),
            factorSources = FactorSources(
                curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources = emptyList(),
                secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources = emptyList()
            ),
            perNetwork = emptyList(),
            version = "0.0.1"
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
}

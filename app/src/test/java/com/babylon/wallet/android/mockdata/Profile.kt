package com.babylon.wallet.android.mockdata

import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.*

val profile = Profile(
    id = "9958f568-8c9b-476a-beeb-017d1f843266",
    creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)",
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
    networks = listOf(
        Network(
            networkID = Radix.Gateway.default.network.id,
            accounts = listOf(
                account1,
                account2
            ),
            personas = emptyList(),
            authorizedDapps = emptyList()
        )
    ),
    version = 1
)

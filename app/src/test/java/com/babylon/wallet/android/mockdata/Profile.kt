package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.SampleDataProvider
import rdx.works.profile.data.model.Header
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import java.time.Instant

fun profile(
    accounts: List<Network.Account> = listOf(account("acc-1"), account("acc-2")),
    personas: List<Network.Persona> = listOf(SampleDataProvider().samplePersona()),
    dApps: List<Network.AuthorizedDapp> = emptyList(),
    p2pLinks: List<P2PLink> = emptyList()
) = Profile(
    header = Header.init(
        id = "9958f568-8c9b-476a-beeb-017d1f843266",
        creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)",
        creationDate = Instant.now(),
        numberOfNetworks = 1
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
        ),
        FactorSource.ledger(
            id = FactorSource.ID("Ledger1"),
            model = FactorSource.LedgerHardwareWallet.DeviceModel.NanoS,
            name = "My Ledger"
        )
    ),
    networks = listOf(
        Network(
            networkID = Radix.Gateway.default.network.id,
            accounts = accounts,
            personas = personas,
            authorizedDapps = dApps
        )
    )
)

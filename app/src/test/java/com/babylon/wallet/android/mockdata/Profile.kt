package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.SampleDataProvider
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.IdentifiedArrayList
import rdx.works.core.InstantGenerator
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
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.TestData

fun profile(
    accounts: IdentifiedArrayList<Network.Account> = identifiedArrayListOf(account("acc-1"), account("acc-2")),
    personas: IdentifiedArrayList<Network.Persona> = identifiedArrayListOf(SampleDataProvider().samplePersona()),
    dApps: List<Network.AuthorizedDapp> = emptyList(),
    p2pLinks: List<P2PLink> = emptyList(),
    gateway: Radix.Gateway = Radix.Gateway.default,
    transaction: Transaction = Transaction.default
) = Profile(
    header = Header.init(
        id = "9958f568-8c9b-476a-beeb-017d1f843266",
        deviceInfo = TestData.deviceInfo,
        creationDate = InstantGenerator(),
        numberOfNetworks = 1
    ),
    appPreferences = AppPreferences(
        transaction = transaction,
        display = Display.default,
        security = Security.default,
        gateways = Gateways(gateway.url, listOf(gateway)),
        p2pLinks = emptyList()
    ),
    factorSources = identifiedArrayListOf(
        DeviceFactorSource.babylon(
            mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
                bip39Passphrase = ""
            )
        ),
        LedgerHardwareWalletFactorSource.newSource(
            deviceID = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5"),
            model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
            name = "My Ledger"
        )
    ),
    networks = listOf(
        Network(
            networkID = gateway.network.id,
            accounts = accounts,
            personas = personas,
            authorizedDapps = dApps
        )
    )
)

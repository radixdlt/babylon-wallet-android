package com.babylon.wallet.android.mockdata

import com.babylon.wallet.android.domain.SampleDataProvider
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.IdentifiedArrayList
import rdx.works.core.InstantGenerator
import rdx.works.core.UUIDGenerator
import rdx.works.core.identifiedArrayListOf
import rdx.works.profile.data.model.DeviceInfo
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
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.extensions.createAccount
import rdx.works.profile.data.model.extensions.nextAccountIndex
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.domain.TestData
import java.time.Instant

fun profile(
    accounts: IdentifiedArrayList<Network.Account> = identifiedArrayListOf(
        account(AccountAddress.sampleMainnet.random().string),
        account(AccountAddress.sampleMainnet.random().string)
    ),
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
        p2pLinks = p2pLinks
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


fun createProfile(
    gateway: Radix.Gateway = Radix.Gateway.default,
    numberOfAccounts: Int = 2
): Profile {
    val gateways = try {
        Gateways.preset.add(gateway)
    } catch (exception: Exception) {
        Gateways.preset
    }.changeCurrent(gateway)

    val mnemonic = MnemonicWithPassphrase(
        mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
        bip39Passphrase = ""
    )
    val factorSource = DeviceFactorSource.babylon(mnemonicWithPassphrase = mnemonic)

    var profile = Profile.initWithFactorSource(
        id = UUIDGenerator.uuid().toString(),
        deviceInfo = DeviceInfo(name = "Unit", manufacturer = "Test", model = "junit"),
        creationDate = Instant.now(),
        gateways = gateways,
        factorSource = factorSource,
        accounts = identifiedArrayListOf()
    )

    repeat(numberOfAccounts) { index ->
        val derivationPath = DerivationPath.forAccount(
            networkId = gateway.network.networkId(),
            accountIndex = profile.nextAccountIndex(
                factorSource = factorSource,
                derivationPathScheme = DerivationPathScheme.CAP_26,
                forNetworkId = gateway.network.networkId()
            ),
            keyType = KeyType.TRANSACTION_SIGNING
        )

        profile = profile.addAccounts(
            accounts = listOf(
                profile.createAccount(
                    displayName = "Account$index",
                    onNetworkId = gateway.network.networkId(),
                    compressedPublicKey = mnemonic.compressedPublicKey(derivationPath = derivationPath).removeLeadingZero(),
                    derivationPath = derivationPath,
                    factorSource = factorSource,
                    onLedgerSettings = Network.Account.OnLedgerSettings.init()
                )
            ),
            onNetwork = gateway.network.networkId()
        )
    }

    return profile
}
package rdx.works.profile.domain

import rdx.works.core.HexCoded32Bytes
import rdx.works.core.InstantGenerator
import rdx.works.core.toDistinctList
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
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import java.util.Random

object TestData {

    val ledgerFactorSource = LedgerHardwareWalletFactorSource.newSource(
        deviceID = HexCoded32Bytes(generateRandomHexString32Bytes()),
        model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
        name = "Ledger1"
    )

    val deviceInfo = DeviceInfo(
        name = "Galaxy A53 5G",
        manufacturer = "Samsung",
        model = "SM-A536B"
    )

    @Suppress("LongMethod")
    fun testProfile2Networks2AccountsEach(mnemonicWithPassphrase: MnemonicWithPassphrase): Profile {
        val network1 = Radix.Gateway.hammunet
        val network2 = Radix.Gateway.kisharnet
        return Profile(
            header = Header.init(
                id = "9958f568-8c9b-476a-beeb-017d1f843266",
                deviceInfo = deviceInfo,
                creationDate = InstantGenerator(),
                numberOfNetworks = 2,
                numberOfAccounts = 4
            ),
            appPreferences = AppPreferences(
                transaction = Transaction.default,
                display = Display.default,
                security = Security.default,
                gateways = Gateways(network1.url, listOf(network1, network2)),
                p2pLinks = listOf(
                    P2PLink.init(
                        connectionPassword = "My password",
                        displayName = "Browser name test"
                    )
                )
            ),
            factorSources = listOf(
                DeviceFactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase),
                ledgerFactorSource
            ).toDistinctList(),
            networks = listOf(
                Network(
                    accounts = listOf(
                        Network.Account(
                            address = "fj3489fj348f",
                            appearanceID = 123,
                            displayName = "my account",
                            networkID = network1.network.networkId().value,
                            securityState = SecurityState.Unsecured(
                                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                    entityIndex = 0,
                                    transactionSigning = FactorInstance(
                                        badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                                            derivationPath = DerivationPath.forAccount(
                                                networkId = network1.network.networkId(),
                                                accountIndex = 0,
                                                keyType = KeyType.TRANSACTION_SIGNING
                                            ),
                                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                        ),
                                        factorSourceId = FactorSource.FactorSourceID.FromHash(
                                            kind = FactorSourceKind.DEVICE,
                                            body = HexCoded32Bytes(
                                                "5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5"
                                            )
                                        )
                                    )
                                )
                            ),
                            onLedgerSettings = Network.Account.OnLedgerSettings.init()
                        ),
                        Network.Account(
                            address = "fj3489fj348f",
                            appearanceID = 123,
                            displayName = "my account",
                            networkID = network1.network.networkId().value,
                            securityState = SecurityState.Unsecured(
                                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                    entityIndex = 1,
                                    transactionSigning = FactorInstance(
                                        badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                                            derivationPath = DerivationPath.forAccount(
                                                networkId = network1.network.networkId(),
                                                accountIndex = 1,
                                                keyType = KeyType.TRANSACTION_SIGNING
                                            ),
                                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                        ),
                                        factorSourceId = FactorSource.FactorSourceID.FromHash(
                                            kind = FactorSourceKind.DEVICE,
                                            body = HexCoded32Bytes(
                                                "5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5"
                                            )
                                        )
                                    )
                                )
                            ),
                            onLedgerSettings = Network.Account.OnLedgerSettings.init()
                        )
                    ),
                    authorizedDapps = emptyList(),
                    networkID = network1.network.networkId().value,
                    personas = emptyList()
                ),
                Network(
                    accounts = listOf(
                        Network.Account(
                            address = "fj3489fj348f",
                            appearanceID = 123,
                            displayName = "my account",
                            networkID = network2.network.networkId().value,
                            securityState = SecurityState.Unsecured(
                                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                    entityIndex = 2,
                                    transactionSigning = FactorInstance(
                                        badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                                            derivationPath = DerivationPath.forAccount(
                                                networkId = network2.network.networkId(),
                                                accountIndex = 2,
                                                keyType = KeyType.TRANSACTION_SIGNING
                                            ),
                                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                        ),
                                        factorSourceId = FactorSource.FactorSourceID.FromHash(
                                            kind = FactorSourceKind.DEVICE,
                                            body = HexCoded32Bytes(
                                                "5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5"
                                            )
                                        )
                                    )
                                )
                            ),
                            onLedgerSettings = Network.Account.OnLedgerSettings.init()
                        ),
                        Network.Account(
                            address = "fj3489fj348f",
                            appearanceID = 123,
                            displayName = "my account",
                            networkID = network2.network.networkId().value,
                            securityState = SecurityState.Unsecured(
                                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                    entityIndex = 3,
                                    transactionSigning = FactorInstance(
                                        badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
                                            derivationPath = DerivationPath.forAccount(
                                                networkId = network2.network.networkId(),
                                                accountIndex = 3,
                                                keyType = KeyType.TRANSACTION_SIGNING
                                            ),
                                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                        ),
                                        factorSourceId = FactorSource.FactorSourceID.FromHash(
                                            kind = FactorSourceKind.DEVICE,
                                            body = HexCoded32Bytes(
                                                "5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5"
                                            )
                                        )
                                    )
                                )
                            ),
                            onLedgerSettings = Network.Account.OnLedgerSettings.init()
                        )
                    ),
                    authorizedDapps = emptyList(),
                    networkID = network2.network.networkId().value,
                    personas = emptyList()
                )
            )
        )
    }

    @Suppress("MagicNumber")
    private fun generateRandomHexString32Bytes(): String {
        val random = Random()
        val sb = StringBuilder()
        while (sb.length < 63) {
            sb.append(Integer.toHexString(random.nextInt(0x10) + 0x10)) // Generates a random number between 0x10 and 0x20
        }
        return sb.toString()
    }
}

package rdx.works.profile.domain

import rdx.works.core.UUIDGenerator
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
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import java.time.Instant

object TestData {

    val ledgerFactorSourceID = FactorSource.ID(UUIDGenerator.uuid().toString())

    val ledgerFactorSource = FactorSource.ledger(
        id = ledgerFactorSourceID,
        model = FactorSource.LedgerHardwareWallet.DeviceModel.NanoS,
        name = "Ledger1"
    )

    @Suppress("LongMethod")
    fun testProfile2Networks2AccountsEach(mnemonicWithPassphrase: MnemonicWithPassphrase): Profile {
        val network1 = Radix.Gateway.hammunet
        val network2 = Radix.Gateway.kisharnet
        return Profile(
            header = Header.init(
                id = "9958f568-8c9b-476a-beeb-017d1f843266",
                creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)",
                creationDate = Instant.now(),
                numberOfNetworks = 2,
                numberOfAccounts = 4
            ),
            appPreferences = AppPreferences(
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
                FactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase),
                ledgerFactorSource
            ),
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
                                    transactionSigning = FactorInstance(
                                        derivationPath = DerivationPath.forAccount(
                                            networkId = network1.network.networkId(),
                                            accountIndex = 0,
                                            keyType = KeyType.TRANSACTION_SIGNING
                                        ),
                                        factorSourceId = FactorSource.ID("IDIDDIIDD"),
                                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                    )
                                )
                            )
                        ),
                        Network.Account(
                            address = "fj3489fj348f",
                            appearanceID = 123,
                            displayName = "my account",
                            networkID = network1.network.networkId().value,
                            securityState = SecurityState.Unsecured(
                                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                    transactionSigning = FactorInstance(
                                        derivationPath = DerivationPath.forAccount(
                                            networkId = network1.network.networkId(),
                                            accountIndex = 0,
                                            keyType = KeyType.TRANSACTION_SIGNING
                                        ),
                                        factorSourceId = FactorSource.ID("IDIDDIIDD"),
                                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                    )
                                )
                            )
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
                                    transactionSigning = FactorInstance(
                                        derivationPath = DerivationPath.forAccount(
                                            networkId = network2.network.networkId(),
                                            accountIndex = 0,
                                            keyType = KeyType.TRANSACTION_SIGNING
                                        ),
                                        factorSourceId = FactorSource.ID("IDIDDIIDD"),
                                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                    )
                                )
                            )
                        ),
                        Network.Account(
                            address = "fj3489fj348f",
                            appearanceID = 123,
                            displayName = "my account",
                            networkID = network2.network.networkId().value,
                            securityState = SecurityState.Unsecured(
                                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                    transactionSigning = FactorInstance(
                                        derivationPath = DerivationPath.forAccount(
                                            networkId = network2.network.networkId(),
                                            accountIndex = 0,
                                            keyType = KeyType.TRANSACTION_SIGNING
                                        ),
                                        factorSourceId = FactorSource.ID("IDIDDIIDD"),
                                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                    )
                                )
                            )
                        )
                    ),
                    authorizedDapps = emptyList(),
                    networkID = network2.network.networkId().value,
                    personas = emptyList()
                )
            )
        )
    }
}

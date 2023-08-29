package rdx.works.profile

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.addP2PLink
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSource.Companion.factorSourceId
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.factorsources.OffDeviceMnemonicFactorSource
import rdx.works.profile.data.model.factorsources.TrustedContactFactorSource
import rdx.works.profile.data.model.pernetwork.CountryOrRegion
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.IdentifiedEntry
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.Companion.initAccountWithDeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Network.Persona.Companion.init
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.RequestedNumber
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.Shared
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.model.pernetwork.addPersona
import rdx.works.profile.data.model.pernetwork.nextAccountIndex
import rdx.works.profile.data.model.pernetwork.nextPersonaIndex
import rdx.works.core.serializers.InstantSerializer
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.createOrUpdateAuthorizedDapp
import java.io.File
import java.time.Instant

class ProfileTest {

    private val json = Json {
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantSerializer)
        }
    }

    @Test
    fun `test profile generation`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
            bip39Passphrase = ""
        )
        val babylonFactorSource = DeviceFactorSource.babylon(
            mnemonicWithPassphrase, model = "Galaxy A53 5G (Samsung SM-A536B)",
            name = "Samsung"
        )
        val mnemonicRepository = mockk<MnemonicRepository>()
        coEvery { mnemonicRepository() } returns mnemonicWithPassphrase

        val profile = Profile.init(
            id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
            deviceName = "Galaxy A53 5G (Samsung SM-A536B)",
            creationDate = InstantGenerator()
        ).copy(factorSources = listOf(babylonFactorSource))

        val defaultNetwork = Radix.Gateway.default.network
        assertEquals(profile.networks.count(), 1)
        assertEquals(profile.networks.first().networkID, defaultNetwork.id)
        assertEquals(profile.networks.first().accounts.count(), 0)
        assertEquals(profile.networks.first().personas.count(), 0)
        assertEquals(
            "Next derivation index for first account",
            0,
            (profile.nextAccountIndex(defaultNetwork.networkId()))
        )

        println("Profile generated $profile")

        val firstAccount = initAccountWithDeviceFactorSource(
            entityIndex = 0,
            displayName = "first account",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = (profile.factorSources.first() as DeviceFactorSource),
            networkId = defaultNetwork.networkId(),
            appearanceID = 0
        )

        var updatedProfile = profile.addAccount(
            account = firstAccount,
            onNetwork = defaultNetwork.networkId()
        )

        println("Profile updated generated $updatedProfile")
        assertEquals(updatedProfile.networks.first().accounts.count(), 1)
        assertEquals(
            "Next derivation index for second account",
            1,
            updatedProfile.nextAccountIndex(defaultNetwork.networkId()),
        )

        val firstPersona = init(
            entityIndex = 0,
            displayName = "First",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            factorSource = (profile.factorSources.first() as DeviceFactorSource),
            networkId = defaultNetwork.networkId()
        )

        updatedProfile = updatedProfile.addPersona(
            persona = firstPersona,
            onNetwork = defaultNetwork.networkId()
        )

        assertEquals(updatedProfile.networks.first().personas.count(), 1)
        assertEquals(
            "Next derivation index for second persona",
            1,
            updatedProfile.nextPersonaIndex(defaultNetwork.networkId())
        )

        val p2pLink = P2PLink.init(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )
        updatedProfile = updatedProfile.addP2PLink(
            p2pLink = p2pLink
        )

        assertEquals(1, updatedProfile.appPreferences.p2pLinks.count())

        Assert.assertTrue(updatedProfile.header.id.isNotBlank())
    }

    @Test
    fun `test against profile json vector`() {
        val profileTestVector = File("src/test/resources/raw/profile_snapshot.json").readText()

        val actual = json.decodeFromString<ProfileSnapshot>(profileTestVector).toProfile()

        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman humble limb repeat video " +
                    "sudden possible story mask neutral prize goose mandate",
            bip39Passphrase = ""
        )

        val gateway = Radix.Gateway.default
        val networkId = gateway.network.networkId()
        val babylonFactorSource = DeviceFactorSource.babylon(
            mnemonicWithPassphrase, model = "computer",
            name = "unit test",
            createdAt = Instant.EPOCH
        )
        var expected = Profile.init(
            id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
            deviceName = "unit test",
            creationDate = Instant.EPOCH,
            gateway = gateway
        ).copy(factorSources = listOf(babylonFactorSource))
        expected = expected.copy(
            factorSources = expected.factorSources + listOf(
                DeviceFactorSource.olympia(mnemonicWithPassphrase),
                TrustedContactFactorSource.newSource(
                    accountAddress = FactorSource.AccountAddress("account_rdx1283u6e8r2jnz4a3jwv0hnrqfr5aq50yc9ts523sd96hzfjxqqcs89q"),
                    emailAddress = "hi@rdx.works",
                    name = "My friend",
                    createdAt = Instant.EPOCH
                ),
                OffDeviceMnemonicFactorSource.newSource(
                    mnemonicWithPassphrase = mnemonicWithPassphrase,
                    label = "Zoo"
                ),
                LedgerHardwareWalletFactorSource.newSource(
                    model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S_PLUS,
                    name = "Orange",
                    deviceID = FactorSource.HexCoded32Bytes(
                        value = factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase)
                    )
                )
            )
        )

        val firstAccount = initAccountWithDeviceFactorSource(
            entityIndex = 0,
            displayName = "First",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = expected.babylonDeviceFactorSource,
            networkId = networkId,
            appearanceID = 0
        )
        expected = expected.addAccount(
            account = firstAccount,
            onNetwork = networkId
        )

        val secondAccount = initAccountWithDeviceFactorSource(
            entityIndex = 1,
            displayName = "Second",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = expected.babylonDeviceFactorSource,
            networkId = networkId,
            appearanceID = 1,
            onLedgerSettings = Network.Account.OnLedgerSettings(
                thirdPartyDeposits = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
                    depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown,
                    assetsExceptionList = listOf(
                        Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException(
                            "resource_tdx_21_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxsmgder",
                            exceptionRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Deny
                        )
                    ),
                    depositorsAllowList = listOf(
                        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress.ResourceAddress(
                            "resource_tdx_21_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxsmgder"
                        ),
                        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID(
                            "resource_tdx_21_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxsmgder:#2#"
                        ),
                    )
                )
            )
        )
        expected = expected.addAccount(
            account = secondAccount,
            onNetwork = networkId
        )

        val thirdAccount = initAccountWithDeviceFactorSource(
            entityIndex = 2,
            displayName = "Third",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = expected.babylonDeviceFactorSource,
            networkId = networkId,
            appearanceID = 2,
            onLedgerSettings = Network.Account.OnLedgerSettings(
                thirdPartyDeposits = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
                    depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll,
                    assetsExceptionList = listOf(
                        Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException(
                            "resource_tdx_21_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxsmgder",
                            exceptionRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Allow
                        )
                    ),
                    depositorsAllowList = listOf(
                        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress.ResourceAddress(
                            "resource_tdx_21_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxsmgder"
                        ),
                        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID(
                            "resource_tdx_21_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxsmgder:#2#"
                        ),
                    )
                )
            )
        )
        expected = expected.addAccount(
            account = thirdAccount,
            onNetwork = networkId
        )

        val firstPersona = init(
            entityIndex = 0,
            displayName = "Satoshi",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            factorSource = expected.babylonDeviceFactorSource,
            networkId = networkId,
            personaData = satoshiPersona()
        )
        expected = expected.addPersona(
            persona = firstPersona,
            onNetwork = networkId
        )

        val secondPersona = init(
            entityIndex = 1,
            displayName = "Mrs Public",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            factorSource = expected.babylonDeviceFactorSource,
            networkId = networkId,
            personaData = PersonaData(
                name = IdentifiedEntry.init(
                    PersonaData.PersonaDataField.Name(
                        variant = PersonaData.PersonaDataField.Name.Variant.Western,
                        given = "Maria",
                        family = "Publicson",
                        nickname = "MP"
                    ),
                    id = "0"
                )
            )
        )
        expected = expected.addPersona(
            persona = secondPersona,
            onNetwork = networkId
        )

        val p2pLink = P2PLink.init(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )

        expected = expected.addP2PLink(
            p2pLink = p2pLink
        )
        expected = expected.addP2PLink(
            p2pLink = P2PLink.init(
                connectionPassword = "beefbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadebeef",
                displayName = "iPhone 13"
            )
        )

        val firstRequest = RequestedNumber(
            RequestedNumber.Quantifier.AtLeast,
            1
        )
        val authorizedDapp = Network.AuthorizedDapp(
            networkID = networkId.value,
            dAppDefinitionAddress = "account_sim1cyvgx33089ukm2pl97pv4max0x40ruvfy4lt60yvya744cve475w0q",
            displayName = "RadiSwap",
            referencesToAuthorizedPersonas = listOf(
                Network.AuthorizedDapp.AuthorizedPersonaSimple(
                    identityAddress = firstPersona.address,
                    sharedAccounts =
                    Shared(
                        ids = listOf(
                            secondAccount.address,
                            thirdAccount.address
                        ),
                        request = RequestedNumber(
                            RequestedNumber.Quantifier.Exactly,
                            2
                        )
                    ),
                    lastLogin = Instant.EPOCH.toString(),
                    sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData.init(firstPersona.personaData, firstRequest)
                ),
                Network.AuthorizedDapp.AuthorizedPersonaSimple(
                    identityAddress = secondPersona.address,
                    sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData(
                        name = secondPersona.personaData.name?.id
                    ),
                    sharedAccounts =
                    Shared(
                        ids = listOf(
                            secondAccount.address
                        ),
                        request = RequestedNumber(
                            RequestedNumber.Quantifier.AtLeast,
                            1
                        )
                    ),
                    lastLogin = Instant.EPOCH.toString(),
                )
            )
        )
        expected = expected.createOrUpdateAuthorizedDapp(
            unverifiedAuthorizedDapp = authorizedDapp
        )

        // Network and gateway
        assertEquals(
            "Gateways are the same",
            expected.appPreferences.gateways,
            actual.appPreferences.gateways
        )

        // defaultDepositGuarantee
        assertEquals(
            "Default deposit guarantees are the same",
            expected.appPreferences.transaction.defaultDepositGuarantee,
            actual.appPreferences.transaction.defaultDepositGuarantee,
            0.0
        )

        // Display
        assertEquals(
            "Fiat currency is the same",
            expected.appPreferences.display.fiatCurrencyPriceTarget,
            actual.appPreferences.display.fiatCurrencyPriceTarget
        )

        assertEquals(
            "Currency amount visible is the same",
            expected.appPreferences.display.isCurrencyAmountVisible,
            actual.appPreferences.display.isCurrencyAmountVisible
        )

        // Security
        assertEquals(
            "Developer mode is the same",
            expected.appPreferences.security.isDeveloperModeEnabled,
            actual.appPreferences.security.isDeveloperModeEnabled
        )

        // Security
        assertEquals(
            "Cloud profile sync is the same",
            expected.appPreferences.security.isCloudProfileSyncEnabled,
            actual.appPreferences.security.isCloudProfileSyncEnabled
        )

        // P2P clients
        assertEquals(
            "P2P clients count is the same",
            expected.appPreferences.p2pLinks.count(),
            actual.appPreferences.p2pLinks.count()
        )
        assertEquals(
            "Connection password is the same for the first p2p client",
            expected.appPreferences.p2pLinks.first().connectionPassword,
            actual.appPreferences.p2pLinks.first().connectionPassword
        )
        assertEquals(
            "The display name is the same for the first p2p client",
            expected.appPreferences.p2pLinks.first().displayName,
            actual.appPreferences.p2pLinks.first().displayName
        )

        // Factor Sources
        assertEquals(
            "The factor sources count are the same",
            expected.factorSources.count(),
            actual.factorSources.count()
        )

        assertEquals(
            "The first factor sources are devices and are the same",
            (expected.factorSources.first() as DeviceFactorSource),
            (actual.factorSources.first() as DeviceFactorSource),
        )

        assertEquals(
            "The third factor sources are trusted contact sources and are the same",
            (expected.factorSources[2] as TrustedContactFactorSource),
            (actual.factorSources[2] as TrustedContactFactorSource),
        )

        assertEquals(
            "The id of the first factor source is the same",
            expected.factorSources.first().id,
            actual.factorSources.first().id
        )

        assertEquals(
            "The next id for creating an account in this factor source",
            expected.nextAccountIndex(networkId),
            actual.nextAccountIndex(networkId)
        )

        assertEquals(
            "The next id for creating an identity in this factor source",
            expected.nextPersonaIndex(networkId),
            actual.nextPersonaIndex(networkId)
        )

        // Per Network count
        assertEquals(
            "The networks count is the same",
            expected.networks.count(),
            actual.networks.count()
        )

        // Network ID
        assertEquals(
            "The first network id is the same",
            expected.networks.first().networkID,
            actual.networks.first().networkID
        )

        // Connected Dapp
        assertEquals(
            "Authorised dApps count is the same",
            expected.networks.first().authorizedDapps.count(),
            actual.networks.first().authorizedDapps.count()
        )

        assertEquals(
            "The first dApps' network id is the same",
            expected.networks.first().authorizedDapps.first().networkID,
            actual.networks.first().authorizedDapps.first().networkID
        )

        assertEquals(
            "The first dApps' display name is the same",
            expected.networks.first().authorizedDapps.first().displayName,
            actual.networks.first().authorizedDapps.first().displayName
        )

        assertEquals(
            "The first dApps' definition address is the same",
            expected.networks.first().authorizedDapps.first().dAppDefinitionAddress,
            actual.networks.first().authorizedDapps.first().dAppDefinitionAddress
        )

        assertEquals(
            "The first dApps' references to authorised personals size is the same",
            expected.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas.size,
            actual.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas.size
        )

        assertEquals(
            "The first dApps' references to the first authorised persona identity address is the same",
            expected.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().identityAddress,
            actual.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().identityAddress
        )

        assertEquals(
            "The first dApps' references to the first authorised persona field ids is the same",
            expected.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().sharedPersonaData,
            actual.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().sharedPersonaData
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts requests is the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.request,
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.request
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts referenced by address count is the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.ids.size,
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.ids.size
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts referenced by address first element is the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.ids.elementAt(0),
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.ids.elementAt(0)
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts referenced by address second element is the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.ids.elementAt(1),
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.ids.elementAt(1)
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona identity address is the same",
            expected.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).identityAddress,
            actual.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).identityAddress
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona field ids is the same",
            expected.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).sharedPersonaData,
            actual.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).sharedPersonaData
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona identity address is the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.request,
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.request
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona shared accounts reference by address are the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.ids.size,
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.ids.size
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona first shared account reference by address is the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.ids.elementAt(0),
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.ids.elementAt(0)
        )


        // Accounts
        assertEquals(
            "The accounts' count is the same",
            expected.networks.first().accounts.count(),
            actual.networks.first().accounts.count()
        )

        repeat(3) { accountIndex ->
            assertEquals(
                "The account entity index is the same",
                (expected.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.entityIndex,
                (actual.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.entityIndex
            )

            assertEquals(
                "The transaction signing badge is the same",
                (expected.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic,
                (actual.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic
            )

            assertEquals(
                "The accounts[$accountIndex] addresses are the same",
                expected.networks.first().accounts[accountIndex].address,
                actual.networks.first().accounts[accountIndex].address
            )

            assertEquals(
                "The accounts[$accountIndex] display name are the same",
                expected.networks.first().accounts[accountIndex].displayName,
                actual.networks.first().accounts[accountIndex].displayName
            )

            // Security State
            val expectedTransactionSigning = (expected.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                .unsecuredEntityControl.transactionSigning
            val expectedDerivationPath = (expectedTransactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic)
                .derivationPath
            val actualTransactionSigning = (actual.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                .unsecuredEntityControl.transactionSigning
            val actualDerivationPath = (actualTransactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic)
                .derivationPath
            assertEquals(
                "The accounts[$accountIndex] derivation path are the same",
                expectedDerivationPath,
                actualDerivationPath
            )

            val expectedPublicKey = (expectedTransactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic)
                .publicKey
            val actualPublicKey = (actualTransactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic)
                .publicKey
            assertEquals(
                "The accounts[$accountIndex] public key are the same",
                expectedPublicKey,
                actualPublicKey
            )

            assertEquals(
                "The accounts[$accountIndex] factor source ids are the same",
                (expected.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.factorSourceId,
                (actual.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.factorSourceId
            )

            assertEquals(
                "The accounts[$accountIndex] onLedgerSettings are the same",
                expected.networks.first().accounts[accountIndex].onLedgerSettings,
                actual.networks.first().accounts[accountIndex].onLedgerSettings
            )
        }

        // Personas
        assertEquals(
            expected.networks.first().personas.count(),
            actual.networks.first().personas.count()
        )

        repeat(2) { personaIndex ->
            assertEquals(
                "The persona entity index is the same",
                (expected.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.entityIndex,
                (actual.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.entityIndex
            )

            assertEquals(
                "The transaction signing badge is the same",
                (expected.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic,
                (actual.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic
            )

            assertEquals(
                "The persona[$personaIndex] address is the same",
                expected.networks.first().personas[personaIndex].address,
                actual.networks.first().personas[personaIndex].address
            )

            assertEquals(
                "The persona[$personaIndex] display name is the same",
                expected.networks.first().personas[personaIndex].displayName,
                actual.networks.first().personas[personaIndex].displayName
            )

            assertEquals(
                "The persona[$personaIndex] first field kind is the same",
                expected.networks.first().personas[personaIndex].personaData.name?.id,
                actual.networks.first().personas[personaIndex].personaData.name?.id
            )
            assertEquals(
                "The persona[$personaIndex] given name is the same",
                expected.networks.first().personas[personaIndex].personaData.name?.value?.given,
                actual.networks.first().personas[personaIndex].personaData.name?.value?.given
            )

            assertEquals(
                "The persona[$personaIndex] family name is the same",
                expected.networks.first().personas[personaIndex].personaData.name?.value?.family,
                actual.networks.first().personas[personaIndex].personaData.name?.value?.family
            )
            assertEquals(
                "The persona[$personaIndex] nickname value is the same",
                expected.networks.first().personas[personaIndex].personaData.name?.value?.nickname,
                actual.networks.first().personas[personaIndex].personaData.name?.value?.nickname
            )

            assertEquals(
                "The persona[$personaIndex] factor source id is the same",
                (expected.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.factorSourceId,
                (actual.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.factorSourceId
            )

            val expectedTransactionSigning = (expected.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                .unsecuredEntityControl.transactionSigning
            val expectedDerivationPath = (expectedTransactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic)
                .derivationPath
            val actualTransactionSigning = (actual.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                .unsecuredEntityControl.transactionSigning
            val actualDerivationPath = (actualTransactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic)
                .derivationPath
            assertEquals(
                "The persona[$personaIndex] derivation path is the same",
                expectedDerivationPath,
                actualDerivationPath
            )

            val expectedPublicKey = (expectedTransactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic)
                .publicKey
            val actualPublicKey = (actualTransactionSigning.badge as FactorInstance.Badge.VirtualSource.HierarchicalDeterministic)
                .publicKey
            assertEquals(
                "The persona[$personaIndex] public key is the same",
                expectedPublicKey,
                actualPublicKey
            )
        }

        // Profile header
        assertEquals(
            "Profile header is the same",
            expected.header,
            actual.header
        )
    }


    private fun satoshiPersona(): PersonaData {
        return PersonaData(
            name = IdentifiedEntry.init(
                PersonaData.PersonaDataField.Name(
                    variant = PersonaData.PersonaDataField.Name.Variant.Eastern,
                    given = "Satoshi",
                    family = "Nakamoto",
                    nickname = "Creator of Bitcoin"
                ),
                "0"
            ),
            dateOfBirth = IdentifiedEntry.init(PersonaData.PersonaDataField.DateOfBirth(Instant.parse("2009-01-03T12:00:00Z")), "1"),
            companyName = IdentifiedEntry.init(PersonaData.PersonaDataField.CompanyName("Bitcoin"), "2"),
            emailAddresses = listOf(
                IdentifiedEntry.init(PersonaData.PersonaDataField.Email("satoshi@nakamoto.bitcoin"), "3"),
                IdentifiedEntry.init(PersonaData.PersonaDataField.Email("be.your@own.bank"), "4")
            ),
            phoneNumbers = listOf(
                IdentifiedEntry.init(PersonaData.PersonaDataField.PhoneNumber("21000000"), "5"),
                IdentifiedEntry.init(PersonaData.PersonaDataField.PhoneNumber("123456789"), "6")
            ),
            urls = listOf(
                IdentifiedEntry.init(PersonaData.PersonaDataField.Url("bitcoin.org"), "7"),
                IdentifiedEntry.init(PersonaData.PersonaDataField.Url("https://github.com/bitcoin-core/secp256k1"), "8"),
            ),
            postalAddresses = listOf(
                IdentifiedEntry.init(
                    PersonaData.PersonaDataField.PostalAddress(
                        listOf(
                            PersonaData.PersonaDataField.PostalAddress.Field.PostalCode("21 000 000"),
                            PersonaData.PersonaDataField.PostalAddress.Field.Prefecture("SHA256"),
                            PersonaData.PersonaDataField.PostalAddress.Field.CountySlashCity("HashTown"),
                            PersonaData.PersonaDataField.PostalAddress.Field.FurtherDivisionsLine0("Sound money street"),
                            PersonaData.PersonaDataField.PostalAddress.Field.FurtherDivisionsLine1(""),
                            PersonaData.PersonaDataField.PostalAddress.Field.CountryOrRegion(CountryOrRegion.Japan)
                        )
                    ), "9"
                ),
                IdentifiedEntry.init(
                    PersonaData.PersonaDataField.PostalAddress(
                        listOf(
                            PersonaData.PersonaDataField.PostalAddress.Field.StreetLine0("Copthall House"),
                            PersonaData.PersonaDataField.PostalAddress.Field.StreetLine1("King Street"),
                            PersonaData.PersonaDataField.PostalAddress.Field.TownSlashCity("Newcastle-under-Lyme"),
                            PersonaData.PersonaDataField.PostalAddress.Field.County("Newcastle"),
                            PersonaData.PersonaDataField.PostalAddress.Field.Postcode("ST5 1UE"),
                            PersonaData.PersonaDataField.PostalAddress.Field.CountryOrRegion(CountryOrRegion.UnitedKingdom)
                        )
                    ), "10"
                )
            ),
            creditCards = listOf(
                IdentifiedEntry.init(
                    PersonaData.PersonaDataField.CreditCard(
                        expiry = PersonaData.PersonaDataField.CreditCard.Expiry(2142, 12),
                        holder = "Satoshi Nakamoto",
                        number = "0000 0000 2100 0000",
                        cvc = 512
                    ), "11"
                )
            )
        )
    }
}

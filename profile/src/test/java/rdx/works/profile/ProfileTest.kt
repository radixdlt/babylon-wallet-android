package rdx.works.profile

import kotlinx.serialization.json.Json
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
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.Companion.initAccountWithDeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Network.Persona.Companion.init
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.model.pernetwork.addPersona
import rdx.works.profile.data.repository.createOrUpdateAuthorizedDapp
import rdx.works.profile.data.utils.getNextAccountDerivationIndex
import rdx.works.profile.data.utils.getNextIdentityDerivationIndex
import java.io.File
import java.time.Instant

class ProfileTest {

    @Test
    fun `test profile generation`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
            bip39Passphrase = ""
        )

        val profile = Profile.init(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            id = "9958f568-8c9b-476a-beeb-017d1f843266",
            deviceName = "Galaxy A53 5G (Samsung SM-A536B)",
            deviceModel = "Samsung",
            creationDate = InstantGenerator()
        )

        val defaultNetwork = Radix.Gateway.default.network
        assertEquals(profile.networks.count(), 1)
        assertEquals(profile.networks.first().networkID, defaultNetwork.id)
        assertEquals(profile.networks.first().accounts.count(), 0)
        assertEquals(profile.networks.first().personas.count(), 0)
        assertEquals(
            "Next derivation index for first account",
            0,
            (profile.factorSources.first() as DeviceFactorSource).nextDerivationIndicesPerNetwork.getNextAccountDerivationIndex(forNetworkId = defaultNetwork.networkId())
        )

        println("Profile generated $profile")

        val firstAccount = initAccountWithDeviceFactorSource(
            displayName = "first account",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = (profile.factorSources.first() as DeviceFactorSource),
            networkId = defaultNetwork.networkId(),
            appearanceID = 0
        )

        var updatedProfile = profile.addAccount(
            account = firstAccount,
            withFactorSourceId = (profile.factorSources.first() as DeviceFactorSource).id,
            onNetwork = defaultNetwork.networkId()
        )

        println("Profile updated generated $updatedProfile")
        assertEquals(updatedProfile.networks.first().accounts.count(), 1)
        assertEquals(
            "Next derivation index for second account",
            1,
            (updatedProfile.factorSources.first() as DeviceFactorSource).nextDerivationIndicesPerNetwork.getNextAccountDerivationIndex(forNetworkId = defaultNetwork.networkId()),
        )

        val firstPersona = init(
            displayName = "First",
            fields = listOf(
                Network.Persona.Field.init(
                    id = Network.Persona.Field.ID.GivenName,
                    value = "Alice"
                ),
                Network.Persona.Field.init(
                    id = Network.Persona.Field.ID.FamilyName,
                    value = "Anderson"
                )
            ),
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            factorSource = (profile.factorSources.first() as DeviceFactorSource),
            networkId = defaultNetwork.networkId()
        )

        updatedProfile = updatedProfile.addPersona(
            persona = firstPersona,
            withFactorSourceId = (profile.factorSources.first() as DeviceFactorSource).id,
            onNetwork = defaultNetwork.networkId()
        )

        assertEquals(updatedProfile.networks.first().personas.count(), 1)
        assertEquals(
            "Next derivation index for second persona",
            1,
            (updatedProfile.factorSources.first() as DeviceFactorSource).nextDerivationIndicesPerNetwork.getNextIdentityDerivationIndex(forNetworkId = defaultNetwork.networkId())
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

        val actual = Json.decodeFromString<ProfileSnapshot>(profileTestVector).toProfile()

        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman humble limb repeat video " +
                    "sudden possible story mask neutral prize goose mandate",
            bip39Passphrase = ""
        )

        val gateway = Radix.Gateway.default
        val networkId = gateway.network.networkId()

        var expected = Profile.init(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceName = "Galaxy A53 5G (Samsung SM-A536B)",
            deviceModel = "Samsung",
            id = "9958f568-8c9b-476a-beeb-017d1f843266",
            creationDate = Instant.parse("2023-03-07T10:48:21Z"),
            gateway = gateway
        )
        expected = expected.copy(factorSources = expected.factorSources + listOf(DeviceFactorSource.olympia(mnemonicWithPassphrase)))

        val firstAccount = initAccountWithDeviceFactorSource(
            displayName = "First",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = expected.babylonDeviceFactorSource,
            networkId = networkId,
            appearanceID = 0
        )
        expected = expected.addAccount(
            account = firstAccount,
            withFactorSourceId = expected.babylonDeviceFactorSource.id,
            onNetwork = networkId
        )

        val secondAccount = initAccountWithDeviceFactorSource(
            displayName = "Second",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = expected.babylonDeviceFactorSource,
            networkId = networkId,
            appearanceID = 2
        )
        expected = expected.addAccount(
            account = secondAccount,
            withFactorSourceId = expected.babylonDeviceFactorSource.id,
            onNetwork = networkId
        )

        val thirdAccount = initAccountWithDeviceFactorSource(
            displayName = "Third",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = expected.babylonDeviceFactorSource,
            networkId = networkId,
            appearanceID = 3
        )
        expected = expected.addAccount(
            account = thirdAccount,
            withFactorSourceId = expected.babylonDeviceFactorSource.id,
            onNetwork = networkId
        )

        val firstPersona = init(
            displayName = "Mrs Incognito",
            fields = listOf(
                Network.Persona.Field.init(
                    id = Network.Persona.Field.ID.GivenName,
                    value = "Jane"
                ),
                Network.Persona.Field.init(
                    id = Network.Persona.Field.ID.FamilyName,
                    value = "Incognitoson"
                )
            ),
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            factorSource = expected.babylonDeviceFactorSource,
            networkId = networkId
        )
        expected = expected.addPersona(
            persona = firstPersona,
            withFactorSourceId = expected.babylonDeviceFactorSource.id,
            onNetwork = networkId
        )

        val secondPersona = init(
            displayName = "Mrs Public",
            fields = listOf(
                Network.Persona.Field.init(
                    id = Network.Persona.Field.ID.GivenName,
                    value = "Maria"
                ),
                Network.Persona.Field.init(
                    id = Network.Persona.Field.ID.FamilyName,
                    value = "Publicson"
                )
            ),
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            factorSource = expected.babylonDeviceFactorSource,
            networkId = networkId
        )
        expected = expected.addPersona(
            persona = secondPersona,
            withFactorSourceId = expected.babylonDeviceFactorSource.id,
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

        val authorizedDapp = Network.AuthorizedDapp(
            networkID = networkId.value,
            dAppDefinitionAddress = "account_tdx_21_ygudy0at0ttc2wmsxw2ejx4dqf3dwlr9rsusk287mmynxeas5p2mk",
            displayName = "RadiSwap",
            referencesToAuthorizedPersonas = listOf(
                Network.AuthorizedDapp.AuthorizedPersonaSimple(
                    identityAddress = firstPersona.address,
                    fieldIDs = listOf(
                        Network.Persona.Field.ID.GivenName,
                        Network.Persona.Field.ID.FamilyName
                    ),
                    sharedAccounts =
                    Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        accountsReferencedByAddress = listOf(
                            secondAccount.address,
                            thirdAccount.address
                        ),
                        request = Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly,
                            2
                        )
                    ),
                    lastLogin = "some date"
                ),
                Network.AuthorizedDapp.AuthorizedPersonaSimple(
                    identityAddress = secondPersona.address,
                    fieldIDs = listOf(
                        Network.Persona.Field.ID.GivenName,
                        Network.Persona.Field.ID.FamilyName
                    ),
                    sharedAccounts =
                    Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        accountsReferencedByAddress = listOf(
                            secondAccount.address
                        ),
                        request = Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast,
                            1
                        )
                    ),
                    lastLogin = "some date"
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
            "The id of the first factor source is the same",
            expected.factorSources.first().id,
            actual.factorSources.first().id
        )

        assertEquals(
            "The next id for creating an account in this factor source",
            (expected.factorSources.first() as DeviceFactorSource).nextDerivationIndicesPerNetwork.getNextAccountDerivationIndex(networkId), //?.first()?.networkId,
            (actual.factorSources.first() as DeviceFactorSource).nextDerivationIndicesPerNetwork.getNextAccountDerivationIndex(networkId) //?.first()?.networkId,
        )

        assertEquals(
            "The next id for creating an identity in this factor source",
            (expected.factorSources.first() as DeviceFactorSource).nextDerivationIndicesPerNetwork.getNextIdentityDerivationIndex(networkId), //?.first()?.networkId, //.getNextAccountDerivationIndex(networkId),
            (actual.factorSources.first() as DeviceFactorSource).nextDerivationIndicesPerNetwork.getNextIdentityDerivationIndex(networkId) //?.first()?.networkId, //.getNextAccountDerivationIndex(networkId)
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
            expected.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().fieldIDs,
            actual.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().fieldIDs
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
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.size,
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.size
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts referenced by address first element is the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(0),
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(0)
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts referenced by address second element is the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(1),
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(1)
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
                .elementAt(1).fieldIDs,
            actual.networks.first().authorizedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).fieldIDs
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
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.size,
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.size
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona first shared account reference by address is the same",
            expected.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.elementAt(0),
            actual.networks.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.elementAt(0)
        )


        // Accounts
        assertEquals(
            "The accounts' count is the same",
            expected.networks.first().accounts.count(),
            actual.networks.first().accounts.count()
        )

        repeat(3) { accountIndex ->
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
            assertEquals(
                "The accounts[$accountIndex] derivation path are the same",
                (expected.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.derivationPath,
                (actual.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.derivationPath
            )

            assertEquals(
                "The accounts[$accountIndex] public key are the same",
                (expected.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.publicKey,
                (actual.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.publicKey
            )

            assertEquals(
                "The accounts[$accountIndex] factor source ids are the same",
                (expected.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.factorSourceId,
                (actual.networks.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.factorSourceId
            )
        }

        // Personas
        assertEquals(
            expected.networks.first().personas.count(),
            actual.networks.first().personas.count()
        )

        repeat(2) { personaIndex ->
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
                expected.networks.first().personas[personaIndex].fields[0].id,
                actual.networks.first().personas[personaIndex].fields[0].id
            )
            assertEquals(
                "The persona[$personaIndex] first field value is the same",
                expected.networks.first().personas[personaIndex].fields[0].value,
                actual.networks.first().personas[personaIndex].fields[0].value
            )

            assertEquals(
                "The persona[$personaIndex] second field kind is the same",
                expected.networks.first().personas[personaIndex].fields[1].id,
                actual.networks.first().personas[personaIndex].fields[1].id
            )
            assertEquals(
                "The persona[$personaIndex] second field value is the same",
                expected.networks.first().personas[personaIndex].fields[1].value,
                actual.networks.first().personas[personaIndex].fields[1].value
            )

            assertEquals(
                "The persona[$personaIndex] factor source id is the same",
                (expected.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.factorSourceId,
                (actual.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.factorSourceId
            )

            assertEquals(
                "The persona[$personaIndex] derivation path is the same",
                (expected.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.derivationPath,
                (actual.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.derivationPath
            )

            assertEquals(
                "The persona[$personaIndex] public key is the same",
                (expected.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.publicKey,
                (actual.networks.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.transactionSigning.publicKey
            )
        }

        // Profile header
        assertEquals(
            "Profile header is the same",
            expected.header,
            actual.header
        )
    }
}

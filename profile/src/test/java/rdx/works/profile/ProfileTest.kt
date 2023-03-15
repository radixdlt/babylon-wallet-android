package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
import io.mockk.every
import io.mockk.mockkObject
import java.io.File
import java.util.UUID
import kotlin.math.exp
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.extensions.addAccountOnNetwork
import rdx.works.profile.data.extensions.addP2PClient
import rdx.works.profile.data.extensions.createPersona
import rdx.works.profile.data.extensions.incrementFactorSourceNextAccountIndex
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.Account.Companion.createNewVirtualAccount
import rdx.works.profile.data.model.pernetwork.OnNetwork.Persona.Companion.createNewPersona
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.createOrUpdateAuthorizedDapp
import rdx.works.profile.derivation.model.NetworkId

class ProfileTest {

    @Test
    fun `test profile generation`() {
        val mnemonic = MnemonicWords(
            "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate"
        )

        val profile = Profile.init(
            mnemonic = mnemonic,
            firstAccountDisplayName = "First",
            creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)"
        )

        assertEquals(profile.onNetwork.count(), 1)
        assertEquals(profile.onNetwork.first().networkID, Network.nebunet.id)
        assertEquals(profile.onNetwork.first().accounts.count(), 1)
        assertEquals(profile.onNetwork.first().personas.count(), 0)

        println("Profile generated $profile")

        val networkId = NetworkId.Nebunet
        val factorSource = FactorSource.babylon(mnemonic = mnemonic)
        val firstAccount = createNewVirtualAccount(
            displayName = "Second",
            entityIndex = factorSource.getNextAccountDerivationIndex(networkId),
            mnemonic = mnemonic,
            factorSource = factorSource,
            networkId = networkId
        )

        var updatedProfile = profile.addAccountOnNetwork(
            account = firstAccount,
            factorSourceId = factorSource.id,
            networkID = networkId
        )

        assertEquals(updatedProfile.onNetwork.first().accounts.count(), 2)

        val firstPersona = createNewPersona(
            displayName = "First",
            fields = listOf(
                OnNetwork.Persona.Field.init(
                    id = "843A4716-D238-4D55-BF5B-1FF7EBDFF717",
                    kind = OnNetwork.Persona.Field.Kind.FirstName,
                    value = "Alice"
                ),
                OnNetwork.Persona.Field.init(
                    id = "6C62C3C8-1CD9-4049-9B2F-347486BA97B9",
                    kind = OnNetwork.Persona.Field.Kind.LastName,
                    value = "Anderson"
                )
            ),
            entityIndex = factorSource.getNextIdentityDerivationIndex(networkId),
            mnemonicWords = mnemonic,
            factorSource = factorSource,
            networkId = networkId
        )

        updatedProfile = updatedProfile.createPersona(
            persona = firstPersona,
            factorSourceId = factorSource.id,
            networkId = networkId
        )

        assertEquals(updatedProfile.onNetwork.first().personas.count(), 1)

        val p2pClient = P2PClient.init(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )
        updatedProfile = updatedProfile.addP2PClient(
            p2pClient = p2pClient
        )

        assertEquals(updatedProfile.appPreferences.p2pClients.count(), 1)

        Assert.assertTrue(profile.id.isNotBlank())
    }

    @Test
    fun `test again profile json vector`() {
        val profileTestVector = File("src/test/resources/raw/profile_snapshot.json").readText()

        val actual = Json.decodeFromString<ProfileSnapshot>(profileTestVector).toProfile()

        val mnemonic = MnemonicWords(
            "bright club bacon dinner achieve pull grid save ramp cereal blush woman humble limb repeat video " +
                    "sudden possible story mask neutral prize goose mandate"
        )

        val gateway = Gateway.nebunet
        val networkId = gateway.network.networkId()

        // Need to mock the generation of the id, so to test it against the stored vector
        println(UUIDGenerator.uuid())
        mockkObject(UUIDGenerator)
        every { UUIDGenerator.uuid() } returns UUID.fromString("9958f568-8c9b-476a-beeb-017d1f843266")


        var expected = Profile.init(
            mnemonic = mnemonic,
            firstAccountDisplayName = "First",
            creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)"
        )

        val secondAccount = createNewVirtualAccount(
            displayName = "Second",
            entityIndex = expected.factorSources.first().getNextAccountDerivationIndex(networkId),
            mnemonic = mnemonic,
            factorSource = expected.factorSources.first(),
            networkId = networkId
        )
        expected = expected.addAccountOnNetwork(
            account = secondAccount,
            factorSourceId = expected.factorSources.first().id,
            networkID = networkId
        )

        val thirdAccount = createNewVirtualAccount(
            displayName = "Third",
            entityIndex = expected.factorSources.first().getNextAccountDerivationIndex(networkId),
            mnemonic = mnemonic,
            factorSource = expected.factorSources.first(),
            networkId = networkId
        )
        expected = expected.addAccountOnNetwork(
            account = thirdAccount,
            factorSourceId = expected.factorSources.first().id,
            networkID = networkId
        )

        val firstPersona = createNewPersona(
            displayName = "Mrs Incognito",
            fields = listOf(
                OnNetwork.Persona.Field.init(
                    id = "843A4716-D238-4D55-BF5B-1FF7EBDFF717",
                    kind = OnNetwork.Persona.Field.Kind.FirstName,
                    value = "Jane"
                ),
                OnNetwork.Persona.Field.init(
                    id = "6C62C3C8-1CD9-4049-9B2F-347486BA97B9",
                    kind = OnNetwork.Persona.Field.Kind.LastName,
                    value = "Incognitoson"
                )
            ),
            entityIndex = expected.factorSources.first().getNextIdentityDerivationIndex(networkId),
            mnemonicWords = mnemonic,
            factorSource = expected.factorSources.first(),
            networkId = networkId
        )
        expected = expected.createPersona(
            persona = firstPersona,
            factorSourceId = expected.factorSources.first().id,
            networkId = networkId
        )

        val secondPersona = createNewPersona(
            displayName = "Mrs Public",
            fields = listOf(
                OnNetwork.Persona.Field.init(
                    id = "FAD199A5-D6A8-425D-8807-C1561C2425C8",
                    kind = OnNetwork.Persona.Field.Kind.FirstName,
                    value = "Maria"
                ),
                OnNetwork.Persona.Field.init(
                    id = "AC37E346-32EF-4670-9097-1AC27B20D394",
                    kind = OnNetwork.Persona.Field.Kind.LastName,
                    value = "Publicson"
                )
            ),
            entityIndex = expected.factorSources.first().getNextIdentityDerivationIndex(networkId),
            mnemonicWords = mnemonic,
            factorSource = expected.factorSources.first(),
            networkId = networkId
        )
        expected = expected.createPersona(
            persona = secondPersona,
            factorSourceId = expected.factorSources.first().id,
            networkId = networkId
        )

        val p2pClient = P2PClient.init(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )

        expected = expected.addP2PClient(
            p2pClient = p2pClient
        )

        val authorizedDapp = OnNetwork.AuthorizedDapp(
            networkID = networkId.value,
            dAppDefinitionAddress = "account_tdx_b_1qlujhx6yh6tuctgw6nl68fr2dwg3y5k7h7mc6l04zsfsg7yeqh",
            displayName = "RadiSwap",
            referencesToAuthorizedPersonas = listOf(
                OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple(
                    identityAddress = "identity_tdx_b_1pwvt6shevmzedf0709cgdq0d6axrts5gjfxaws46wdpsedwrfm",
                    fieldIDs = listOf(
                        "843A4716-D238-4D55-BF5B-1FF7EBDFF717",
                        "6C62C3C8-1CD9-4049-9B2F-347486BA97B9"
                    ),
                    sharedAccounts =
                    OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        accountsReferencedByAddress = listOf(
                            "account_tdx_b_1ppvvvxm3mpk2cja05fwhpmev0ylsznqfqhlewnrxg5gqmpswhu",
                            "account_tdx_b_1pr2q677ep9d5wxnhkkay9c6gvqln6hg3ul006w0a54tshau0z6"
                        ),
                        request = OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly,
                            2
                        )
                    ),
                    lastUsedOn = "some date"
                ),
                OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple(
                    identityAddress = "identity_tdx_b_1p0vtykvnyhqfamnk9jpnjeuaes9e7f72sekpw6ztqnkshkxgen",
                    fieldIDs = listOf(
                        "FAD199A5-D6A8-425D-8807-C1561C2425C8",
                        "AC37E346-32EF-4670-9097-1AC27B20D394"
                    ),
                    sharedAccounts =
                    OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        accountsReferencedByAddress = listOf(
                            "account_tdx_b_1ppvvvxm3mpk2cja05fwhpmev0ylsznqfqhlewnrxg5gqmpswhu"
                        ),
                        request = OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast,
                            1
                        )
                    ),
                    lastUsedOn = "some date"
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

        // P2P clients
        assertEquals(
            "P2P clients count is the same",
            expected.appPreferences.p2pClients.count(),
            actual.appPreferences.p2pClients.count()
        )
        assertEquals(
            "Connection password is the same for the first p2p client",
            expected.appPreferences.p2pClients.first().connectionPassword,
            actual.appPreferences.p2pClients.first().connectionPassword
        )
        assertEquals(
            "The display name is the same for the first p2p client",
            expected.appPreferences.p2pClients.first().displayName,
            actual.appPreferences.p2pClients.first().displayName
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

        // Per Network count
        assertEquals(
            "The networks count is the same",
            expected.onNetwork.count(),
            actual.onNetwork.count()
        )

        // Network ID
        assertEquals(
            "The first network id is the same",
            expected.onNetwork.first().networkID,
            actual.onNetwork.first().networkID
        )

        // Connected Dapp
        assertEquals(
            "Authorised dApps count is the same",
            expected.onNetwork.first().authorizedDapps.count(),
            actual.onNetwork.first().authorizedDapps.count()
        )

        assertEquals(
            "The first dApps' network id is the same",
            expected.onNetwork.first().authorizedDapps.first().networkID,
            actual.onNetwork.first().authorizedDapps.first().networkID
        )

        assertEquals(
            "The first dApps' display name is the same",
            expected.onNetwork.first().authorizedDapps.first().displayName,
            actual.onNetwork.first().authorizedDapps.first().displayName
        )

        assertEquals(
            "The first dApps' definition address is the same",
            expected.onNetwork.first().authorizedDapps.first().dAppDefinitionAddress,
            actual.onNetwork.first().authorizedDapps.first().dAppDefinitionAddress
        )

        assertEquals(
            "The first dApps' references to authorised personals size is the same",
            expected.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas.size,
            actual.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas.size
        )

        assertEquals(
            "The first dApps' references to the first authorised persona identity address is the same",
            expected.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().identityAddress,
            actual.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().identityAddress
        )

        assertEquals(
            "The first dApps' references to the first authorised persona field ids is the same",
            expected.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().fieldIDs,
            actual.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas.first().fieldIDs
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts requests is the same",
            expected.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.request,
            actual.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.request
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts referenced by address count is the same",
            expected.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.size,
            actual.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.size
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts referenced by address first element is the same",
            expected.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(0),
            actual.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(0)
        )

        assertEquals(
            "The first dApps' references to the first authorised persona shared accounts referenced by address second element is the same",
            expected.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(1),
            actual.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(1)
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona identity address is the same",
            expected.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).identityAddress,
            actual.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).identityAddress
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona field ids is the same",
            expected.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).fieldIDs,
            actual.onNetwork.first().authorizedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).fieldIDs
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona identity address is the same",
            expected.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.request,
            actual.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.request
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona shared accounts reference by address are the same",
            expected.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.size,
            actual.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.size
        )

        assertEquals(
            "The first dApps' references to the first authorised dApp first reference to authorised persona first shared account reference by address is the same",
            expected.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.elementAt(0),
            actual.onNetwork.first().authorizedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.elementAt(0)
        )


        // Accounts
        assertEquals(
            "The accounts' count is the same",
            expected.onNetwork.first().accounts.count(),
            actual.onNetwork.first().accounts.count()
        )

        repeat(3) { accountIndex ->
            assertEquals(
                "The accounts[$accountIndex] addresses are the same",
                expected.onNetwork.first().accounts[accountIndex].address,
                actual.onNetwork.first().accounts[accountIndex].address
            )

            assertEquals(
                "The accounts[$accountIndex] display name are the same",
                expected.onNetwork.first().accounts[accountIndex].displayName,
                actual.onNetwork.first().accounts[accountIndex].displayName
            )

            assertEquals(
                "The accounts[$accountIndex] derivation path are the same",
                expected.onNetwork.first().accounts[accountIndex].derivationPath,
                actual.onNetwork.first().accounts[accountIndex].derivationPath
            )

            assertEquals(
                "The accounts[$accountIndex] index are the same",
                expected.onNetwork.first().accounts[accountIndex].index,
                actual.onNetwork.first().accounts[accountIndex].index
            )

            // Security State
            assertEquals(
                "The accounts[$accountIndex] derivation path are the same",
                (expected.onNetwork.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.derivationPath,
                (actual.onNetwork.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.derivationPath
            )

            assertEquals(
                "The accounts[$accountIndex] public key are the same",
                (expected.onNetwork.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.publicKey,
                (actual.onNetwork.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.publicKey
            )

            assertEquals(
                "The accounts[$accountIndex] factor source ids are the same",
                (expected.onNetwork.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.factorSourceId,
                (actual.onNetwork.first().accounts[accountIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.factorSourceId
            )
        }

        // Personas
        assertEquals(
            expected.onNetwork.first().personas.count(),
            actual.onNetwork.first().personas.count()
        )

        repeat(2) { personaIndex ->
            assertEquals(
                "The persona[$personaIndex] index is the same",
                expected.onNetwork.first().personas[personaIndex].index,
                actual.onNetwork.first().personas[personaIndex].index
            )

            assertEquals(
                "The persona[$personaIndex] address is the same",
                expected.onNetwork.first().personas[personaIndex].address,
                actual.onNetwork.first().personas[personaIndex].address
            )

            assertEquals(
                "The persona[$personaIndex] display name is the same",
                expected.onNetwork.first().personas[personaIndex].displayName,
                actual.onNetwork.first().personas[personaIndex].displayName
            )

            assertEquals(
                "The persona[$personaIndex] first field kind is the same",
                expected.onNetwork.first().personas[personaIndex].fields[0].kind,
                actual.onNetwork.first().personas[personaIndex].fields[0].kind
            )
            assertEquals(
                "The persona[$personaIndex] first field value is the same",
                expected.onNetwork.first().personas[personaIndex].fields[0].value,
                actual.onNetwork.first().personas[personaIndex].fields[0].value
            )

            assertEquals(
                "The persona[$personaIndex] second field kind is the same",
                expected.onNetwork.first().personas[personaIndex].fields[1].kind,
                actual.onNetwork.first().personas[personaIndex].fields[1].kind
            )
            assertEquals(
                "The persona[$personaIndex] second field value is the same",
                expected.onNetwork.first().personas[personaIndex].fields[1].value,
                actual.onNetwork.first().personas[personaIndex].fields[1].value
            )

            assertEquals(
                "The persona[$personaIndex] factor source id is the same",
                (expected.onNetwork.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.factorSourceId,
                (actual.onNetwork.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.factorSourceId
            )

            assertEquals(
                "The persona[$personaIndex] derivation path is the same",
                (expected.onNetwork.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.derivationPath,
                (actual.onNetwork.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.derivationPath
            )

            assertEquals(
                "The persona[$personaIndex] public key is the same",
                (expected.onNetwork.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.publicKey,
                (actual.onNetwork.first().personas[personaIndex].securityState as SecurityState.Unsecured)
                    .unsecuredEntityControl.genesisFactorInstance.publicKey
            )
        }

        // Profile version
        assertEquals(
            "Profile version is the same",
            expected.version,
            actual.version
        )

        // Profile id
        assertEquals(
            "Profile id is the same",
            expected.id,
            actual.id
        )

        // Device name
        assertEquals(
            "Profile creating device is the same",
            expected.creatingDevice,
            actual.creatingDevice
        )
    }
}

package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import rdx.works.profile.data.extensions.addAccountOnNetwork
import rdx.works.profile.data.extensions.addP2PClient
import rdx.works.profile.data.extensions.createOrUpdatePersonaOnNetwork
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.Account.Companion.createNewVirtualAccount
import rdx.works.profile.data.model.pernetwork.OnNetwork.Persona.Companion.createNewPersona
import rdx.works.profile.data.repository.createOrUpdateConnectedDapp
import rdx.works.profile.data.utils.accountsPerNetworkCount
import rdx.works.profile.data.utils.personasPerNetworkCount
import rdx.works.profile.derivation.model.NetworkId
import java.io.File

class ProfileTest {

    @Test
    fun `test profile generation`() {
        val mnemonic = MnemonicWords(
            "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate"
        )

        val networkAndGateway = NetworkAndGateway.nebunet
        val profile = Profile.init(
            networkAndGateway = networkAndGateway,
            mnemonic = mnemonic,
            firstAccountDisplayName = "First"
        )

        Assert.assertEquals(profile.onNetwork.count(), 1)
        Assert.assertEquals(profile.onNetwork.first().networkID, Network.nebunet.id)
        Assert.assertEquals(profile.onNetwork.first().accounts.count(), 1)
        Assert.assertEquals(profile.onNetwork.first().personas.count(), 0)

        println("Profile generated $profile")

        val networkId = NetworkId.Nebunet
        val firstAccount = createNewVirtualAccount(
            displayName = "Second",
            entityIndex = profile.onNetwork.accountsPerNetworkCount(networkId),
            mnemonic = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )

        var updatedProfile = profile.addAccountOnNetwork(
            firstAccount,
            networkID = NetworkId.Nebunet
        )

        Assert.assertEquals(updatedProfile.onNetwork.first().accounts.count(), 2)

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
            entityIndex = profile.onNetwork.personasPerNetworkCount(networkId),
            mnemonicWords = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )

        updatedProfile = updatedProfile.createOrUpdatePersonaOnNetwork(
            firstPersona
        )

        Assert.assertEquals(updatedProfile.onNetwork.first().personas.count(), 1)

        val p2pClient = P2PClient.init(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )
        updatedProfile = updatedProfile.addP2PClient(
            p2pClient = p2pClient
        )

        Assert.assertEquals(updatedProfile.appPreferences.p2pClients.count(), 1)
    }

    @Test
    fun `test again profile json vector`() {
        val profileTestVector = File("src/test/resources/raw/profile_snapshot.json").readText()

        val currentProfile = Json.decodeFromString<ProfileSnapshot>(profileTestVector).toProfile()

        val mnemonic = MnemonicWords(
            "bright club bacon dinner achieve pull grid save ramp cereal blush woman humble limb repeat video " +
                    "sudden possible story mask neutral prize goose mandate"
        )

        val networkAndGateway = NetworkAndGateway.nebunet
        val networkId = networkAndGateway.network.networkId()
        var profile = Profile.init(
            networkAndGateway = networkAndGateway,
            mnemonic = mnemonic,
            firstAccountDisplayName = "First"
        )

        val secondAccount = createNewVirtualAccount(
            displayName = "Second",
            entityIndex = profile.onNetwork.accountsPerNetworkCount(networkId),
            mnemonic = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )
        profile = profile.addAccountOnNetwork(
            account = secondAccount,
            networkID = networkId
        )

        val thirdAccount = createNewVirtualAccount(
            displayName = "Third",
            entityIndex = profile.onNetwork.accountsPerNetworkCount(networkId),
            mnemonic = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )
        profile = profile.addAccountOnNetwork(
            account = thirdAccount,
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
            entityIndex = profile.onNetwork.personasPerNetworkCount(networkId),
            mnemonicWords = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )
        profile = profile.createOrUpdatePersonaOnNetwork(
            persona = firstPersona
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
            entityIndex = profile.onNetwork.personasPerNetworkCount(networkId),
            mnemonicWords = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )
        profile = profile.createOrUpdatePersonaOnNetwork(
            persona = secondPersona
        )

        val p2pClient = P2PClient.init(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )

        profile = profile.addP2PClient(
            p2pClient = p2pClient
        )

        val connectedDapp = OnNetwork.ConnectedDapp(
            networkID = networkId.value,
            dAppDefinitionAddress = "account_tdx_b_1qlujhx6yh6tuctgw6nl68fr2dwg3y5k7h7mc6l04zsfsg7yeqh",
            displayName = "RadiSwap",
            referencesToAuthorizedPersonas = listOf(
                OnNetwork.ConnectedDapp.AuthorizedPersonaSimple(
                    identityAddress = "identity_tdx_b_1pwvt6shevmzedf0709cgdq0d6axrts5gjfxaws46wdpsedwrfm",
                    fieldIDs = listOf(
                        "843A4716-D238-4D55-BF5B-1FF7EBDFF717",
                        "6C62C3C8-1CD9-4049-9B2F-347486BA97B9"
                    ),
                    sharedAccounts =
                    OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        accountsReferencedByAddress = listOf(
                            "account_tdx_b_1ppvvvxm3mpk2cja05fwhpmev0ylsznqfqhlewnrxg5gqmpswhu",
                            "account_tdx_b_1pr2q677ep9d5wxnhkkay9c6gvqln6hg3ul006w0a54tshau0z6"
                        ),
                        request = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly,
                            2
                        )
                    ),
                    lastUsedOn = "some date"
                ),
                OnNetwork.ConnectedDapp.AuthorizedPersonaSimple(
                    identityAddress = "identity_tdx_b_1p0vtykvnyhqfamnk9jpnjeuaes9e7f72sekpw6ztqnkshkxgen",
                    fieldIDs = listOf(
                        "FAD199A5-D6A8-425D-8807-C1561C2425C8",
                        "AC37E346-32EF-4670-9097-1AC27B20D394"
                    ),
                    sharedAccounts =
                    OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        accountsReferencedByAddress = listOf(
                            "account_tdx_b_1ppvvvxm3mpk2cja05fwhpmev0ylsznqfqhlewnrxg5gqmpswhu"
                        ),
                        request = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast,
                            1
                        )
                    ),
                    lastUsedOn = "some date"
                )
            )
        )
        profile = profile.createOrUpdateConnectedDapp(
            unverifiedConnectedDapp = connectedDapp
        )

        // Network and gateway
        Assert.assertEquals(profile.appPreferences.networkAndGateway, currentProfile.appPreferences.networkAndGateway)

        // Display
        Assert.assertEquals(
            profile.appPreferences.display.fiatCurrencyPriceTarget,
            currentProfile.appPreferences.display.fiatCurrencyPriceTarget
        )

        // P2P clients
        Assert.assertEquals(
            profile.appPreferences.p2pClients.count(),
            currentProfile.appPreferences.p2pClients.count()
        )
        Assert.assertEquals(
            profile.appPreferences.p2pClients.first().connectionPassword,
            currentProfile.appPreferences.p2pClients.first().connectionPassword
        )
        Assert.assertEquals(
            profile.appPreferences.p2pClients.first().displayName,
            currentProfile.appPreferences.p2pClients.first().displayName
        )

        // Factor Sources
        Assert.assertEquals(
            profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.count(),
            currentProfile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.count()
        )

        Assert.assertEquals(
            profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.first().factorSourceID,
            currentProfile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.first().factorSourceID
        )

        // Per Network count
        Assert.assertEquals(profile.onNetwork.count(), currentProfile.onNetwork.count())

        // Network ID
        Assert.assertEquals(profile.onNetwork.first().networkID, currentProfile.onNetwork.first().networkID)

        // Connected Dapp
        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.count(),
            currentProfile.onNetwork.first().connectedDapps.count()
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().networkID,
            currentProfile.onNetwork.first().connectedDapps.first().networkID
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().displayName,
            currentProfile.onNetwork.first().connectedDapps.first().displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().dAppDefinitionAddress,
            currentProfile.onNetwork.first().connectedDapps.first().dAppDefinitionAddress
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.size,
            currentProfile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.size
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first().identityAddress,
            currentProfile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first().identityAddress
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first().fieldIDs,
            currentProfile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first().fieldIDs
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.request,
            currentProfile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.request
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.size,
            currentProfile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.size
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(0),
            currentProfile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(0)
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(1),
            currentProfile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.first().sharedAccounts.accountsReferencedByAddress.elementAt(1)
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).identityAddress,
            currentProfile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).identityAddress
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).fieldIDs,
            currentProfile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas
                .elementAt(1).fieldIDs
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.request,
            currentProfile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.request
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.size,
            currentProfile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.size
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.elementAt(0),
            currentProfile.onNetwork.first().connectedDapps.first()
                .referencesToAuthorizedPersonas.elementAt(1).sharedAccounts.accountsReferencedByAddress.elementAt(0)
        )


        // ///// Accounts
        Assert.assertEquals(
            profile.onNetwork.first().accounts.count(),
            currentProfile.onNetwork.first().accounts.count()
        )

        // 1st
        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().address,
            currentProfile.onNetwork.first().accounts.first().address
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().displayName,
            currentProfile.onNetwork.first().accounts.first().displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().derivationPath,
            currentProfile.onNetwork.first().accounts.first().derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().index,
            currentProfile.onNetwork.first().accounts.first().index
        )

        // Security State
        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().securityState.discriminator,
            currentProfile.onNetwork.first().accounts.first().securityState.discriminator
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            currentProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            currentProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            currentProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            currentProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID,
            currentProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID
        )

        // 2nd
        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].address,
            currentProfile.onNetwork.first().accounts[1].address
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].displayName,
            currentProfile.onNetwork.first().accounts[1].displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].derivationPath,
            currentProfile.onNetwork.first().accounts[1].derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].index,
            currentProfile.onNetwork.first().accounts[1].index
        )

        // Security State
        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].securityState.discriminator,
            currentProfile.onNetwork.first().accounts[1].securityState.discriminator
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            currentProfile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            currentProfile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            currentProfile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            currentProfile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind
        )

        // 3rd
        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].address,
            currentProfile.onNetwork.first().accounts[2].address
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].displayName,
            currentProfile.onNetwork.first().accounts[2].displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].derivationPath,
            currentProfile.onNetwork.first().accounts[2].derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].index,
            currentProfile.onNetwork.first().accounts[2].index
        )

        // Security State
        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].securityState.discriminator,
            currentProfile.onNetwork.first().accounts[2].securityState.discriminator
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            currentProfile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            currentProfile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            currentProfile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            currentProfile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind
        )

        // ///// Personas
        Assert.assertEquals(
            profile.onNetwork.first().personas.count(),
            currentProfile.onNetwork.first().personas.count()
        )

        // 1st
        Assert.assertEquals(
            profile.onNetwork.first().personas.first().index,
            currentProfile.onNetwork.first().personas.first().index
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().address,
            currentProfile.onNetwork.first().personas.first().address
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().derivationPath,
            currentProfile.onNetwork.first().personas.first().derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().displayName,
            currentProfile.onNetwork.first().personas.first().displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().fields[0].kind,
            currentProfile.onNetwork.first().personas.first().fields[0].kind
        )
        Assert.assertEquals(
            profile.onNetwork.first().personas.first().fields[0].value,
            currentProfile.onNetwork.first().personas.first().fields[0].value
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().fields[1].kind,
            currentProfile.onNetwork.first().personas.first().fields[1].kind
        )
        Assert.assertEquals(
            profile.onNetwork.first().personas.first().fields[1].value,
            currentProfile.onNetwork.first().personas.first().fields[1].value
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().securityState.discriminator,
            currentProfile.onNetwork.first().personas.first().securityState.discriminator
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            currentProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            currentProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            currentProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            currentProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID,
            currentProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID
        )

        // 2nd
        Assert.assertEquals(
            profile.onNetwork.first().personas[1].index,
            currentProfile.onNetwork.first().personas[1].index
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1].address,
            currentProfile.onNetwork.first().personas[1].address
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1].derivationPath,
            currentProfile.onNetwork.first().personas[1].derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1].displayName,
            currentProfile.onNetwork.first().personas[1].displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1].fields[0].kind,
            currentProfile.onNetwork.first().personas[1].fields[0].kind
        )
        Assert.assertEquals(
            profile.onNetwork.first().personas[1].fields[0].value,
            currentProfile.onNetwork.first().personas[1].fields[0].value
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1].fields[1].kind,
            currentProfile.onNetwork.first().personas[1].fields[1].kind
        )
        Assert.assertEquals(
            profile.onNetwork.first().personas[1].fields[1].value,
            currentProfile.onNetwork.first().personas[1].fields[1].value
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1].securityState.discriminator,
            currentProfile.onNetwork.first().personas[1].securityState.discriminator
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            currentProfile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            currentProfile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            currentProfile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            currentProfile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID,
            currentProfile.onNetwork.first().personas[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID
        )

        // Profile version
        Assert.assertEquals(profile.version, currentProfile.version)
    }
}

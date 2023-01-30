package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import rdx.works.profile.data.extensions.addAccountOnNetwork
import rdx.works.profile.data.extensions.addConnectedDapp
import rdx.works.profile.data.extensions.addP2PClient
import rdx.works.profile.data.extensions.addPersonaOnNetwork
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.Account.Companion.createNewVirtualAccount
import rdx.works.profile.data.model.pernetwork.OnNetwork.Persona.Companion.createNewPersona
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

        val networkAndGateway = NetworkAndGateway.hammunet
        val profile = Profile.init(
            networkAndGateway = networkAndGateway,
            mnemonic = mnemonic,
            firstAccountDisplayName = "First"
        )

        Assert.assertEquals(profile.onNetwork.count(), 1)
        Assert.assertEquals(profile.onNetwork.first().networkID, Network.hammunet.id)
        Assert.assertEquals(profile.onNetwork.first().accounts.count(), 1)
        Assert.assertEquals(profile.onNetwork.first().personas.count(), 0)

        println("Profile generated $profile")

        val networkId = NetworkId.Hammunet
        val firstAccount = createNewVirtualAccount(
            displayName = "Second",
            entityIndex = profile.onNetwork.accountsPerNetworkCount(networkId),
            mnemonic = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )

        var updatedProfile = profile.addAccountOnNetwork(
            firstAccount,
            networkID = NetworkId.Hammunet
        )

        Assert.assertEquals(updatedProfile.onNetwork.first().accounts.count(), 2)

        val firstPersona = createNewPersona(
            displayName = "First",
            fields = listOf(
                OnNetwork.Persona.Field.init(OnNetwork.Persona.Field.FieldKind.FirstName, "Alice"),
                OnNetwork.Persona.Field.init(OnNetwork.Persona.Field.FieldKind.LastName, "Anderson")
            ),
            entityIndex = profile.onNetwork.personasPerNetworkCount(networkId),
            mnemonicWords = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )

        updatedProfile = updatedProfile.addPersonaOnNetwork(
            firstPersona,
            networkID = NetworkId.Hammunet
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
        val hammunetProfileTestVector = File("src/test/resources/raw/profile_snapshot.json").readText()

        val hammunetProfile = Json.decodeFromString<ProfileSnapshot>(hammunetProfileTestVector).toProfile()

        val mnemonic = MnemonicWords(
            "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate"
        )

        val networkAndGateway = NetworkAndGateway.hammunet
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
                OnNetwork.Persona.Field.init(OnNetwork.Persona.Field.FieldKind.FirstName, "Jane"),
                OnNetwork.Persona.Field.init(OnNetwork.Persona.Field.FieldKind.LastName, "Incognitoson")
            ),
            entityIndex = profile.onNetwork.personasPerNetworkCount(networkId),
            mnemonicWords = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )
        profile = profile.addPersonaOnNetwork(
            persona = firstPersona,
            networkID = networkId
        )

        val secondPersona = createNewPersona(
            displayName = "Mrs Public",
            fields = listOf(
                OnNetwork.Persona.Field.init(OnNetwork.Persona.Field.FieldKind.FirstName, "Maria"),
                OnNetwork.Persona.Field.init(OnNetwork.Persona.Field.FieldKind.LastName, "Publicson")
            ),
            entityIndex = profile.onNetwork.personasPerNetworkCount(networkId),
            mnemonicWords = mnemonic,
            factorSources = profile.factorSources,
            networkId = networkId
        )
        profile = profile.addPersonaOnNetwork(
            persona = secondPersona,
            networkID = networkId
        )

        val p2pClient = P2PClient.init(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )

        profile = profile.addP2PClient(
            p2pClient = p2pClient
        )

        profile = profile.addConnectedDapp(
            dAppDefinitionAddress = "account_tdx_a_1qd5svul20u30qnq408zhj2tw5evqrunq48eg0jsjf9qsx5t8qu",
            dAppDisplayName = "My Mac DApp",
            networkId = networkId.value,
            referencesToAuthorizedPersona = setOf(
                OnNetwork.ConnectedDapp.AuthorizedPersonaSimple(
                    identityAddress = "account_tdx_22_1qa803nvt5fqp89wg7lrayga3r727s5fsgxcdanrqmjvs98ghge",
                    fieldIDs = setOf("1", "2"),
                    referencesToAuthorizedAccounts = setOf(
                        OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                            "account_tdx_22_1qa803nvt5fqp89wg7lrayga3r727s5fsgxcdanrqmjvs98ghge"
                        )
                    )
                )
            )
        )

        // Network and gateway
        Assert.assertEquals(profile.appPreferences.networkAndGateway, hammunetProfile.appPreferences.networkAndGateway)

        // Display
        Assert.assertEquals(
            profile.appPreferences.display.fiatCurrencyPriceTarget,
            hammunetProfile.appPreferences.display.fiatCurrencyPriceTarget
        )

        // P2P clients
        Assert.assertEquals(
            profile.appPreferences.p2pClients.count(),
            hammunetProfile.appPreferences.p2pClients.count()
        )
        Assert.assertEquals(
            profile.appPreferences.p2pClients.first().connectionPassword,
            hammunetProfile.appPreferences.p2pClients.first().connectionPassword
        )
        Assert.assertEquals(
            profile.appPreferences.p2pClients.first().displayName,
            hammunetProfile.appPreferences.p2pClients.first().displayName
        )

        // Factor Sources
        Assert.assertEquals(
            profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.count(),
            hammunetProfile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.count()
        )

        Assert.assertEquals(
            profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.first().factorSourceID,
            hammunetProfile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.first().factorSourceID
        )

        // Per Network count
        Assert.assertEquals(profile.onNetwork.count(), hammunetProfile.onNetwork.count())

        // Network ID
        Assert.assertEquals(profile.onNetwork.first().networkID, hammunetProfile.onNetwork.first().networkID)

        // Connected Dapp
        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.count(),
            hammunetProfile.onNetwork.first().connectedDapps.count()
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().networkID,
            hammunetProfile.onNetwork.first().connectedDapps.first().networkID
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().displayName,
            hammunetProfile.onNetwork.first().connectedDapps.first().displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().address,
            hammunetProfile.onNetwork.first().connectedDapps.first().address
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.size,
            hammunetProfile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.size
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first().identityAddress,
            hammunetProfile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first().identityAddress
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first().fieldIDs,
            hammunetProfile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first().fieldIDs
        )

        Assert.assertEquals(
            profile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first()
                .referencesToAuthorizedAccounts.first(),
            hammunetProfile.onNetwork.first().connectedDapps.first().referencesToAuthorizedPersonas.first()
                .referencesToAuthorizedAccounts.first()
        )


        // ///// Accounts
        Assert.assertEquals(
            profile.onNetwork.first().accounts.count(),
            hammunetProfile.onNetwork.first().accounts.count()
        )

        // 1st
        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().entityAddress.address,
            hammunetProfile.onNetwork.first().accounts.first().entityAddress.address
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().displayName,
            hammunetProfile.onNetwork.first().accounts.first().displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().derivationPath,
            hammunetProfile.onNetwork.first().accounts.first().derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().index,
            hammunetProfile.onNetwork.first().accounts.first().index
        )

        // Security State
        Assert.assertEquals(
            profile.onNetwork.first().accounts.first().securityState.discriminator,
            hammunetProfile.onNetwork.first().accounts.first().securityState.discriminator
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            hammunetProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            hammunetProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            hammunetProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            hammunetProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID,
            hammunetProfile.onNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID
        )

        // 2nd
        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].entityAddress.address,
            hammunetProfile.onNetwork.first().accounts[1].entityAddress.address
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].displayName,
            hammunetProfile.onNetwork.first().accounts[1].displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].derivationPath,
            hammunetProfile.onNetwork.first().accounts[1].derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].index,
            hammunetProfile.onNetwork.first().accounts[1].index
        )

        // Security State
        Assert.assertEquals(
            profile.onNetwork.first().accounts[1].securityState.discriminator,
            hammunetProfile.onNetwork.first().accounts[1].securityState.discriminator
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            hammunetProfile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            hammunetProfile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            hammunetProfile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            hammunetProfile.onNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind
        )

        // 3rd
        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].entityAddress.address,
            hammunetProfile.onNetwork.first().accounts[2].entityAddress.address
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].displayName,
            hammunetProfile.onNetwork.first().accounts[2].displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].derivationPath,
            hammunetProfile.onNetwork.first().accounts[2].derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].index,
            hammunetProfile.onNetwork.first().accounts[2].index
        )

        // Security State
        Assert.assertEquals(
            profile.onNetwork.first().accounts[2].securityState.discriminator,
            hammunetProfile.onNetwork.first().accounts[2].securityState.discriminator
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            hammunetProfile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            hammunetProfile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            hammunetProfile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID
        )

        Assert.assertEquals(
            profile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            hammunetProfile.onNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind
        )

        // ///// Personas
        Assert.assertEquals(
            profile.onNetwork.first().personas.count(),
            hammunetProfile.onNetwork.first().personas.count()
        )

        // 1st
        Assert.assertEquals(
            profile.onNetwork.first().personas.first().index,
            hammunetProfile.onNetwork.first().personas.first().index
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().entityAddress.address,
            hammunetProfile.onNetwork.first().personas.first().entityAddress.address
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().derivationPath,
            hammunetProfile.onNetwork.first().personas.first().derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().displayName,
            hammunetProfile.onNetwork.first().personas.first().displayName
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().fields[0].kind,
            hammunetProfile.onNetwork.first().personas.first().fields[0].kind
        )
        Assert.assertEquals(
            profile.onNetwork.first().personas.first().fields[0].value,
            hammunetProfile.onNetwork.first().personas.first().fields[0].value
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().fields[1].kind,
            hammunetProfile.onNetwork.first().personas.first().fields[1].kind
        )
        Assert.assertEquals(
            profile.onNetwork.first().personas.first().fields[1].value,
            hammunetProfile.onNetwork.first().personas.first().fields[1].value
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first().securityState.discriminator,
            hammunetProfile.onNetwork.first().personas.first().securityState.discriminator
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            hammunetProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            hammunetProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            hammunetProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            hammunetProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind
        )

        Assert.assertEquals(
            profile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID,
            hammunetProfile.onNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID
        )

        // Profile version
        Assert.assertEquals(profile.version, hammunetProfile.version)
    }
}

package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import rdx.works.profile.data.AccountDerivationPath
import rdx.works.profile.data.AccountIndex
import rdx.works.profile.data.CompressedPublicKey
import rdx.works.profile.data.IdentityDerivationPath
import rdx.works.profile.data.PersonaIndex
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.UnsecuredSecurityState
import rdx.works.profile.data.addAccountOnNetwork
import rdx.works.profile.data.addP2PClient
import rdx.works.profile.data.addPersonaOnNetwork
import rdx.works.profile.data.createNewPersona
import rdx.works.profile.data.createNewVirtualAccount
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.enginetoolkit.EngineToolkitImpl
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.pernetwork.PersonaField
import java.io.File

class ProfileTest {

    @Test
    fun `test profile generation`() {
        val mnemonic = MnemonicWords("bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate")

        val networkAndGateway = NetworkAndGateway.hammunet
        val profile = Profile.init(
            networkAndGateway = networkAndGateway,
            mnemonic = mnemonic,
            firstAccountDisplayName = "First"
        )

        Assert.assertEquals(profile.perNetwork.count(), 1)
        Assert.assertEquals(profile.perNetwork.first().networkID, Network.hammunet.id)
        Assert.assertEquals(profile.perNetwork.first().accounts.count(), 1)
        Assert.assertEquals(profile.perNetwork.first().personas.count(), 0)

        println("Profile generated $profile")

        val engineToolkit = EngineToolkitImpl()

        val networkId = NetworkId.Hammunet
        val firstAccount = createNewVirtualAccount(
            displayName = "Second",
            engineToolkit = engineToolkit,
            entityDerivationPath = AccountDerivationPath(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            entityIndex = AccountIndex(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            derivePublicKey = CompressedPublicKey(
                mnemonic = mnemonic
            ),
            createSecurityState = UnsecuredSecurityState(
                factorSources = profile.factorSources
            )
        )

        var updatedProfile = profile.addAccountOnNetwork(
            firstAccount,
            networkID = NetworkId.Hammunet
        )

        Assert.assertEquals(updatedProfile.perNetwork.first().accounts.count(), 2)

        val firstPersona = createNewPersona(
            displayName = "First",
            fields = listOf(
                PersonaField.init("firstName", "Alice"),
                PersonaField.init("lastName", "Anderson")
            ),
            engineToolkit = engineToolkit,
            entityDerivationPath = IdentityDerivationPath(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            entityIndex = PersonaIndex(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            derivePublicKey = CompressedPublicKey(
                mnemonic = mnemonic
            ),
            createSecurityState = UnsecuredSecurityState(
                factorSources = profile.factorSources
            )
        )

        updatedProfile = updatedProfile.addPersonaOnNetwork(
            firstPersona,
            networkID = NetworkId.Hammunet
        )

        Assert.assertEquals(updatedProfile.perNetwork.first().personas.count(), 1)

        updatedProfile = updatedProfile.addP2PClient(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )

        Assert.assertEquals(updatedProfile.appPreferences.p2pClients.count(), 1)

        val updatedProfileString = Json.encodeToString(updatedProfile)

        println("Updated profile $updatedProfileString")
    }

    @Test
    fun testVector() {
        val hammunetProfileTestVector = File("src/test/resources/raw/profile_snapshot_hammunet.json").readText()

        val hammunetProfile = Json.decodeFromString<Profile>(hammunetProfileTestVector)

        val mnemonic = MnemonicWords("bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate")

        val networkAndGateway = NetworkAndGateway.hammunet
        val networkId = networkAndGateway.network.networkId()
        var profile = Profile.init(
            networkAndGateway = networkAndGateway,
            mnemonic = mnemonic,
            firstAccountDisplayName = "First"
        )

        val engineToolkit = EngineToolkitImpl()

        val secondAccount = createNewVirtualAccount(
            displayName = "Second",
            engineToolkit = engineToolkit,
            entityDerivationPath = AccountDerivationPath(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            entityIndex = AccountIndex(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            derivePublicKey = CompressedPublicKey(
                mnemonic = mnemonic
            ),
            createSecurityState = UnsecuredSecurityState(
                factorSources = profile.factorSources
            )
        )
        profile = profile.addAccountOnNetwork(
            account = secondAccount,
            networkID = networkId
        )

        val thirdAccount = createNewVirtualAccount(
            displayName = "Third",
            engineToolkit = engineToolkit,
            entityDerivationPath = AccountDerivationPath(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            entityIndex = AccountIndex(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            derivePublicKey = CompressedPublicKey(
                mnemonic = mnemonic
            ),
            createSecurityState = UnsecuredSecurityState(
                factorSources = profile.factorSources
            )
        )
        profile = profile.addAccountOnNetwork(
            account = thirdAccount,
            networkID = networkId
        )


        val firstPersona = createNewPersona(
            displayName = "Mrs Incognito",
            fields = listOf(
                PersonaField.init("firstName", "Jane"),
                PersonaField.init("lastName", "Incognitoson")
            ),
            engineToolkit = engineToolkit,
            entityDerivationPath = IdentityDerivationPath(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            entityIndex = PersonaIndex(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            derivePublicKey = CompressedPublicKey(
                mnemonic = mnemonic
            ),
            createSecurityState = UnsecuredSecurityState(
                factorSources = profile.factorSources
            )
        )
        profile = profile.addPersonaOnNetwork(
            persona = firstPersona,
            networkID = networkId
        )

        val secondPersona = createNewPersona(
            displayName = "Mrs Public",
            fields = listOf(
                PersonaField.init("firstName", "Maria"),
                PersonaField.init("lastName", "Publicson")
            ),
            engineToolkit = engineToolkit,
            entityDerivationPath = IdentityDerivationPath(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            entityIndex = PersonaIndex(
                perNetwork = profile.perNetwork,
                networkId = networkId
            ),
            derivePublicKey = CompressedPublicKey(
                mnemonic = mnemonic
            ),
            createSecurityState = UnsecuredSecurityState(
                factorSources = profile.factorSources
            )
        )
        profile = profile.addPersonaOnNetwork(
            persona = secondPersona,
            networkID = networkId
        )

        profile = profile.addP2PClient(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )

        // Network and gateway
        Assert.assertEquals(profile.appPreferences.networkAndGateway, hammunetProfile.appPreferences.networkAndGateway)

        // Display
        Assert.assertEquals(profile.appPreferences.display.fiatCurrencyPriceTarget,
            hammunetProfile.appPreferences.display.fiatCurrencyPriceTarget)

        // P2P clients
        Assert.assertEquals(profile.appPreferences.p2pClients.count(), hammunetProfile.appPreferences.p2pClients.count())
        Assert.assertEquals(profile.appPreferences.p2pClients.first().connectionPassword,
            hammunetProfile.appPreferences.p2pClients.first().connectionPassword)
        Assert.assertEquals(profile.appPreferences.p2pClients.first().displayName,
            hammunetProfile.appPreferences.p2pClients.first().displayName)

        // Factor Sources
        Assert.assertEquals(
            profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.count(),
            hammunetProfile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.count())

        //TODO
//        Assert.assertEquals(
//            profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.first().factorSourceID,
//            hammunetProfile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.first().factorSourceID)


        // Per Network count
        Assert.assertEquals(profile.perNetwork.count(), hammunetProfile.perNetwork.count())

        // Network ID
        Assert.assertEquals(profile.perNetwork.first().networkID, hammunetProfile.perNetwork.first().networkID)

        // Connected Dapps
        Assert.assertEquals(profile.perNetwork.first().connectedDapps.count(),
            hammunetProfile.perNetwork.first().connectedDapps.count())


        /////// Accounts
        Assert.assertEquals(profile.perNetwork.first().accounts.count(),
            hammunetProfile.perNetwork.first().accounts.count())

        // 1st
        //TODO derive addresses from engine toolkit and test properly
//        Assert.assertEquals(profile.perNetwork.first().accounts.first().address.address,
//            hammunetProfile.perNetwork.first().accounts.first().address.address)

        Assert.assertEquals(profile.perNetwork.first().accounts.first().displayName,
            hammunetProfile.perNetwork.first().accounts.first().displayName)

        Assert.assertEquals(profile.perNetwork.first().accounts.first().derivationPath,
            hammunetProfile.perNetwork.first().accounts.first().derivationPath)

        Assert.assertEquals(profile.perNetwork.first().accounts.first().index,
            hammunetProfile.perNetwork.first().accounts.first().index)

        // Security State
        Assert.assertEquals(profile.perNetwork.first().accounts.first().securityState.discriminator,
            hammunetProfile.perNetwork.first().accounts.first().securityState.discriminator)

        Assert.assertEquals(profile.perNetwork.first().accounts.first()
            .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            hammunetProfile.perNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath)

        Assert.assertEquals(profile.perNetwork.first().accounts.first()
            .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            hammunetProfile.perNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey)

        Assert.assertEquals(profile.perNetwork.first().accounts.first()
            .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            hammunetProfile.perNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID)

        Assert.assertEquals(profile.perNetwork.first().accounts.first()
            .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            hammunetProfile.perNetwork.first().accounts.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind)

        //TODO
//        Assert.assertEquals(profile.perNetwork.first().accounts.first()
//            .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID,
//            hammunetProfile.perNetwork.first().accounts.first()
//                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID)

        //2nd
        Assert.assertEquals(profile.perNetwork.first().accounts[1].displayName,
            hammunetProfile.perNetwork.first().accounts[1].displayName)

        Assert.assertEquals(profile.perNetwork.first().accounts[1].derivationPath,
            hammunetProfile.perNetwork.first().accounts[1].derivationPath)

        Assert.assertEquals(profile.perNetwork.first().accounts[1].index,
            hammunetProfile.perNetwork.first().accounts[1].index)

        // Security State
        Assert.assertEquals(profile.perNetwork.first().accounts[1].securityState.discriminator,
            hammunetProfile.perNetwork.first().accounts[1].securityState.discriminator)

        Assert.assertEquals(profile.perNetwork.first().accounts[1]
            .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            hammunetProfile.perNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath)

        Assert.assertEquals(profile.perNetwork.first().accounts[1]
            .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            hammunetProfile.perNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey)

        Assert.assertEquals(profile.perNetwork.first().accounts[1]
            .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            hammunetProfile.perNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID)

        Assert.assertEquals(profile.perNetwork.first().accounts[1]
            .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            hammunetProfile.perNetwork.first().accounts[1]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind)

        //3rd
        Assert.assertEquals(profile.perNetwork.first().accounts[2].displayName,
            hammunetProfile.perNetwork.first().accounts[2].displayName)

        Assert.assertEquals(profile.perNetwork.first().accounts[2].derivationPath,
            hammunetProfile.perNetwork.first().accounts[2].derivationPath)

        Assert.assertEquals(profile.perNetwork.first().accounts[2].index,
            hammunetProfile.perNetwork.first().accounts[2].index)

        // Security State
        Assert.assertEquals(profile.perNetwork.first().accounts[2].securityState.discriminator,
            hammunetProfile.perNetwork.first().accounts[2].securityState.discriminator)

        Assert.assertEquals(profile.perNetwork.first().accounts[2]
            .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            hammunetProfile.perNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath)

        Assert.assertEquals(profile.perNetwork.first().accounts[2]
            .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            hammunetProfile.perNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey)

        Assert.assertEquals(profile.perNetwork.first().accounts[2]
            .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            hammunetProfile.perNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID)

        Assert.assertEquals(profile.perNetwork.first().accounts[2]
            .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            hammunetProfile.perNetwork.first().accounts[2]
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind)


        /////// Personas
        Assert.assertEquals(profile.perNetwork.first().personas.count(),
            hammunetProfile.perNetwork.first().personas.count())

        // 1st
        Assert.assertEquals(profile.perNetwork.first().personas.first().index,
            hammunetProfile.perNetwork.first().personas.first().index)

        //TODO derive addresses from engine toolkit and test properly
//        Assert.assertEquals(profile.perNetwork.first().personas.first().address,
//            hammunetProfile.perNetwork.first().personas.first().address)

        Assert.assertEquals(profile.perNetwork.first().personas.first().derivationPath,
            hammunetProfile.perNetwork.first().personas.first().derivationPath)

        Assert.assertEquals(profile.perNetwork.first().personas.first().displayName,
            hammunetProfile.perNetwork.first().personas.first().displayName)

        Assert.assertEquals(profile.perNetwork.first().personas.first().fields[0].kind,
            hammunetProfile.perNetwork.first().personas.first().fields[0].kind)
        Assert.assertEquals(profile.perNetwork.first().personas.first().fields[0].value,
            hammunetProfile.perNetwork.first().personas.first().fields[0].value)

        Assert.assertEquals(profile.perNetwork.first().personas.first().fields[1].kind,
            hammunetProfile.perNetwork.first().personas.first().fields[1].kind)
        Assert.assertEquals(profile.perNetwork.first().personas.first().fields[1].value,
            hammunetProfile.perNetwork.first().personas.first().fields[1].value)

        Assert.assertEquals(profile.perNetwork.first().personas.first().securityState.discriminator,
            hammunetProfile.perNetwork.first().personas.first().securityState.discriminator)

        Assert.assertEquals(profile.perNetwork.first().personas.first()
            .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID,
            hammunetProfile.perNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorInstanceID)

        Assert.assertEquals(profile.perNetwork.first().personas.first()
            .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath,
            hammunetProfile.perNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.derivationPath)

        Assert.assertEquals(profile.perNetwork.first().personas.first()
            .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey,
            hammunetProfile.perNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.publicKey)

        Assert.assertEquals(profile.perNetwork.first().personas.first()
            .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind,
            hammunetProfile.perNetwork.first().personas.first()
                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceKind)

        //TODO
//        Assert.assertEquals(profile.perNetwork.first().personas.first()
//            .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID,
//            hammunetProfile.perNetwork.first().personas.first()
//                .securityState.unsecuredEntityControl.genesisFactorInstance.factorSourceReference.factorSourceID)


        // Profile version
        Assert.assertEquals(profile.version, hammunetProfile.version)
    }
}
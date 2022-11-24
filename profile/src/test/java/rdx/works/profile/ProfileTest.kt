package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.enginetoolkit.EngineToolkitImpl
import rdx.works.profile.model.apppreferences.Network
import rdx.works.profile.model.apppreferences.NetworkAndGateway
import rdx.works.profile.model.pernetwork.PersonaField

class ProfileTest {

    @Test
    fun `test profile generation`() {
        val mnemonic = MnemonicWords("bright club bacon dinner achieve pull grid save ramp cereal blush woman humble limb repeat video" +
                " sudden possible story mask neutral prize goose mandate")

        val networkAndGateway = NetworkAndGateway.primary
        val profile = Profile.init(
            networkAndGateway = networkAndGateway,
            mnemonic = mnemonic,
            firstAccountDisplayName = "First"
        )

        Assert.assertEquals(profile.perNetwork.count(), 1)
        Assert.assertEquals(profile.perNetwork.first().networkID, Network.adapanet.id)
        Assert.assertEquals(profile.perNetwork.first().accounts.count(), 1)
        Assert.assertEquals(profile.perNetwork.first().personas.count(), 0)

        println("Profile generated $profile")

        val engineToolkit = EngineToolkitImpl()

        val networkId = NetworkId.Adapanet
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
            networkID = NetworkId.Adapanet
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
            networkID = NetworkId.Adapanet
        )

        Assert.assertEquals(updatedProfile.perNetwork.first().personas.count(), 1)

        updatedProfile = updatedProfile.addP2PClient(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio"
        )

        Assert.assertEquals(updatedProfile.appPreferences.p2pClients.connections.count(), 1)

        val updatedProfileString = Json.encodeToString(updatedProfile)

        println("Updated profile $updatedProfileString")
    }
}
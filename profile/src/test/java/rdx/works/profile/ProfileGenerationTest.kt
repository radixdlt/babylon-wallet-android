package rdx.works.profile

import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.addP2PLink
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Network.Account.Companion.initAccountWithDeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Network.Persona.Companion.init
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.model.pernetwork.addPersona
import rdx.works.profile.data.model.pernetwork.nextAccountIndex
import rdx.works.profile.data.model.pernetwork.nextPersonaIndex
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.TestData

class ProfileGenerationTest {

    @Test
    fun `test profile generation`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
            bip39Passphrase = ""
        )
        val babylonFactorSource = DeviceFactorSource.babylon(
            mnemonicWithPassphrase, model = TestData.deviceInfo.displayName,
            name = "Samsung"
        )
        val mnemonicRepository = mockk<MnemonicRepository>()
        coEvery { mnemonicRepository() } returns mnemonicWithPassphrase

        val profile = Profile.init(
            id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
            deviceInfo = TestData.deviceInfo,
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
}

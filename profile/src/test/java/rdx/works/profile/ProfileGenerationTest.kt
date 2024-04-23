package rdx.works.profile

import com.radixdlt.extensions.removeLeadingZero
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.core.InstantGenerator
import rdx.works.core.identifiedArrayListOf
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.P2PLinkPurpose
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.extensions.addP2PLink
import rdx.works.profile.data.model.extensions.initializeAccount
import rdx.works.profile.data.model.extensions.nextAccountIndex
import rdx.works.profile.data.model.extensions.renameAccountDisplayName
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Persona.Companion.init
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.model.pernetwork.addPersona
import rdx.works.profile.data.model.pernetwork.nextPersonaIndex
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.derivation.model.KeyType
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

        var profile = Profile.init(
            id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
            deviceInfo = TestData.deviceInfo,
            creationDate = InstantGenerator()
        ).copy(factorSources = identifiedArrayListOf(babylonFactorSource))

        val defaultNetwork = Radix.Gateway.default.network
        assertEquals(profile.networks.count(), 0)
        assertEquals(
            "Next derivation index for first account",
            0,
            profile.nextAccountIndex(
                factorSource = babylonFactorSource,
                derivationPathScheme = DerivationPathScheme.CAP_26,
                forNetworkId = defaultNetwork.networkId(),
            )
        )

        println("Profile generated $profile")

        val derivationPath = DerivationPath.forAccount(
            networkId = defaultNetwork.networkId(),
            accountIndex = 0,
            keyType = KeyType.TRANSACTION_SIGNING
        )
        val firstAccount = initializeAccount(
            displayName = "first account",
            onNetworkId = defaultNetwork.networkId(),
            compressedPublicKey = mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath).removeLeadingZero(),
            derivationPath = derivationPath,
            factorSource = (profile.factorSources.first() as DeviceFactorSource),
            onLedgerSettings = Network.Account.OnLedgerSettings.init()
        )

        profile = profile.addAccounts(
            accounts = listOf(firstAccount),
            onNetwork = defaultNetwork.networkId()
        )

        assertEquals(profile.networks.count(), 1)
        assertEquals(profile.networks.first().networkID, defaultNetwork.id)
        assertEquals(profile.networks.first().accounts.count(), 1)
        assertEquals(profile.networks.first().personas.count(), 0)

        println("Profile updated generated $profile")
        assertEquals(profile.networks.first().accounts.count(), 1)
        assertEquals(
            "Next derivation index for second account",
            1,
            profile.nextAccountIndex(
                factorSource = babylonFactorSource,
                derivationPathScheme = DerivationPathScheme.CAP_26,
                forNetworkId = defaultNetwork.networkId()
            )
        )

        val firstPersona = init(
            entityIndex = 0,
            displayName = "First",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            factorSource = (profile.factorSources.first() as DeviceFactorSource),
            networkId = defaultNetwork.networkId()
        )

        profile = profile.addPersona(
            persona = firstPersona,
            onNetwork = defaultNetwork.networkId()
        )

        assertEquals(profile.networks.first().personas.count(), 1)
        assertEquals(
            "Next derivation index for second persona",
            1,
            profile.nextPersonaIndex(
                derivationPathScheme = DerivationPathScheme.CAP_26,
                forNetworkId = defaultNetwork.networkId(),
                factorSourceID = babylonFactorSource.id
            )
        )

        val p2pLink = P2PLink.init(
            connectionPassword = "deadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeafdeadbeeffadedeaf",
            displayName = "Brave browser on Mac Studio",
            publicKey = "PublicKey key test",
            purpose = P2PLinkPurpose.General
        )
        profile = profile.addP2PLink(
            p2pLink = p2pLink
        )

        assertEquals(1, profile.appPreferences.p2pLinks.count())

        Assert.assertTrue(profile.header.id.isNotBlank())

        val newDisplayNameForAccount = "Test account"
        profile = profile.renameAccountDisplayName(
            accountToRename = firstAccount,
            newDisplayName = newDisplayNameForAccount
        )
        assertEquals(profile.networks.first().accounts.count(), 1)
        assertEquals(profile.networks.first().accounts[0].displayName, newDisplayNameForAccount)
    }

    @Test
    fun `test adding duplicate factor sources to profile`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
            bip39Passphrase = ""
        )
        val babylonFactorSource1 = DeviceFactorSource.babylon(
            mnemonicWithPassphrase, model = TestData.deviceInfo.displayName,
            name = "Samsung"
        )
        val babylonFactorSource2 = DeviceFactorSource.babylon(
            mnemonicWithPassphrase, model = TestData.deviceInfo.displayName,
            name = "Samsung2"
        )
        val mnemonicRepository = mockk<MnemonicRepository>()
        coEvery { mnemonicRepository() } returns mnemonicWithPassphrase

        val profile = Profile.init(
            id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
            deviceInfo = TestData.deviceInfo,
            creationDate = InstantGenerator()
        ).copy(factorSources = identifiedArrayListOf(babylonFactorSource1))
        val updatedProfile = profile.copy(factorSources = (profile.factorSources + babylonFactorSource2).toIdentifiedArrayList())
        assertEquals(1, updatedProfile.factorSources.count())
    }

    @Test
    fun `test adding duplicate account to Profile`() {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
            bip39Passphrase = ""
        )
        val babylonFactorSource1 = DeviceFactorSource.babylon(
            mnemonicWithPassphrase, model = TestData.deviceInfo.displayName,
            name = "Samsung"
        )
        val mnemonicRepository = mockk<MnemonicRepository>()
        coEvery { mnemonicRepository() } returns mnemonicWithPassphrase

        val profile = Profile.init(
            id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
            deviceInfo = TestData.deviceInfo,
            creationDate = InstantGenerator()
        ).copy(factorSources = identifiedArrayListOf(babylonFactorSource1))
        val derivationPath = DerivationPath.forAccount(
            networkId = Radix.Gateway.default.network.networkId(),
            accountIndex = 0,
            keyType = KeyType.TRANSACTION_SIGNING
        )
        val firstAccount = initializeAccount(
            displayName = "first account",
            onNetworkId = Radix.Gateway.default.network.networkId(),
            compressedPublicKey = mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath).removeLeadingZero(),
            derivationPath = derivationPath,
            factorSource = (profile.factorSources.first() as DeviceFactorSource),
            onLedgerSettings = Network.Account.OnLedgerSettings.init()
        )
        val updatedProfile = profile.copy(factorSources = (profile.factorSources).toIdentifiedArrayList()).addAccounts(
            accounts = listOf(firstAccount),
            onNetwork = Radix.Gateway.default.network.networkId()
        ).addAccounts(
            accounts = listOf(firstAccount),
            onNetwork = Radix.Gateway.default.network.networkId()
        )
        assertEquals(1, updatedProfile.networks.first().accounts.count())
    }
}

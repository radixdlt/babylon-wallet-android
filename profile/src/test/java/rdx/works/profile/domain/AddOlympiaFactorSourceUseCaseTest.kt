package rdx.works.profile.domain

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.Header
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Security
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.KeyType

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddOlympiaFactorSourceUseCaseTest {

    private val profileRepository = mockk<ProfileRepository>()
    private val mnemonicRepository = mockk<MnemonicRepository>()

    @Test
    fun `new factor source is added to a profile, if it does not already exist`() = runTest {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "noodle question hungry sail type offer grocery clay nation hello mixture forum",
            bip39Passphrase = ""
        )
        val olympiaMnemonic = MnemonicWithPassphrase(
            mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
            bip39Passphrase = ""
        )

        val network = Radix.Gateway.hammunet
        val profile = Profile(
            header = Header.init(
                id = "9958f568-8c9b-476a-beeb-017d1f843266",
                deviceName = "Galaxy A53 5G (Samsung SM-A536B)",
                creationDate = InstantGenerator(),
                numberOfNetworks = 1,
                numberOfAccounts = 1
            ),
            appPreferences = AppPreferences(
                display = Display.default,
                security = Security.default,
                gateways = Gateways(network.url, listOf(network)),
                p2pLinks = listOf(
                    P2PLink.init(
                        connectionPassword = "My password",
                        displayName = "Browser name test"
                    )
                )
            ),
            factorSources = listOf(DeviceFactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase)),
            networks = listOf(
                Network(
                    accounts = listOf(
                        Network.Account(
                            address = "fj3489fj348f",
                            appearanceID = 123,
                            displayName = "my account",
                            networkID = network.network.networkId().value,
                            securityState = SecurityState.Unsecured(
                                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                    entityIndex = 0,
                                    transactionSigning = FactorInstance(
                                        derivationPath = DerivationPath.forAccount(
                                            networkId = network.network.networkId(),
                                            accountIndex = 0,
                                            keyType = KeyType.TRANSACTION_SIGNING
                                        ),
                                        factorSourceId = FactorSource.FactorSourceID.FromHash(
                                            kind = FactorSourceKind.DEVICE,
                                            body = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5")
                                        ),
                                        publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                    )
                                )
                            )
                        )
                    ),
                    authorizedDapps = emptyList(),
                    networkID = network.network.networkId().value,
                    personas = emptyList()
                )
            )
        )

        coEvery { mnemonicRepository.readMnemonic(any()) } returns null
        coEvery { mnemonicRepository.saveMnemonic(any(), any()) } just Runs
        coEvery { profileRepository.profileState } returns flowOf(ProfileState.Restored(profile))
        coEvery { profileRepository.saveProfile(any()) } just Runs

        val usecase = AddOlympiaFactorSourceUseCase(profileRepository, mnemonicRepository)
        val capturedProfile = slot<Profile>()
        usecase(olympiaMnemonic)
        coVerify(exactly = 1) { profileRepository.saveProfile(capture(capturedProfile)) }
        assert(capturedProfile.captured.factorSources.size == 2)

        coEvery { mnemonicRepository.readMnemonic(any()) } returns olympiaMnemonic
        usecase(olympiaMnemonic)
        coVerify(exactly = 1) { mnemonicRepository.saveMnemonic(any(), any()) }
        coVerify(exactly = 1) { profileRepository.saveProfile(any()) }
    }
}

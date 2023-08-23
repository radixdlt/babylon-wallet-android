package rdx.works.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.addPersona
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.domain.persona.CreatePersonaWithDeviceFactorSourceUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePersonaWithDeviceFactorSourceUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `given profile already exists, when creating new persona, verify its returned and persisted to the profile`() {
        // given
        val personaName = "First persona"
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = "noodle question hungry sail type offer grocery clay nation hello mixture forum",
            bip39Passphrase = ""
        )
        val network = Radix.Gateway.hammunet
        testScope.runTest {
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
                factorSources = listOf(
                    DeviceFactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase)
                ),
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
                                ),
                                onLedgerSettings = Network.Account.OnLedgerSettings.init()
                            )
                        ),
                        authorizedDapps = emptyList(),
                        networkID = network.network.networkId().value,
                        personas = emptyList()
                    )
                )
            )

            val mnemonicRepository = mock<MnemonicRepository> {
                onBlocking {
                    invoke(profile.babylonDeviceFactorSource.id)
                } doReturn mnemonicWithPassphrase
            }

            val profileRepository = Mockito.mock(ProfileRepository::class.java)
            whenever(profileRepository.profileState).thenReturn(flowOf(ProfileState.Restored(profile)))

            val createPersonaWithDeviceFactorSourceUseCase = CreatePersonaWithDeviceFactorSourceUseCase(mnemonicRepository, profileRepository, testDispatcher)

            val newPersona = createPersonaWithDeviceFactorSourceUseCase(
                displayName = personaName,
                personaData = PersonaData()
            )

            val updatedProfile = profile.addPersona(
                persona = newPersona,
                onNetwork = network.network.networkId()
            )

            verify(profileRepository).saveProfile(updatedProfile)
        }
    }
}

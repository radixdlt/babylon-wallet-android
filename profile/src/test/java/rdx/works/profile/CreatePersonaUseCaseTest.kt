package rdx.works.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import rdx.works.profile.data.extensions.addPersona
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.domain.CreatePersonaUseCase
import rdx.works.profile.domain.GetMnemonicUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePersonaUseCaseTest {

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
        val personaFields = listOf(
            OnNetwork.Persona.Field(
                id = "ID213",
                kind = OnNetwork.Persona.Field.Kind.FirstName,
                value = "Emily"
            ),
            OnNetwork.Persona.Field(
                id = "ID0921",
                kind = OnNetwork.Persona.Field.Kind.LastName,
                value = "Jacobs"
            )
        )
        val network = Gateway.hammunet
        testScope.runTest {
            val profile = Profile(
                id = "9958f568-8c9b-476a-beeb-017d1f843266",
                creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)",
                appPreferences = AppPreferences(
                    display = Display.default,
                    gateways = Gateways(network.url, listOf(network)),
                    p2pLinks = listOf(
                        P2PLink.init(
                            connectionPassword = "My password",
                            displayName = "Browser name test"
                        )
                    )
                ),
                factorSources = listOf(
                    FactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase)
                ),
                onNetwork = listOf(
                    OnNetwork(
                        accounts = listOf(
                            OnNetwork.Account(
                                address = "fj3489fj348f",
                                appearanceID = 123,
                                displayName = "my account",
                                networkID = network.network.networkId().value,
                                securityState = SecurityState.Unsecured(
                                    unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                        genesisFactorInstance = FactorInstance(
                                            derivationPath = DerivationPath.forAccount("m/1'/1'/1'/1'/1'/1'"),
                                            factorSourceId = FactorSource.ID("IDIDDIIDD"),
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
                ),
                version = 1
            )

            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking {
                    invoke(profile.babylonDeviceFactorSource.id)
                } doReturn mnemonicWithPassphrase
            }

            val profileDataSource = Mockito.mock(ProfileDataSource::class.java)
            whenever(profileDataSource.readProfile()).thenReturn(profile)

            val createPersonaUseCase = CreatePersonaUseCase(getMnemonicUseCase, profileDataSource, testDispatcher)

            val newPersona = createPersonaUseCase(
                displayName = personaName,
                fields = personaFields
            )

            val updatedProfile = profile.addPersona(
                persona = newPersona,
                withFactorSourceId = profile.babylonDeviceFactorSource.id,
                onNetwork = network.network.networkId()
            )

            verify(profileDataSource).saveProfile(updatedProfile)
        }
    }
}

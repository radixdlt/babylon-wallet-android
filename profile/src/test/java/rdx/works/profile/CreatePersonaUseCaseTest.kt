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
import rdx.works.profile.data.extensions.addPersonaOnNetwork
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.EntityAddress
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.FactorSourceReference
import rdx.works.profile.data.model.pernetwork.PerNetwork
import rdx.works.profile.data.model.pernetwork.PersonaField
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.derivation.model.NetworkId
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
        val personaFields = listOf(
            PersonaField(
                id = "ID213",
                kind = PersonaField.PersonaFieldKind.FirstName,
                value = "Emily"
            ),
            PersonaField(
                id = "ID0921",
                kind = PersonaField.PersonaFieldKind.LastName,
                value = "Jacobs"
            )
        )

        testScope.runTest {
            val profile = Profile(
                appPreferences = AppPreferences(
                    display = Display.default,
                    NetworkAndGateway.hammunet,
                    p2pClients = listOf(
                        P2PClient.init(
                            connectionPassword = "My password",
                            displayName = "Browser name test"
                        )
                    )
                ),
                factorSources = FactorSources(
                    curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources = listOf(
                        FactorSources.Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource(
                            creationDate = "Date",
                            factorSourceID = "XXX111222333",
                            label = "Label"
                        )
                    ),
                    secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources = emptyList()
                ),
                perNetwork = listOf(
                    PerNetwork(
                        accounts = listOf(
                            Account(
                                entityAddress = EntityAddress("fj3489fj348f"),
                                appearanceID = 123,
                                derivationPath = "m/1'/1'/1'/1'/1'/1'",
                                displayName = "my account",
                                index = 0,
                                securityState = SecurityState.Unsecured(
                                    discriminator = "dsics",
                                    unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                        genesisFactorInstance = FactorInstance(
                                            derivationPath = DerivationPath("few", "disc"),
                                            factorInstanceID = "IDIDDIIDD",
                                            factorSourceReference = FactorSourceReference(
                                                factorSourceID = "f32f3",
                                                factorSourceKind = "kind"
                                            ),
                                            initializationDate = "Date1",
                                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                        )
                                    )
                                )
                            )
                        ),
                        connectedDapps = emptyList(),
                        networkID = 999,
                        personas = emptyList()
                    )
                ),
                version = "9.9.9"
            )

            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking {
                    invoke(
                        profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources.first().factorSourceID
                    )
                } doReturn "noodle question hungry sail type offer grocery clay nation hello mixture forum"
            }

            val profileDataSource = Mockito.mock(ProfileDataSource::class.java)
            whenever(profileDataSource.readProfile()).thenReturn(profile)

            val createPersonaUseCase = CreatePersonaUseCase(getMnemonicUseCase, profileDataSource, testDispatcher)

            val newPersona = createPersonaUseCase(
                displayName = personaName,
                fields = personaFields
            )

            val updatedProfile = profile.addPersonaOnNetwork(
                newPersona,
                networkID = NetworkId.Hammunet
            )

            verify(profileDataSource).saveProfile(updatedProfile)
        }
    }
}

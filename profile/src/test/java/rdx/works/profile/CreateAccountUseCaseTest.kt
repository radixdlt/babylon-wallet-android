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
import rdx.works.profile.data.extensions.addAccountOnNetwork
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
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.CreateAccountUseCase
import rdx.works.profile.domain.GetMnemonicUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `given profile already exists, when creating new account, verify its returned and persisted to the profile`() {
        testScope.runTest {
            // given
            val accountName = "First account"
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

            val profileRepository = Mockito.mock(ProfileRepository::class.java)
            whenever(profileRepository.readProfileSnapshot()).thenReturn(profile.snapshot())
            whenever(profileRepository.getCurrentNetworkId()).thenReturn(NetworkId.Hammunet)

            val createAccountUseCase = CreateAccountUseCase(getMnemonicUseCase, profileRepository, testDispatcher)

            val account = createAccountUseCase(
                displayName = accountName
            )

            val updatedProfile = profile.addAccountOnNetwork(
                account,
                networkID = NetworkId.Hammunet
            )

            verify(profileRepository).saveProfileSnapshot(updatedProfile.snapshot())
        }
    }
}

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
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.ProfileDataSource
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
            val phrase = "noodle question hungry sail type offer grocery clay nation hello mixture forum"
            val accountName = "First account"
            val profile = Profile(
                id = "9958f568-8c9b-476a-beeb-017d1f843266",
                creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)",
                appPreferences = AppPreferences(
                    display = Display.default,
                    Gateways(Gateway.hammunet.url, listOf(Gateway.hammunet)),
                    p2pClients = listOf(
                        P2PClient.init(
                            connectionPassword = "My password",
                            displayName = "Browser name test"
                        )
                    )
                ),
                factorSources = factorSources(fromPhrase = phrase),
                onNetwork = listOf(
                    OnNetwork(
                        accounts = listOf(
                            OnNetwork.Account(
                                address = "fj3489fj348f",
                                appearanceID = 123,
                                displayName = "my account",
                                index = 0,
                                networkID = 999,
                                securityState = SecurityState.Unsecured(
                                    unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
                                        genesisFactorInstance = FactorInstance(
                                            derivationPath = DerivationPath.accountDerivationPath("m/1'/1'/1'/1'/1'/1'"),
                                            factorSourceId = "IDIDDIIDD",
                                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
                                        )
                                    )
                                )
                            )
                        ),
                        authorizedDapps = emptyList(),
                        networkID = 999,
                        personas = emptyList()
                    )
                ),
                version = 1
            )

            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking {
                    invoke(profile.factorSources.first().id)
                } doReturn phrase
            }

            val profileDataSource = Mockito.mock(ProfileDataSource::class.java)
            whenever(profileDataSource.readProfile()).thenReturn(profile)
            whenever(profileDataSource.getCurrentNetworkId()).thenReturn(NetworkId.Hammunet)

            val createAccountUseCase = CreateAccountUseCase(
                generateMnemonicUseCase = getMnemonicUseCase,
                profileDataSource = profileDataSource,
                testDispatcher
            )

            val account = createAccountUseCase(
                displayName = accountName
            )

            val updatedProfile = profile.addAccountOnNetwork(
                account,
                networkID = NetworkId.Hammunet
            )

            verify(profileDataSource).saveProfile(updatedProfile)
        }
    }
}

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
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.domain.account.CreateAccountUseCase
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `given profile already exists, when creating new account, verify its returned and persisted to the profile`() {
        testScope.runTest {
            // given
            val mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = "noodle question hungry sail type offer grocery clay nation hello mixture forum",
                bip39Passphrase = ""
            )
            val accountName = "First account"
            val network = Radix.Gateway.hammunet
            val profile = Profile(
                header = Header.init(
                    id = "9958f568-8c9b-476a-beeb-017d1f843266",
                    creatingDevice = "Galaxy A53 5G (Samsung SM-A536B)",
                    creationDate = Instant.now()
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
                factorSources = listOf(FactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase)),
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
                                        genesisFactorInstance = FactorInstance(
                                            derivationPath = DerivationPath.forAccount(
                                                networkId = network.network.networkId(),
                                                accountIndex = 0,
                                                keyType = KeyType.TRANSACTION_SIGNING
                                            ),
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
                )
            )

            val mnemonicRepository = mock<MnemonicRepository> {
                onBlocking {
                    invoke(profile.babylonDeviceFactorSource.id)
                } doReturn mnemonicWithPassphrase
            }

            val profileRepository = Mockito.mock(ProfileRepository::class.java)
            whenever(profileRepository.profileState).thenReturn(flowOf(ProfileState.Restored(profile)))

            val createAccountUseCase = CreateAccountUseCase(
                mnemonicRepository = mnemonicRepository,
                profileRepository = profileRepository,
                testDispatcher
            )

            val account = createAccountUseCase(
                displayName = accountName
            )

            val updatedProfile = profile.addAccount(
                account = account,
                withFactorSourceId = profile.babylonDeviceFactorSource.id,
                onNetwork = network.network.networkId()
            )

            verify(profileRepository).saveProfile(updatedProfile)
        }
    }
}

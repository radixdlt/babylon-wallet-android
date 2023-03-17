package rdx.works.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.DeviceInfo
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetMnemonicUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateProfileUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeDeviceInfoRepository = FakeDeviceInfoRepository()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `given profile already exists, when generate profile called, return existing profile`() {
        testScope.runTest {
            // given
            val mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
                bip39Passphrase = ""
            )
            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking { invoke() } doReturn mnemonicWithPassphrase
            }

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
                                networkID = 999,
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
                        networkID = 999,
                        personas = emptyList()
                    )
                ),
                version = 1
            )
            val profileDataSource = Mockito.mock(ProfileDataSource::class.java)
            whenever(profileDataSource.readProfile()).thenReturn(profile)

            // when
            val generateProfileUseCase = GenerateProfileUseCase(
                getMnemonicUseCase = getMnemonicUseCase,
                profileDataSource = profileDataSource,
                deviceInfoRepository = fakeDeviceInfoRepository,
                defaultDispatcher = testDispatcher
            )

            // then
            Assert.assertEquals(generateProfileUseCase("main"), profile)
        }
    }

    @Test
    fun `given profile does not exist, when generating one, verify correct data generated from mnemonic`() {
        testScope.runTest {
            val mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
                bip39Passphrase = ""
            )
            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking { invoke() } doReturn mnemonicWithPassphrase
            }

            val expectedFactorSourceId = FactorSource.factorSourceId(mnemonicWithPassphrase)
            val profileDataSource = Mockito.mock(ProfileDataSource::class.java)
            val generateProfileUseCase = GenerateProfileUseCase(
                getMnemonicUseCase = getMnemonicUseCase,
                profileDataSource = profileDataSource,
                deviceInfoRepository = fakeDeviceInfoRepository,
                defaultDispatcher = testDispatcher
            )

            whenever(profileDataSource.readProfile()).thenReturn(null)

            val profile = generateProfileUseCase("main")

            Assert.assertEquals(
                "Factor Source ID",
                expectedFactorSourceId,
                profile.babylonDeviceFactorSource.id
            )

            Assert.assertEquals(
                "Account's Factor Source ID",
                expectedFactorSourceId,
                (profile.onNetwork.first().accounts.first().securityState as SecurityState.Unsecured).unsecuredEntityControl
                    .genesisFactorInstance.factorSourceId
            )
        }
    }

    @Test
    fun `given profile does not exist, when generating one, verify correct data generated from other mnemonic`() {
        testScope.runTest {
            val mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = "noodle question hungry sail type offer grocery clay nation hello mixture forum",
                bip39Passphrase = ""
            )
            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking { invoke() } doReturn mnemonicWithPassphrase
            }

            val expectedFactorSourceId = FactorSource.factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase)
            val profileDataSource = Mockito.mock(ProfileDataSource::class.java)
            val generateProfileUseCase = GenerateProfileUseCase(
                getMnemonicUseCase = getMnemonicUseCase,
                profileDataSource = profileDataSource,
                deviceInfoRepository = fakeDeviceInfoRepository,
                defaultDispatcher = testDispatcher
            )

            whenever(profileDataSource.readProfile()).thenReturn(null)

            val profile = generateProfileUseCase("main")

            Assert.assertEquals(
                "Factor Source ID",
                expectedFactorSourceId,
                profile.babylonDeviceFactorSource.id
            )

            Assert.assertEquals(
                "Account's Factor Source ID",
                expectedFactorSourceId,
                (profile.onNetwork.first().accounts.first().securityState as SecurityState.Unsecured).unsecuredEntityControl
                    .genesisFactorInstance.factorSourceId
            )
        }
    }

    private class FakeDeviceInfoRepository: DeviceInfoRepository {
        override fun getDeviceInfo(): DeviceInfo = DeviceInfo(
            name = "Galaxy A53 5G",
            manufacturer = "samsung",
            model = "SM-A536B"
        )

    }
}

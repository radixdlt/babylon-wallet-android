package rdx.works.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.EntityAddress
import rdx.works.profile.data.model.pernetwork.FactorSourceReference
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.PerNetwork
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetMnemonicUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateProfileUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `given profile already exists, when generate profile called, return existing profile`() {

        testScope.runTest {
            // given
            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking { invoke() } doReturn "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                        "humble limb repeat video sudden possible story mask neutral prize goose mandate"
            }

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
            val profileRepository = Mockito.mock(ProfileRepository::class.java)
            whenever(profileRepository.readProfileSnapshot()).thenReturn(profile.snapshot())

            // when
            val generateProfileUseCase = GenerateProfileUseCase(getMnemonicUseCase, profileRepository, testDispatcher)

            // then
            Assert.assertEquals(generateProfileUseCase("main"), profile)
        }
    }

    @Test
    fun `given profile does not exist, when generating one, verify correct data generated from mnemonic`() {

        testScope.runTest {
            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking { invoke() } doReturn "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                        "humble limb repeat video sudden possible story mask neutral prize goose mandate"
            }
            val profileRepository = Mockito.mock(ProfileRepository::class.java)
            val generateProfileUseCase = GenerateProfileUseCase(getMnemonicUseCase, profileRepository, testDispatcher)

            whenever(profileRepository.readProfileSnapshot()).thenReturn(null)

            val profile = generateProfileUseCase("main")

            val factorSourceId = "4d8b07d0220a9b838b7626dc917b96512abc629bd912a66f60c942fc5fa2f287"
            val factorInstanceId = "873692e7b1cb8d2efa20633eedbeb9dab389cfb9ed1258b12ff4fd74dde05f02"

            Assert.assertEquals(
                profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                    .first().factorSourceID, factorSourceId
            )

            Assert.assertEquals(
                profile.perNetwork.first().accounts.first().securityState.unsecuredEntityControl
                    .genesisFactorInstance.factorInstanceID, factorInstanceId
            )
        }
    }

    @Test
    fun `given profile does not exist, when generating one, verify correct data generated from other mnemonic`() {

        testScope.runTest {

            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking { invoke() } doReturn "noodle question hungry sail type offer grocery clay nation hello mixture forum"
            }
            val profileRepository = Mockito.mock(ProfileRepository::class.java)
            val generateProfileUseCase = GenerateProfileUseCase(getMnemonicUseCase, profileRepository, testDispatcher)

            whenever(profileRepository.readProfileSnapshot()).thenReturn(null)

            val profile = generateProfileUseCase("main")

            val factorSourceId = "6e1a2745f14f9326a49d2daf8e83087920e6980630f6fc635dd566c21201934d"
            val factorInstanceId = "3d49eb91ba3d5c56c5a13cb00da3561fa8ba68182b1c0a662f8fffbcf2eddc1e"

            Assert.assertEquals(
                profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                    .first().factorSourceID, factorSourceId
            )

            Assert.assertEquals(
                profile.perNetwork.first().accounts.first().securityState.unsecuredEntityControl
                    .genesisFactorInstance.factorInstanceID, factorInstanceId
            )
        }
    }
}

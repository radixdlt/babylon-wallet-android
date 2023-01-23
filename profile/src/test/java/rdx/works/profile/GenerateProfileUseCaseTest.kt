package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
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
import rdx.works.profile.data.extensions.compressedPublicKey
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
import rdx.works.profile.data.repository.AccountDerivationPath
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.utils.hashToFactorId
import rdx.works.profile.derivation.model.NetworkId
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
                                networkID = 999,
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
            val profileDataSource = Mockito.mock(ProfileDataSource::class.java)
            whenever(profileDataSource.readProfile()).thenReturn(profile)

            // when
            val generateProfileUseCase = GenerateProfileUseCase(getMnemonicUseCase, profileDataSource, testDispatcher)

            // then
            Assert.assertEquals(generateProfileUseCase("main"), profile)
        }
    }

    @Test
    fun `given profile does not exist, when generating one, verify correct data generated from mnemonic`() {
        testScope.runTest {
            val mnemonicPhrase = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate"
            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking { invoke() } doReturn mnemonicPhrase
            }

            val expectedFactorSourceId = generateFactorSourceId(mnemonicPhrase)
            val expectedFactorInstanceId = generateInstanceId(
                mnemonicPhrase,
                NetworkAndGateway.betanet.network.networkId()
            )

            val profileDataSource = Mockito.mock(ProfileDataSource::class.java)
            val generateProfileUseCase = GenerateProfileUseCase(getMnemonicUseCase, profileDataSource, testDispatcher)

            whenever(profileDataSource.readProfile()).thenReturn(null)

            val profile = generateProfileUseCase("main")

            Assert.assertEquals(
                profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                    .first().factorSourceID,
                expectedFactorSourceId
            )

            Assert.assertEquals(
                profile.perNetwork.first().accounts.first().securityState.unsecuredEntityControl
                    .genesisFactorInstance.factorInstanceID,
                expectedFactorInstanceId
            )
        }
    }

    @Test
    fun `given profile does not exist, when generating one, verify correct data generated from other mnemonic`() {
        testScope.runTest {
            val mnemonicPhrase = "noodle question hungry sail type offer grocery clay nation hello mixture forum"
            val getMnemonicUseCase = mock<GetMnemonicUseCase> {
                onBlocking { invoke() } doReturn mnemonicPhrase
            }
            val networkAndGateway = NetworkAndGateway.betanet

            val expectedFactorSourceId = generateFactorSourceId(mnemonicPhrase)
            val expectedFactorInstanceId = generateInstanceId(mnemonicPhrase, networkAndGateway.network.networkId())

            val profileDataSource = Mockito.mock(ProfileDataSource::class.java)
            val generateProfileUseCase = GenerateProfileUseCase(getMnemonicUseCase, profileDataSource, testDispatcher)

            whenever(profileDataSource.readProfile()).thenReturn(null)

            val profile = generateProfileUseCase("main")

            Assert.assertEquals(
                profile.factorSources.curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                    .first().factorSourceID,
                expectedFactorSourceId
            )

            Assert.assertEquals(
                profile.perNetwork.first().accounts.first().securityState.unsecuredEntityControl
                    .genesisFactorInstance.factorInstanceID,
                expectedFactorInstanceId
            )
        }
    }

    private fun generateFactorSourceId(mnemonicPhrase: String): String {
        val mnemonicWords = MnemonicWords(mnemonicPhrase)
        return FactorSources.Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource.deviceFactorSource(
            mnemonicWords,
            label = "main"
        ).factorSourceID
    }

    private fun generateInstanceId(mnemonicPhrase: String, networkId: NetworkId): String {
        return MnemonicWords(mnemonicPhrase)
            .compressedPublicKey(
                derivationPath = AccountDerivationPath(
                    entityIndex = 0,
                    networkId = networkId
                ).path()
            ).hashToFactorId()
    }
}

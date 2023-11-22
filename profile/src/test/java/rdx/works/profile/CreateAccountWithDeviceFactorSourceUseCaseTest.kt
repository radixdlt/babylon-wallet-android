package rdx.works.profile

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.TestData
import rdx.works.profile.domain.account.CreateAccountWithDeviceFactorSourceUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountWithDeviceFactorSourceUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val ensureBabylonFactorSourceExistUseCase = mockk<EnsureBabylonFactorSourceExistUseCase>()
    private val mnemonicWithPassphrase = MnemonicWithPassphrase(
        mnemonic = "noodle question hungry sail type offer grocery clay nation hello mixture forum",
        bip39Passphrase = ""
    )

    @Before
    fun setUp() {
        coEvery { ensureBabylonFactorSourceExistUseCase() } returns TestData.testProfile2Networks2AccountsEach(mnemonicWithPassphrase)
    }

    @Test
    fun `given profile already exists, when creating new account, verify its returned and persisted to the profile`() {
        testScope.runTest {
            // given
            val accountName = "First account"
            val network = Radix.Gateway.hammunet
            val profile = TestData.testProfile2Networks2AccountsEach(mnemonicWithPassphrase)

            val mnemonicRepository = mock<MnemonicRepository> {
                onBlocking {
                    readMnemonic(profile.babylonMainDeviceFactorSource.id)
                } doReturn Result.success(mnemonicWithPassphrase)
            }

            val profileRepository = Mockito.mock(ProfileRepository::class.java)
            whenever(profileRepository.profileState).thenReturn(flowOf(ProfileState.Restored(profile)))
            coEvery { ensureBabylonFactorSourceExistUseCase() } returns profile

            val createAccountWithDeviceFactorSourceUseCase = CreateAccountWithDeviceFactorSourceUseCase(
                mnemonicRepository = mnemonicRepository,
                profileRepository = profileRepository,
                ensureBabylonFactorSourceExistUseCase = ensureBabylonFactorSourceExistUseCase,
                defaultDispatcher = testDispatcher
            )

            val account = createAccountWithDeviceFactorSourceUseCase(
                displayName = accountName
            )

            val updatedProfile = profile.addAccount(
                account = account,
                onNetwork = network.network.networkId()
            )

            verify(profileRepository).saveProfile(updatedProfile)
            coVerify(exactly = 1) { ensureBabylonFactorSourceExistUseCase() }
        }
    }
}

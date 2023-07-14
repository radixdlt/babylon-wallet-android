package rdx.works.profile.domain.account

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.utils.getNextDerivationPathForAccount
import rdx.works.profile.domain.TestData

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateAccountWithLedgerFactorSourceUseCaseTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `given profile already exists, creating new ledger account adds it to profile`() {
        testScope.runTest {
            // given
            val mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = "noodle question hungry sail type offer grocery clay nation hello mixture forum",
                bip39Passphrase = ""
            )
            val accountName = "First account"
            val network = Radix.Gateway.hammunet
            val profile = TestData.testProfile2Networks2AccountsEach(mnemonicWithPassphrase)

            val profileRepository = Mockito.mock(ProfileRepository::class.java)
            whenever(profileRepository.profileState).thenReturn(flowOf(ProfileState.Restored(profile)))

            val createAccountWithLedgerFactorSourceUseCase = CreateAccountWithLedgerFactorSourceUseCase(
                profileRepository = profileRepository,
                testDispatcher
            )
            val ledgerFactorSource = TestData.ledgerFactorSource
            val account = createAccountWithLedgerFactorSourceUseCase(
                displayName = accountName,
                derivedPublicKeyHex = "7229e3b98ffa35a4ce28b891ff0a9f95c9d959eff58d0e61015fab3a3b2d18f9",
                TestData.ledgerFactorSource.id,
                ledgerFactorSource.getNextDerivationPathForAccount(network.network.networkId())
            )

            val updatedProfile = profile.addAccount(
                account = account,
                withFactorSourceId = ledgerFactorSource.id,
                onNetwork = network.network.networkId()
            )

            verify(profileRepository).saveProfile(updatedProfile)
        }
    }
}

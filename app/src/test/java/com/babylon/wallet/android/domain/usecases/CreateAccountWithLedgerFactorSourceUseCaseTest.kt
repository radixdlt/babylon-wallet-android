package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.ResolveAccountsLedgerStateRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.model.pernetwork.nextAccountIndex
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.domain.TestData

internal class CreateAccountWithLedgerFactorSourceUseCaseTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val resolveAccountsLedgerStateRepository = mockk<ResolveAccountsLedgerStateRepository>()

    @Before
    fun setUp() {
        coEvery { resolveAccountsLedgerStateRepository.invoke(any()) } returns Result.failure(Exception(""))
    }

    @Test
    fun `given profile already exists, creating new ledger account adds it to profile`() {
        testScope.runTest {
            // given
            val mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = "noodle question hungry sail type offer grocery clay nation hello mixture forum",
                bip39Passphrase = ""
            )
            val accountName = "First account"
            val network = Radix.Gateway.hammunet.network
            val profile = TestData.testProfile2Networks2AccountsEach(mnemonicWithPassphrase)

            val profileRepository = Mockito.mock(ProfileRepository::class.java)
            whenever(profileRepository.profileState).thenReturn(flowOf(ProfileState.Restored(profile)))

            val createAccountWithLedgerFactorSourceUseCase = CreateAccountWithLedgerFactorSourceUseCase(
                profileRepository = profileRepository,
                resolveAccountsLedgerStateRepository = resolveAccountsLedgerStateRepository,
                testDispatcher
            )
            val derivationPath = DerivationPath.forAccount(
                networkId = network.networkId(),
                accountIndex = profile.currentNetwork.nextAccountIndex(TestData.ledgerFactorSource),
                keyType = KeyType.TRANSACTION_SIGNING
            )
            val account = createAccountWithLedgerFactorSourceUseCase(
                displayName = accountName,
                derivedPublicKeyHex = "7229e3b98ffa35a4ce28b891ff0a9f95c9d959eff58d0e61015fab3a3b2d18f9",
                derivationPath = derivationPath,
                ledgerFactorSourceID = TestData.ledgerFactorSource.id
            )

            val updatedProfile = profile.addAccounts(
                accounts = listOf(account),
                onNetwork = network.networkId()
            )

            verify(profileRepository).saveProfile(updatedProfile)
        }
    }
}
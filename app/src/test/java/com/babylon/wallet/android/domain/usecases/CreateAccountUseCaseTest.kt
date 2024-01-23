package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.ResolveAccountsLedgerStateRepository
import com.babylon.wallet.android.presentation.accessfactorsource.AccessFactorSourceOutput
import com.radixdlt.extensions.removeLeadingZero
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
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.extensions.nextAccountIndex
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.domain.TestData

class CreateAccountUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mnemonicWithPassphrase = MnemonicWithPassphrase(
        mnemonic = "prison post shoot verb lunch blue limb stick later winner tide roof situate excuse joy muffin cruel fix bag evil call glide resist aware",
        bip39Passphrase = ""
    )
    private val profile = TestData.testProfile2Networks2AccountsEach(mnemonicWithPassphrase)
    val gateway = Radix.Gateway.hammunet
    private val profileRepository = Mockito.mock(ProfileRepository::class.java)
    private val resolveAccountsLedgerStateRepository = mockk<ResolveAccountsLedgerStateRepository>()
    private val derivationPath = DerivationPath.forAccount(
        networkId = gateway.network.networkId(),
        accountIndex = profile.nextAccountIndex(
            factorSource = TestData.ledgerFactorSource,
            derivationPathScheme = DerivationPathScheme.CAP_26,
            forNetworkId = gateway.network.networkId()
        ),
        keyType = KeyType.TRANSACTION_SIGNING
    )

    @Before
    fun setUp() {
        coEvery { resolveAccountsLedgerStateRepository(any()) } returns Result.failure(Exception(""))
    }

    @Test
    fun `given a account name, a factor source, and a public key with derivation path, when CreateAccountUseCase, then create new account and save it to the profile`() {
        testScope.runTest {
            // given
            val displayName = "A"
            val factorSource = TestData.ledgerFactorSource
            val publicKeyAndDerivationPath = AccessFactorSourceOutput.PublicKeyAndDerivationPath(
                compressedPublicKey = mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath)
                    .removeLeadingZero(),
                derivationPath = derivationPath,
            )

            // when
            whenever(profileRepository.profileState).thenReturn(flowOf(ProfileState.Restored(profile)))
            val createAccountUseCase = CreateAccountUseCase(profileRepository, resolveAccountsLedgerStateRepository)
            val account = createAccountUseCase.invoke(
                displayName = displayName,
                factorSource = factorSource,
                publicKeyAndDerivationPath = publicKeyAndDerivationPath,
                onNetworkId = gateway.network.networkId()
            )

            // then
            val updatedProfile = profile.addAccounts(
                accounts = listOf(account),
                onNetwork = gateway.network.networkId()
            )
            verify(profileRepository).saveProfile(updatedProfile)
        }
    }
}
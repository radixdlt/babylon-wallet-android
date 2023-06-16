package com.babylon.wallet.android.presentation.settings.backup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.utils.AppEventBus
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.RestoreMnemonicUseCase

/*
@OptIn(ExperimentalCoroutinesApi::class)
class RestoreMnemonicViewModelTest : StateViewModelTest<RestoreMnemonicViewModel>() {

    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val restoreMnemonicUseCase = mockk<RestoreMnemonicUseCase>()
    private val appEventBus = mockk<AppEventBus>()

    private val mnemonicWithPassphrase = MnemonicWithPassphrase(
        mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
        bip39Passphrase = ""
    )

    private val sampleProfile = sampleDataProvider.sampleProfile(
        mnemonicWithPassphrase = mnemonicWithPassphrase
    ).copy(
        networks = listOf(
            Network(
                networkID = Radix.Gateway.default.network.id,
                accounts = listOf(
                    sampleDataProvider.sampleAccount(
                        address = ACCOUNT_ADDRESS,
                        factorSourceId = FactorSource.factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase)
                    )
                ),
                personas = listOf(),
                authorizedDapps = listOf()
            )
        ),
        factorSources = listOf(
            FactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase)
        )
    )

    override fun initVM(): RestoreMnemonicViewModel = RestoreMnemonicViewModel(
        savedStateHandle = savedStateHandle,
        getProfileUseCase = getProfileUseCase,
        restoreMnemonicUseCase = restoreMnemonicUseCase,
        appEventBus = appEventBus
    )

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_FACTOR_SOURCE_ID) } returns ACCOUNT_ADDRESS
        every { getProfileUseCase() } returns flowOf(sampleProfile)
    }

    @Test
    fun `when user types mnemonic, the words array is filled correctly`() = runTest {
        val vm = vm.value

        (0 until SeedPhraseLength.TWENTY_FOUR.words).forEach {
            vm.onWordChanged(it, "zoo")
            advanceUntilIdle()
        }
        vm.state.test {
            val item = expectMostRecentItem()

            assertEquals(
                "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo",
                item.wordsPhrase
            )
            assertTrue(item.seedPhraseValid)
        }
    }

    @Test
    fun `when words are typed, the button is enabled only on valid word counts`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            vm.onChangeSeedPhraseLength(SeedPhraseLength.TWENTY_FOUR)
            (0 until SeedPhraseLength.TWENTY_FOUR.words).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertTrue(expectMostRecentItem().seedPhraseValid)
            vm.onChangeSeedPhraseLength(SeedPhraseLength.TWENTY_FOUR)
            (0 until SeedPhraseLength.TWENTY_FOUR.words - 1).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertFalse(expectMostRecentItem().seedPhraseValid)

            vm.onChangeSeedPhraseLength(SeedPhraseLength.TWELVE)
            (0 until SeedPhraseLength.TWELVE.words).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertTrue(expectMostRecentItem().seedPhraseValid)
            vm.onChangeSeedPhraseLength(SeedPhraseLength.FIFTEEN)
            (0 until SeedPhraseLength.FIFTEEN.words - 1).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertFalse(expectMostRecentItem().seedPhraseValid)

            vm.onChangeSeedPhraseLength(SeedPhraseLength.FIFTEEN)
            (0 until SeedPhraseLength.FIFTEEN.words).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertTrue(expectMostRecentItem().seedPhraseValid)
            vm.onChangeSeedPhraseLength(SeedPhraseLength.FIFTEEN)
            (0 until SeedPhraseLength.FIFTEEN.words - 1).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertFalse(expectMostRecentItem().seedPhraseValid)

            vm.onChangeSeedPhraseLength(SeedPhraseLength.EIGHTEEN)
            (0 until SeedPhraseLength.EIGHTEEN.words).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertTrue(expectMostRecentItem().seedPhraseValid)
            vm.onChangeSeedPhraseLength(SeedPhraseLength.EIGHTEEN)
            (0 until SeedPhraseLength.EIGHTEEN.words - 1).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertFalse(expectMostRecentItem().seedPhraseValid)

            vm.onChangeSeedPhraseLength(SeedPhraseLength.TWENTY_ONE)
            (0 until SeedPhraseLength.TWENTY_ONE.words).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertTrue(expectMostRecentItem().seedPhraseValid)
            vm.onChangeSeedPhraseLength(SeedPhraseLength.TWENTY_ONE)
            (0 until SeedPhraseLength.TWENTY_ONE.words - 1).forEach {
                vm.onWordChanged(it, "zoo")
                advanceUntilIdle()
            }
            assertFalse(expectMostRecentItem().seedPhraseValid)

        }
    }


    private companion object {
        private const val ACCOUNT_ADDRESS = "account_rdx_abcd"
    }
}
 */

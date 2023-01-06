package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.createaccount.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.createaccount.ARG_REQUEST_SOURCE
import com.babylon.wallet.android.presentation.createaccount.CreateAccountConfirmationEvent
import com.babylon.wallet.android.presentation.createaccount.CreateAccountConfirmationViewModel
import com.babylon.wallet.android.presentation.createaccount.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.utils.truncatedHash
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.EntityAddress
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.FactorSourceReference
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.ProfileRepository

@ExperimentalCoroutinesApi
class CreateAccountConfirmationViewModelTest : BaseViewModelTest<CreateAccountConfirmationViewModel>() {

    private val savedStateHandle = Mockito.mock(SavedStateHandle::class.java)
    private val profileRepository = mockk<ProfileRepository>()
    private val accountId = "fj3489fj348f"
    private val accountName = "My main account"

    @Before
    override fun setUp() = runTest {
        super.setUp()
        whenever(savedStateHandle.get<String>(ARG_ACCOUNT_ID)).thenReturn(accountId)
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME)).thenReturn(accountName)
        whenever(savedStateHandle.get<Boolean>(Screen.ARG_HAS_PROFILE)).thenReturn(false)
        coEvery { profileRepository.getAccounts() } returns listOf(Account(
            entityAddress = EntityAddress(accountId),
            appearanceID = 123,
            derivationPath = "m/1'/1'/1'/1'/1'/1'",
            displayName = accountName,
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
        ))
    }

    @Test
    fun `given profile did not exist, when view model init, verify correct account state and go next`() = runTest {
        // given
        whenever(savedStateHandle.get<CreateAccountRequestSource>(ARG_REQUEST_SOURCE)).thenReturn(
            CreateAccountRequestSource.FirstTime)
        val viewModel = vm.value
        val event = mutableListOf<CreateAccountConfirmationEvent>()

        // when
        viewModel.accountConfirmed()

        viewModel.oneOffEvent
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreateAccountConfirmationViewModel.AccountConfirmationUiState(
                accountName = accountName,
                accountAddressTruncated = accountId.truncatedHash(),
                appearanceId = 123
            ),
            viewModel.accountUiState
        )

        Assert.assertEquals(event.first(), CreateAccountConfirmationEvent.NavigateToHome)
    }

    @Test
    fun `given profile did exist, when view model init, verify correct account state and dismiss`() = runTest {
        whenever(savedStateHandle.get<CreateAccountRequestSource>(ARG_REQUEST_SOURCE)).thenReturn(
            CreateAccountRequestSource.Wallet)
        val event = mutableListOf<CreateAccountConfirmationEvent>()
        val viewModel = vm.value
        viewModel.accountConfirmed()

        viewModel.oneOffEvent
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreateAccountConfirmationViewModel.AccountConfirmationUiState(
                accountName = accountName,
                accountAddressTruncated = accountId.truncatedHash(),
                appearanceId = 123
            ),
            viewModel.accountUiState
        )

        Assert.assertEquals(event.first(), CreateAccountConfirmationEvent.FinishAccountCreation)
    }

    override fun initVM(): CreateAccountConfirmationViewModel {
        return CreateAccountConfirmationViewModel(profileRepository, savedStateHandle)
    }
}
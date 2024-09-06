package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.fakes.FakePreferenceManager
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import rdx.works.core.TimestampGenerator
import rdx.works.core.domain.cloudbackup.BackupState
import rdx.works.core.sargon.changeGateway
import rdx.works.profile.data.repository.CheckKeystoreIntegrityUseCase
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase

@ExperimentalCoroutinesApi
class GetEntitiesWithSecurityPromptUseCaseTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET))
    private val mnemonicRepositoryMock = mockk<MnemonicRepository>()
    private val getBackupStateUseCaseMock = mockk<GetBackupStateUseCase>()
    private val checkKeystoreIntegrityUseCase = mockk<CheckKeystoreIntegrityUseCase>().apply {
        every { didMnemonicIntegrityChange } returns flowOf(false)
    }

    private val getEntitiesWithSecurityPromptUseCase = GetEntitiesWithSecurityPromptUseCase(
        getProfileUseCase = GetProfileUseCase(
            profileRepository = FakeProfileRepository(profile),
            dispatcher = coroutineRule.dispatcher
        ),
        preferencesManager = FakePreferenceManager(),
        mnemonicRepository = mnemonicRepositoryMock,
        getBackupStateUseCase = getBackupStateUseCaseMock,
        checkKeystoreIntegrityUseCase = checkKeystoreIntegrityUseCase
    )

    @Test
    fun `when cloud backup is disabled but manual backup file is updated then no security prompt`() = runTest {
        // no recovery required
        coEvery { mnemonicRepositoryMock.mnemonicExist(key = any()) } returns true

        val now = TimestampGenerator()
        val oneDayBefore = now.minusDays(1)
        // when cloud backup disabled but user has exported an updated manual backup file
        coEvery { getBackupStateUseCaseMock() } returns flowOf(
            BackupState.CloudBackupDisabled(
                email = "email",
                lastCloudBackupTime = oneDayBefore,
                lastManualBackupTime = now.toInstant(),
                lastModifiedProfileTime = oneDayBefore
            )
        )

        val event = mutableListOf<List<EntityWithSecurityPrompt>>()
        getEntitiesWithSecurityPromptUseCase().onEach {
            event.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        val entityWithSecurityPromptList = event.first()
        val prompts = entityWithSecurityPromptList.flatMap { it.prompts }

        Assert.assertTrue(prompts.isEmpty())
    }

    @Test
    fun `when cloud backup is enabled but not working then security prompt is problem with configuration backup`() = runTest {
        // no recovery required
        coEvery { mnemonicRepositoryMock.mnemonicExist(key = any()) } returns true

        val now = TimestampGenerator()
        val oneDayBefore = now.minusDays(1)
        // when cloud backup is enabled but not working, for example unavailable service
        coEvery { getBackupStateUseCaseMock() } returns flowOf(
            BackupState.CloudBackupEnabled(
                email = "email",
                hasAnyErrors = true,
                lastCloudBackupTime = oneDayBefore,
                lastManualBackupTime = now.toInstant(),
                lastModifiedProfileTime = oneDayBefore
            )
        )

        val event = mutableListOf<List<EntityWithSecurityPrompt>>()
        getEntitiesWithSecurityPromptUseCase().onEach {
            event.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        val entityWithSecurityPromptList = event.first()
        val prompts = entityWithSecurityPromptList.flatMap { it.prompts }

        Assert.assertTrue(prompts.isNotEmpty())
        Assert.assertTrue(prompts.contains(SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM))
    }
}
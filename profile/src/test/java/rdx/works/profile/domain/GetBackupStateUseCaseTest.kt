package rdx.works.profile.domain

import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import rdx.works.core.domain.cloudbackup.BackupState
import rdx.works.core.domain.cloudbackup.GoogleDriveFileId
import rdx.works.core.domain.cloudbackup.LastCloudBackupEvent
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.updateCloudSyncEnabled
import rdx.works.profile.FakeProfileRepository
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import rdx.works.profile.cloudbackup.domain.CloudBackupErrorStream
import rdx.works.profile.cloudbackup.model.BackupServiceException
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import java.time.Instant

@ExperimentalCoroutinesApi
class GetBackupStateUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val preferencesManagerMock = mockk<PreferencesManager>()
    private val googleSignInManagerMock = mockk<GoogleSignInManager>()

    @Test
    fun `given cloud backup enabled, when no cloud backup errors, then no warnings on screen`() = testScope.runTest {
        coEvery { googleSignInManagerMock.getSignedInGoogleAccount()?.email } returns "mail@email.com"
        coEvery { preferencesManagerMock.lastManualBackupInstant } returns flowOf(Instant.now())
        coEvery { preferencesManagerMock.lastCloudBackupEvent } returns flowOf(
            LastCloudBackupEvent(
                fileId = GoogleDriveFileId(id = "googleDriveFileId"),
                profileModifiedTime = Timestamp.now(),
                cloudBackupTime = Timestamp.now()
            )
        )

        val getBackupStateUseCase = GetBackupStateUseCase(
            profileRepository = FakeProfileRepository(Profile.sample()),
            preferencesManager = preferencesManagerMock,
            googleSignInManager = googleSignInManagerMock,
            cloudBackupErrorStream = CloudBackupErrorStreamFake(),
        )

        val event = mutableListOf<BackupState>()
        getBackupStateUseCase().onEach {
            event.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        Assert.assertTrue(event.isNotEmpty())
        Assert.assertTrue(event.first().isCloudBackupEnabled)
        Assert.assertNull(event.first().backupWarning)
    }

    @Test
    fun `given cloud backup enabled, when cloud backup service error and neither updated cloud backup nor manual backup, then warnings on screen`() =
        testScope.runTest {
            val profile = Profile.sample()
            val lastProfileModified = profile.header.lastModified

            coEvery { googleSignInManagerMock.getSignedInGoogleAccount()?.email } returns "mail@email.com"
            coEvery { preferencesManagerMock.lastManualBackupInstant } returns flowOf(
                lastProfileModified.minusDays(2).toInstant()
            )
            coEvery { preferencesManagerMock.lastCloudBackupEvent } returns flowOf(
                LastCloudBackupEvent(
                    fileId = GoogleDriveFileId(id = "googleDriveFileId"),
                    profileModifiedTime = lastProfileModified,
                    cloudBackupTime = lastProfileModified.minusHours(4)
                )
            )
            val cloudBackupErrorStreamFake = CloudBackupErrorStreamFake(
                error = BackupServiceException.ServiceException(statusCode = 1, message = "service error")
            )

            val getBackupStateUseCase = GetBackupStateUseCase(
                profileRepository = FakeProfileRepository(profile),
                preferencesManager = preferencesManagerMock,
                googleSignInManager = googleSignInManagerMock,
                cloudBackupErrorStream = cloudBackupErrorStreamFake,
            )

            val event = mutableListOf<BackupState>()
            getBackupStateUseCase().onEach {
                event.add(it)
            }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            advanceUntilIdle()

            Assert.assertTrue(event.isNotEmpty())
            Assert.assertTrue(event.first().isCloudBackupEnabled)
            Assert.assertTrue(event.first().isNotUpdated)
            Assert.assertNotNull(event.first().backupWarning)
        }

    @Test
    fun `given cloud backup disabled, when cloud backup not updated and updated manual backup, then no warnings on screen`() =
        testScope.runTest {
            val profile = Profile.sample().updateCloudSyncEnabled(false)
            val lastProfileModifiedTime = profile.header.lastModified
            val lastCloudBackupTime = lastProfileModifiedTime.minusHours(1)

            coEvery { googleSignInManagerMock.getSignedInGoogleAccount()?.email } returns "mail@email.com"
            coEvery { preferencesManagerMock.lastManualBackupInstant } returns flowOf(lastProfileModifiedTime.toInstant())
            coEvery { preferencesManagerMock.lastCloudBackupEvent } returns flowOf(
                LastCloudBackupEvent(
                    fileId = GoogleDriveFileId(id = "googleDriveFileId"),
                    profileModifiedTime = lastProfileModifiedTime,
                    cloudBackupTime = lastCloudBackupTime
                )
            )
            val cloudBackupErrorStreamFake = CloudBackupErrorStreamFake(
                error = BackupServiceException.ServiceException(statusCode = 1, message = "service error")
            )

            val getBackupStateUseCase = GetBackupStateUseCase(
                profileRepository = FakeProfileRepository(profile),
                preferencesManager = preferencesManagerMock,
                googleSignInManager = googleSignInManagerMock,
                cloudBackupErrorStream = cloudBackupErrorStreamFake,
            )

            val event = mutableListOf<BackupState>()
            getBackupStateUseCase().onEach {
                event.add(it)
            }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            advanceUntilIdle()

            Assert.assertTrue(event.isNotEmpty())
            Assert.assertFalse(event.first().isCloudBackupEnabled)
            Assert.assertFalse(event.first().isNotUpdated)
            Assert.assertNull(event.first().backupWarning)
        }

    @Test
    fun `given cloud backup enabled, when cloud backup gets unauthorized and neither updated cloud backup nor manual backup, then warnings on screen and cloud backup disabled`() =
        testScope.runTest {
            val profile = Profile.sample()
            val lastProfileModifiedTime = profile.header.lastModified
            val lastManualBackupTime = lastProfileModifiedTime.minusDays(3)
            val lastCloudBackupTime = lastProfileModifiedTime.minusHours(1)

            coEvery { googleSignInManagerMock.getSignedInGoogleAccount()?.email } returns "mail@email.com"
            coEvery { preferencesManagerMock.lastManualBackupInstant } returns flowOf(lastManualBackupTime.toInstant())
            coEvery { preferencesManagerMock.lastCloudBackupEvent } returns flowOf(
                LastCloudBackupEvent(
                    fileId = GoogleDriveFileId(id = "googleDriveFileId"),
                    profileModifiedTime = lastProfileModifiedTime,
                    cloudBackupTime = lastCloudBackupTime
                )
            )
            val cloudBackupErrorStreamFake = CloudBackupErrorStreamFake(
                error = BackupServiceException.UnauthorizedException
            )

            val getBackupStateUseCase = GetBackupStateUseCase(
                profileRepository = FakeProfileRepository(profile),
                preferencesManager = preferencesManagerMock,
                googleSignInManager = googleSignInManagerMock,
                cloudBackupErrorStream = cloudBackupErrorStreamFake,
            )

            val event = mutableListOf<BackupState>()
            getBackupStateUseCase().onEach {
                event.add(it)
            }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            advanceUntilIdle()

            Assert.assertTrue(event.isNotEmpty())
            Assert.assertFalse(event.first().isCloudBackupEnabled)
            Assert.assertTrue(event.first().isNotUpdated)
            Assert.assertNotNull(event.first().backupWarning)
        }

    @Test
    fun `given cloud backup disabled, when cloud backup gets deleted, then last cloud backup label is none`() =
        testScope.runTest {
            val profile = Profile.sample().updateCloudSyncEnabled(false)
            val lastProfileModifiedTime = profile.header.lastModified
            val lastManualBackupTime = lastProfileModifiedTime.minusDays(3)

            coEvery { googleSignInManagerMock.getSignedInGoogleAccount()?.email } returns "mail@email.com"
            coEvery { preferencesManagerMock.lastManualBackupInstant } returns flowOf(lastManualBackupTime.toInstant())
            coEvery { preferencesManagerMock.lastCloudBackupEvent } returns flowOf(null)
            val cloudBackupErrorStreamFake = CloudBackupErrorStreamFake(
                error = BackupServiceException.UnauthorizedException
            )

            val getBackupStateUseCase = GetBackupStateUseCase(
                profileRepository = FakeProfileRepository(profile),
                preferencesManager = preferencesManagerMock,
                googleSignInManager = googleSignInManagerMock,
                cloudBackupErrorStream = cloudBackupErrorStreamFake,
            )

            val event = mutableListOf<BackupState>()
            getBackupStateUseCase().onEach {
                event.add(it)
            }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            advanceUntilIdle()

            Assert.assertTrue(event.isNotEmpty())
            Assert.assertFalse(event.first().isCloudBackupEnabled)
            Assert.assertTrue(event.first().isNotUpdated)
            Assert.assertNull(event.first().lastCloudBackupTime)
        }
}

class CloudBackupErrorStreamFake(private val error: BackupServiceException? = null) : CloudBackupErrorStream {

    override val errors: MutableStateFlow<BackupServiceException?>
        get() = MutableStateFlow(error)

    override fun onError(error: BackupServiceException) {
        errors.update { error }
    }

    override fun resetErrors() {
        errors.update { null }
    }

}
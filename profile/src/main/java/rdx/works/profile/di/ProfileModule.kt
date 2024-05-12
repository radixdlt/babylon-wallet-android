package rdx.works.profile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rdx.works.profile.cloudbackup.DriveClient
import rdx.works.profile.cloudbackup.DriveClientImpl
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.BackupProfileRepositoryImpl
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.DAppConnectionRepositoryImpl
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.DeviceInfoRepositoryImpl
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.ProfileRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ProfileModule {

    @Binds
    @Singleton
    fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    fun bindBackupProfileRepository(
        backupProfileRepositoryImpl: BackupProfileRepositoryImpl
    ): BackupProfileRepository

    @Binds
    @Singleton
    fun bindDriveClient(
        driveClientImpl: DriveClientImpl
    ): DriveClient

    @Binds
    fun bindDAppConnectionRepository(
        dAppConnectionRepositoryImpl: DAppConnectionRepositoryImpl
    ): DAppConnectionRepository

    @Binds
    fun bindDeviceInfoRepository(
        deviceInfoRepositoryImpl: DeviceInfoRepositoryImpl
    ): DeviceInfoRepository
}

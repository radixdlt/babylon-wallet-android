package rdx.works.profile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.DAppConnectionRepositoryImpl
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.DeviceInfoRepositoryImpl
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.ProfileDataSourceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ProfileModule {

    @Binds
    @Singleton
    fun bindProfileDataSource(
        profileDataSourceImpl: ProfileDataSourceImpl
    ): ProfileDataSource

    @Binds
    fun bindDAppConnectionRepository(
        dAppConnectionRepositoryImpl: DAppConnectionRepositoryImpl
    ): DAppConnectionRepository

    @Binds
    fun bindDeviceInfoRepository(
        deviceInfoRepositoryImpl: DeviceInfoRepositoryImpl
    ): DeviceInfoRepository
}

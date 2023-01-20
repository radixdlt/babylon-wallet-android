package rdx.works.profile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.AccountRepositoryImpl
import rdx.works.profile.data.repository.NetworkRepository
import rdx.works.profile.data.repository.NetworkRepositoryImpl
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.data.repository.PersonaRepositoryImpl
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
    @Singleton
    fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    fun bindNetworkRepository(
        networkRepositoryImpl: NetworkRepositoryImpl
    ): NetworkRepository

    @Binds
    @Singleton
    fun bindPersonaRepository(
        personaRepositoryImpl: PersonaRepositoryImpl
    ): PersonaRepository
}

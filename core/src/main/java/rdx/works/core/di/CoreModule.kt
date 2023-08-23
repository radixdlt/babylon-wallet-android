package rdx.works.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rdx.works.core.storage.FileRepository
import rdx.works.core.storage.FileRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CoreModule {

    @Binds
    @Singleton
    fun bindFileRepository(
        fileRepositoryImpl: FileRepositoryImpl
    ): FileRepository

}

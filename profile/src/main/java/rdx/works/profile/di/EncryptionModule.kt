package rdx.works.profile.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rdx.works.datastore.EncryptedDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EncryptionModule {

    @Provides
    @Singleton
    fun provideEncryptedDataStore(
        @ApplicationContext context: Context
    ): EncryptedDataStore = EncryptedDataStore(context)
}

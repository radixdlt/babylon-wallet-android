package rdx.works.profile.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rdx.works.profile.datastore.EncryptedPreferencesManager.Companion.DATA_STORE_NAME
import rdx.works.profile.datastore.EncryptedPreferencesManager.Companion.PERMANENT_DATA_STORE_NAME
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EncryptionModule {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATA_STORE_NAME
    )

    private val Context.permanentDataStore: DataStore<Preferences> by preferencesDataStore(
        name = PERMANENT_DATA_STORE_NAME
    )

    @Provides
    @ProfileDataStore
    @Singleton
    fun provideEncryptedDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    @Provides
    @PermanentDataStore
    @Singleton
    fun providePermanentDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.permanentDataStore
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ProfileDataStore

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class PermanentDataStore

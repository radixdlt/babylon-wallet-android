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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import rdx.works.profile.datastore.EncryptedPreferencesManager.Companion.DATA_STORE_NAME
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EncryptionModule {

    @Suppress("InjectDispatcher")
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATA_STORE_NAME,
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    )

    @Provides
    @ProfileDataStore
    @Singleton
    fun provideEncryptedDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ProfileDataStore

package rdx.works.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    private const val DATASTORE_NAME_ENCRPYPTED = "rdx_encrypted_datastore"
    private const val DATASTORE_NAME_PERMANENT_ENCRPYPTED = "rdx_permanent_datastore"
    private const val DATASTORE_HOST_ID = "rdx_host_id"
    private const val LEGACY_SHARED_PREFERENCES_DEVICE = "device_prefs"

    private val Context.hostInfoPreferences: DataStore<Preferences> by preferencesDataStore(
        name = DATASTORE_HOST_ID,
        produceMigrations = { context ->
            // HostId used to be stored in a shared preferences file
            listOf(SharedPreferencesMigration(context, LEGACY_SHARED_PREFERENCES_DEVICE))
        }
    )

    private val Context.encryptedDataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATASTORE_NAME_ENCRPYPTED
    )

    private val Context.permanentEncryptedDataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATASTORE_NAME_PERMANENT_ENCRPYPTED
    )

    @Provides
    @EncryptedPreferences
    @Singleton
    fun provideEncryptedDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.encryptedDataStore

    @Provides
    @PermanentEncryptedPreferences
    @Singleton
    fun providePermanentDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.permanentEncryptedDataStore

    @Provides
    @Singleton
    @HostInfoPreferences
    fun hostInfoPreferences(
        @ApplicationContext context: Context
    ) = context.hostInfoPreferences

}
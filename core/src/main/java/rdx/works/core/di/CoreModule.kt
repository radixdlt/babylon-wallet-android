package rdx.works.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.radixdlt.sargon.Bios
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.os.driver.AndroidEventBusDriver
import com.radixdlt.sargon.os.driver.AndroidProfileStateChangeDriver
import com.radixdlt.sargon.os.driver.BiometricsHandler
import com.radixdlt.sargon.os.from
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import rdx.works.core.storage.FileRepository
import rdx.works.core.storage.FileRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CoreBindings {

    @Binds
    @Singleton
    fun bindFileRepository(
        fileRepositoryImpl: FileRepositoryImpl
    ): FileRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CoreProvider {

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

    @Provides
    @Singleton
    fun provideEventBusDriver(): AndroidEventBusDriver = AndroidEventBusDriver

    @Provides
    @Singleton
    fun provideProfileStateChangeDriver(): AndroidProfileStateChangeDriver = AndroidProfileStateChangeDriver

    @Suppress("LongParameterList")
    @Provides
    @Singleton
    fun provideBios(
        @ApplicationContext context: Context,
        @GatewayHttpClient httpClient: OkHttpClient,
        eventBusDriver: AndroidEventBusDriver,
        profileStateChangeDriver: AndroidProfileStateChangeDriver,
        biometricsHandler: BiometricsHandler,
        @EncryptedPreferences encryptedPreferences: DataStore<Preferences>,
        @NonEncryptedPreferences preferences: DataStore<Preferences>,
        @HostInfoPreferences hostInfoPreferences: DataStore<Preferences>,
    ): Bios = Bios.from(
        context = context,
        httpClient = httpClient,
        biometricsHandler = biometricsHandler,
        encryptedPreferencesDataStore = encryptedPreferences,
        preferencesDatastore = preferences,
        deviceInfoDatastore = hostInfoPreferences,
        eventBusDriver = eventBusDriver,
        profileStateChangeDriver = profileStateChangeDriver
    )

    @Provides
    @Singleton
    fun provideSargonOsManager(
        bios: Bios,
        @ApplicationScope applicationScope: CoroutineScope,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): SargonOsManager = SargonOsManager.factory(
        bios = bios,
        applicationScope = applicationScope,
        defaultDispatcher = defaultDispatcher
    )
}

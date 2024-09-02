package rdx.works.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.radixdlt.sargon.Bios
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
import okhttp3.OkHttpClient
import rdx.works.core.BuildConfig
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
interface CoreProvider {
    @Provides
    @Singleton
    fun provideBios(
        @ApplicationContext context: Context,
        httpClient: OkHttpClient,
        eventBusDriver: AndroidEventBusDriver,
        profileStateChangeDriver: AndroidProfileStateChangeDriver,
        biometricsHandler: BiometricsHandler,
        @EncryptedPreferences encryptedPreferences: DataStore<Preferences>,
        @NonEncryptedPreferences preferences: DataStore<Preferences>,
        @DeviceInfoPreferences deviceInfoPreferences: DataStore<Preferences>,
    ): Bios = Bios.from(
        context = context,
        enableLogging = BuildConfig.DEBUG,
        httpClient = httpClient,
        biometricsHandler = biometricsHandler,
        encryptedPreferencesDataStore = encryptedPreferences,
        preferencesDatastore = preferences,
        deviceInfoDatastore = deviceInfoPreferences,
        eventBusDriver = eventBusDriver,
        profileStateChangeDriver = profileStateChangeDriver
    )
}

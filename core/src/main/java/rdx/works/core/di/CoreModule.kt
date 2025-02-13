package rdx.works.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.radixdlt.sargon.AuthorizationPurpose
import com.radixdlt.sargon.AuthorizationResponse
import com.radixdlt.sargon.Bios
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HostInteractor
import com.radixdlt.sargon.KeyDerivationRequest
import com.radixdlt.sargon.KeyDerivationResponse
import com.radixdlt.sargon.SignRequestOfAuthIntent
import com.radixdlt.sargon.SignRequestOfSubintent
import com.radixdlt.sargon.SignRequestOfTransactionIntent
import com.radixdlt.sargon.SignResponseOfAuthIntentHash
import com.radixdlt.sargon.SignResponseOfSubintentHash
import com.radixdlt.sargon.SignResponseOfTransactionIntentHash
import com.radixdlt.sargon.SpotCheckResponse
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
        hostInteractor = HostInteractorStub(),
        applicationScope = applicationScope,
        defaultDispatcher = defaultDispatcher
    )
}

private class HostInteractorStub : HostInteractor {

    override suspend fun deriveKeys(request: KeyDerivationRequest): KeyDerivationResponse {
        throw CommonException.Unknown()
    }

    override suspend fun requestAuthorization(purpose: AuthorizationPurpose): AuthorizationResponse {
        throw CommonException.Unknown()
    }

    override suspend fun signAuth(request: SignRequestOfAuthIntent): SignResponseOfAuthIntentHash {
        throw CommonException.Unknown()
    }

    override suspend fun signSubintents(request: SignRequestOfSubintent): SignResponseOfSubintentHash {
        throw CommonException.Unknown()
    }

    override suspend fun signTransactions(request: SignRequestOfTransactionIntent): SignResponseOfTransactionIntentHash {
        throw CommonException.Unknown()
    }

    override suspend fun spotCheck(factorSource: FactorSource, allowSkip: Boolean): SpotCheckResponse {
        throw CommonException.Unknown()
    }
}

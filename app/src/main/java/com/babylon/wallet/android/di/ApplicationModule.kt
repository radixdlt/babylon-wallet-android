package com.babylon.wallet.android.di

import android.app.backup.BackupManager
import android.content.ClipboardManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.babylon.wallet.android.data.repository.cache.CacheClient
import com.babylon.wallet.android.data.repository.cache.EncryptedDiskCacheClient
import com.babylon.wallet.android.data.repository.homecards.HomeCardsObserverWrapper
import com.babylon.wallet.android.data.repository.homecards.HomeCardsObserverWrapperImpl
import com.radixdlt.sargon.HomeCardsManager
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.RadixConnectMobile
import com.radixdlt.sargon.extensions.init
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    const val PREFERENCES_NAME = "rdx_datastore"

    private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
        name = PREFERENCES_NAME
    )

    @Provides
    @Singleton
    fun provideClipboardManager(
        @ApplicationContext context: Context
    ): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.userDataStore
    }

    @Provides
    @Singleton
    fun provideCacheClient(
        @ApplicationContext applicationContext: Context,
        jsonSerializer: Json,
    ): CacheClient {
        return EncryptedDiskCacheClient(applicationContext, jsonSerializer)
    }

    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext applicationContext: Context
    ): BackupManager {
        return BackupManager(applicationContext)
    }

    @Provides
    @Singleton
    fun provideRadixConnectMobile(
        @ApplicationContext context: Context,
        @GatewayHttpClient httpClient: OkHttpClient
    ): RadixConnectMobile = RadixConnectMobile.init(
        context = context,
        okHttpClient = httpClient
    )

    @Provides
    @Singleton
    fun provideHomeCardsObserverWrapper(): HomeCardsObserverWrapper = HomeCardsObserverWrapperImpl()

    @Provides
    @Singleton
    fun provideHomeCardsManager(
        @GatewayHttpClient httpClient: OkHttpClient,
        dataStore: DataStore<Preferences>,
        observer: HomeCardsObserverWrapper,
    ): HomeCardsManager = HomeCardsManager.init(
        okHttpClient = httpClient,
        /**
         * For now we'll only use MainNet as it's enough to fulfill all the scenarios
         * regarding Home Cards initialized from dApps and RadQuest deep link
         */
        networkId = NetworkId.MAINNET,
        dataStore = dataStore,
        observer = observer
    )
}

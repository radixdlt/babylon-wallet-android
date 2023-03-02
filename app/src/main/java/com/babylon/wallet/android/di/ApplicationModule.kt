package com.babylon.wallet.android.di

import android.content.ClipboardManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.babylon.wallet.android.data.repository.cache.CacheClient
import com.babylon.wallet.android.data.repository.cache.EncryptedDiskCacheClient
import com.babylon.wallet.android.data.repository.time.CurrentTime
import com.babylon.wallet.android.data.repository.time.CurrentTimeImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "rdx_datastore"
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
    fun provideCurrentTime(): CurrentTime = CurrentTimeImpl()
}

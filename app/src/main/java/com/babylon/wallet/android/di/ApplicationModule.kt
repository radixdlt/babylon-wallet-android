package com.babylon.wallet.android.di

import android.content.ClipboardManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.babylon.wallet.android.data.MainViewRepositoryImpl
import com.babylon.wallet.android.data.dapp.DAppConnectionRepositoryImpl
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.domain.dapp.DAppConnectionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "rdx_datastore"
    )

    @Provides
    @Singleton
    fun provideMainViewRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MainViewRepository = MainViewRepositoryImpl(ioDispatcher)

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
    fun provideDAppConnectionRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DAppConnectionRepository = DAppConnectionRepositoryImpl(ioDispatcher)
}

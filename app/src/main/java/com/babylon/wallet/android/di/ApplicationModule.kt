package com.babylon.wallet.android.di

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import com.babylon.wallet.android.PreferencesManager
import com.babylon.wallet.android.data.MainViewRepositoryImpl
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.MainViewRepository
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
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences = context.getSharedPreferences("rdx_prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun providePreferencesManager(
        sharedPreferences: SharedPreferences
    ): PreferencesManager = PreferencesManager(sharedPreferences)
}

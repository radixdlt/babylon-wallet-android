package com.babylon.wallet.android.di

import android.content.ClipboardManager
import android.content.Context
import com.babylon.wallet.android.data.MainViewRepositoryImpl
import com.babylon.wallet.android.domain.MainViewRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    @Singleton
    fun provideMainViewRepository(): MainViewRepository = MainViewRepositoryImpl()

    @Provides
    @Singleton
    fun provideClipboardManager(@ApplicationContext context: Context): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}

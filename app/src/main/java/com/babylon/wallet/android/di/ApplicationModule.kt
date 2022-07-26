package com.babylon.wallet.android.di

import com.babylon.wallet.android.MainViewRepository
import com.babylon.wallet.android.MainViewRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    fun provideMainViewRepository(): MainViewRepository = MainViewRepositoryImpl()

}
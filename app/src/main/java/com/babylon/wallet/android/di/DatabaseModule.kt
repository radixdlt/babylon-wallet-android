package com.babylon.wallet.android.di

import android.content.Context
import com.babylon.wallet.android.data.repository.cache.database.DAppDirectoryDao
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.StateDatabase
import com.babylon.wallet.android.data.repository.cache.database.TokenPriceDao
import com.babylon.wallet.android.data.repository.cache.database.locker.AccountLockerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStateDatabase(
        @ApplicationContext applicationContext: Context
    ): StateDatabase {
        return StateDatabase.factory(applicationContext)
    }

    @Provides
    fun provideStateDao(
        stateDatabase: StateDatabase
    ): StateDao {
        return stateDatabase.stateDao()
    }

    @Provides
    fun provideTokenPriceDao(
        stateDatabase: StateDatabase
    ): TokenPriceDao {
        return stateDatabase.tokenPriceDao()
    }

    @Provides
    fun provideAccountLockerDao(
        stateDatabase: StateDatabase
    ): AccountLockerDao {
        return stateDatabase.accountLockerDao()
    }

    @Provides
    fun provideDAppDirectoryDao(
        stateDatabase: StateDatabase
    ): DAppDirectoryDao {
        return stateDatabase.dAppDirectoryDao()
    }
}

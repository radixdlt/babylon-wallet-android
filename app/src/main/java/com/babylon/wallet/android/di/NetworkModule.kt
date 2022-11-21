package com.babylon.wallet.android.di

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.PeerdroidClientImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rdx.works.peerdroid.data.PeerdroidConnector
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providePeerdroidClient(
        peerdroidConnector: PeerdroidConnector
    ): PeerdroidClient = PeerdroidClientImpl(
        peerdroidConnector = peerdroidConnector
    )
}

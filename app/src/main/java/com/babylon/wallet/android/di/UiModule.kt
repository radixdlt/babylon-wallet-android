package com.babylon.wallet.android.di

import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxyImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface UiModule {

    @Binds
    fun bindAccessFactorSourcesIOHandler(
        accessFactorSourcesProxyImpl: AccessFactorSourcesProxyImpl
    ): AccessFactorSourcesIOHandler

    @Binds
    fun bindAccessFactorSourcesProxy(
        accessFactorSourcesProxyImpl: AccessFactorSourcesProxyImpl
    ): AccessFactorSourcesProxy
}

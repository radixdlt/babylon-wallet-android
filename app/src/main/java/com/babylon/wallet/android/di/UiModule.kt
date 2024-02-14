package com.babylon.wallet.android.di

import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxyImpl
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesUiProxy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
interface UiModule {

    @Binds
    fun bindAccessFactorSourcesUiProxy(
        accessFactorSourcesProxyImpl: AccessFactorSourcesProxyImpl
    ): AccessFactorSourcesUiProxy

    @Binds
    fun bindAccessFactorSourcesProxy(
        accessFactorSourcesProxyImpl: AccessFactorSourcesProxyImpl
    ): AccessFactorSourcesProxy
}

package com.babylon.wallet.android.di

import com.babylon.wallet.android.presentation.accessfactorsource.AccessFactorSourceProxy
import com.babylon.wallet.android.presentation.accessfactorsource.AccessFactorSourceProxyImpl
import com.babylon.wallet.android.presentation.accessfactorsource.AccessFactorSourceUiProxy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
interface ViewModule {

    @Binds
    fun bindInputOutput(
        accessFactorSourceProxyImpl: AccessFactorSourceProxyImpl
    ): AccessFactorSourceUiProxy

    @Binds
    fun bindAccessFactorSourceProxy(
        accessFactorSourceProxyImpl: AccessFactorSourceProxyImpl
    ): AccessFactorSourceProxy
}

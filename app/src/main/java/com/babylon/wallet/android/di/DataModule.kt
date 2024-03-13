package com.babylon.wallet.android.di

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.DappMessengerImpl
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.LedgerMessengerImpl
import com.babylon.wallet.android.data.repository.NPSSurveyRepository
import com.babylon.wallet.android.data.repository.NPSSurveyRepositoryImpl
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.HttpCacheImpl
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepositoryImpl
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepositoryImpl
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.state.StateRepositoryImpl
import com.babylon.wallet.android.data.repository.stream.StreamRepository
import com.babylon.wallet.android.data.repository.stream.StreamRepositoryImpl
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepositoryImpl
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.repository.transaction.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.preferences.PreferencesManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
interface DataModule {

    @Binds
    fun bindStateRepository(
        stateRepository: StateRepositoryImpl
    ): StateRepository

    @Binds
    fun bindStreamRepository(
        streamRepository: StreamRepositoryImpl
    ): StreamRepository

    @Binds
    fun bindWellKnownDAppDefinitionRepository(
        wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepositoryImpl
    ): WellKnownDAppDefinitionRepository

    @Binds
    fun bindTransactionRepository(
        transactionRepository: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    fun bindNPSSurveyRepository(
        nPSSurveyRepositoryImpl: NPSSurveyRepositoryImpl
    ): NPSSurveyRepository

    @Binds
    fun bindNetworkInfoRepository(
        networkInfoRepository: NetworkInfoRepositoryImpl
    ): NetworkInfoRepository

    @Binds
    fun bindFiatPriceRepository(
        tokenPriceRepository: FiatPriceRepositoryImpl
    ): FiatPriceRepository

    @Binds
    fun bindDAppMessenger(
        dAppMessenger: DappMessengerImpl
    ): DappMessenger

    @Binds
    fun bindLedgerMessenger(
        ledgerMessenger: LedgerMessengerImpl
    ): LedgerMessenger

    @Binds
    @Singleton
    fun bindIncomingRequestRepository(
        dAppMessenger: IncomingRequestRepositoryImpl
    ): IncomingRequestRepository

    @Binds
    @Singleton
    fun bindHttpCache(
        cache: HttpCacheImpl
    ): HttpCache

    @Binds
    @Singleton
    fun bindPreferenceManager(
        preferencesManager: PreferencesManagerImpl
    ): PreferencesManager
}

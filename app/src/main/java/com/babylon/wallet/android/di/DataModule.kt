package com.babylon.wallet.android.di

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.DappMessengerImpl
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.LedgerMessengerImpl
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.HttpCacheImpl
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepositoryImpl
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.entity.EntityRepositoryImpl
import com.babylon.wallet.android.data.repository.metadata.MetadataRepository
import com.babylon.wallet.android.data.repository.metadata.MetadataRepositoryImpl
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepositoryImpl
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.state.StateRepositoryImpl
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.repository.transaction.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindEntityRepository(
        entityRepository: EntityRepositoryImpl
    ): EntityRepository

    @Binds
    fun bindStateRepository(
        stateRepository: StateRepositoryImpl
    ): StateRepository

    @Binds
    fun bindMetadataRepository(
        metadataRepository: MetadataRepositoryImpl
    ): MetadataRepository

    @Binds
    fun bindTransactionRepository(
        transactionRepository: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    fun bindNetworkInfoRepository(
        networkInfoRepository: NetworkInfoRepositoryImpl
    ): NetworkInfoRepository

    @Binds
    fun bindDappMetadataRepository(
        dappMetadataRepository: DAppRepositoryImpl
    ): DAppRepository

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
}

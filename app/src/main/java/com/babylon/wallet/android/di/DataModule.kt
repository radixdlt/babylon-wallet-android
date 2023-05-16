package com.babylon.wallet.android.di

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.DappMessengerImpl
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.LedgerMessengerImpl
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.HttpCacheImpl
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepositoryRCNetImpl
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.entity.EntityRepositoryRCNetImpl
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepositoryImpl
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
        entityRepository: EntityRepositoryRCNetImpl
    ): EntityRepository

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
        dappMetadataRepository: DappMetadataRepositoryRCNetImpl
    ): DappMetadataRepository

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

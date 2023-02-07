package com.babylon.wallet.android.di

import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.DAppMessengerImpl
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepositoryImpl
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.entity.EntityRepositoryImpl
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepositoryImpl
import com.babylon.wallet.android.data.repository.nonfungible.NonFungibleRepository
import com.babylon.wallet.android.data.repository.nonfungible.NonFungibleRepositoryImpl
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
    fun bindTransactionRepository(
        transactionRepository: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    fun bindNonFungibleRepository(
        nonFungibleRepository: NonFungibleRepositoryImpl
    ): NonFungibleRepository

    @Binds
    fun bindNetworkInfoRepository(
        networkInfoRepository: NetworkInfoRepositoryImpl
    ): NetworkInfoRepository

    @Binds
    fun bindDappMetadataRepository(
        dappMetadataRepository: DappMetadataRepositoryImpl
    ): DappMetadataRepository

    @Binds
    fun bindDAppMessenger(
        dAppMessenger: DAppMessengerImpl
    ): DAppMessenger

    @Binds
    @Singleton
    fun bindIncomingRequestRepository(
        dAppMessenger: IncomingRequestRepositoryImpl
    ): IncomingRequestRepository
}

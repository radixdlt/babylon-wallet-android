package com.babylon.wallet.android.di

import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.DAppMessengerImpl
import com.babylon.wallet.android.data.dapp.DAppRepositoryImpl
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.entity.EntityRepositoryImpl
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepositoryImpl
import com.babylon.wallet.android.data.repository.nonfungible.NonFungibleRepository
import com.babylon.wallet.android.data.repository.nonfungible.NonFungibleRepositoryImpl
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.repository.transaction.TransactionRepositoryImpl
import com.babylon.wallet.android.domain.dapp.DAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindDAppRepository(
        dAppRepository: DAppRepositoryImpl
    ): DAppRepository

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
    fun bindDAppMessenger(
        dAppMessenger: DAppMessengerImpl
    ): DAppMessenger
}

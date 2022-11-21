package com.babylon.wallet.android.di

import com.babylon.wallet.android.data.dapp.DAppRepositoryImpl
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.entity.EntityRepositoryImpl
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
    fun bindDAppRepository(dAppRepository: DAppRepositoryImpl): DAppRepository

    @Binds
    fun bindsEntityRepository(
        entityRepository: EntityRepositoryImpl
    ): EntityRepository

    @Binds
    fun bindsTransactionRepository(
        transactionRepository: TransactionRepositoryImpl
    ): TransactionRepository
}

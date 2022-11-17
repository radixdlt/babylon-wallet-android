package com.babylon.wallet.android.di

import com.babylon.wallet.android.domain.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.repository.entity.EntityRepositoryImpl
import com.babylon.wallet.android.domain.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.repository.transaction.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindsEntityRepository(
        entityRepository: EntityRepositoryImpl
    ): EntityRepository

    @Binds
    fun bindsTransactionRepository(
        transactionRepository: TransactionRepositoryImpl
    ): TransactionRepository

}

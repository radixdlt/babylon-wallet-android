package com.babylon.wallet.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rdx.works.profile.domain.UpdatePersonaUseCase
import rdx.works.profile.domain.UpdatePersonaUseCaseImpl

@Module
@InstallIn(SingletonComponent::class)
interface UseCaseModule {

    @Binds
    fun bindUpdatePersonaUseCase(
        useCase: UpdatePersonaUseCaseImpl
    ): UpdatePersonaUseCase
}

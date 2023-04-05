package rdx.works.peerdroid.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.peerdroid.data.PeerdroidLinkImpl

@Module
@InstallIn(ViewModelComponent::class)
object PeerdroidLinkModule {

    @Provides
    @ViewModelScoped
    internal fun providePeerdroidLink(
        @ApplicationContext applicationContext: Context,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PeerdroidLink = PeerdroidLinkImpl(
        applicationContext = applicationContext,
        applicationScope = applicationScope,
        ioDispatcher = ioDispatcher
    )
}

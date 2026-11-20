package rdx.works.peerdroid.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.data.RadixConnectTransportSettings
import rdx.works.peerdroid.data.iroh.AndroidNetworkChangeMonitor
import rdx.works.peerdroid.data.iroh.IrohConnector
import rdx.works.peerdroid.data.iroh.IrohPeerdroidConnector
import rdx.works.peerdroid.data.iroh.IrohRelayConfig
import rdx.works.peerdroid.data.iroh.IrohSessionFactory
import rdx.works.peerdroid.data.iroh.NativeIrohSessionFactory
import javax.inject.Singleton

/**
 * The iroh transport's singletons. Opaque outside peerdroid: the app only gets [IrohRelayConfig] and the
 * connector/link interfaces.
 */
class IrohTransportComponents internal constructor(
    internal val sessionFactory: IrohSessionFactory,
    internal val connector: IrohConnector
)

@Module
@InstallIn(SingletonComponent::class)
object IrohModule {

    @Provides
    @Singleton
    internal fun provideIrohTransportComponents(
        @ApplicationContext applicationContext: Context,
        settings: RadixConnectTransportSettings,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): IrohTransportComponents {
        val sessionFactory = NativeIrohSessionFactory(applicationContext = applicationContext)
        return IrohTransportComponents(
            sessionFactory = sessionFactory,
            connector = IrohPeerdroidConnector(
                sessionFactory = sessionFactory,
                settings = settings,
                networkChangeMonitor = AndroidNetworkChangeMonitor(applicationContext),
                applicationScope = applicationScope,
                ioDispatcher = ioDispatcher
            )
        )
    }

    @Provides
    internal fun provideIrohRelayConfig(
        components: IrohTransportComponents
    ): IrohRelayConfig = components.sessionFactory

    @Provides
    @IrohTransport
    internal fun provideIrohPeerdroidConnector(
        components: IrohTransportComponents
    ): PeerdroidConnector = components.connector
}

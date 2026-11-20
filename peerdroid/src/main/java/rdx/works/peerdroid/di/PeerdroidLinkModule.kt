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
import rdx.works.peerdroid.data.CompositePeerdroidLink
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.peerdroid.data.PeerdroidLinkImpl
import rdx.works.peerdroid.data.PeerdroidTransport
import rdx.works.peerdroid.data.RadixConnectTransportSettings
import rdx.works.peerdroid.data.iroh.IrohPeerdroidLink

@Module
@InstallIn(ViewModelComponent::class)
object PeerdroidLinkModule {

    @Provides
    @ViewModelScoped
    @WebRtcTransport
    internal fun provideWebRtcPeerdroidLink(
        @ApplicationContext applicationContext: Context,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PeerdroidLink = PeerdroidLinkImpl(
        applicationContext = applicationContext,
        applicationScope = applicationScope,
        ioDispatcher = ioDispatcher
    )

    @Provides
    @ViewModelScoped
    @IrohTransport
    internal fun provideIrohPeerdroidLink(
        irohTransportComponents: IrohTransportComponents,
        settings: RadixConnectTransportSettings,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PeerdroidLink = IrohPeerdroidLink(
        sessionFactory = irohTransportComponents.sessionFactory,
        irohConnector = irohTransportComponents.connector,
        settings = settings,
        ioDispatcher = ioDispatcher,
        // The linking session closes after the linking screen (and its view model) may be gone.
        closingScope = applicationScope
    )

    /**
     * The link the app uses: WebRTC and/or iroh, following [RadixConnectTransportSettings]. Each transport is only
     * created when the selected mode needs it.
     */
    @Provides
    @ViewModelScoped
    internal fun providePeerdroidLink(
        @WebRtcTransport webRtcLink: dagger.Lazy<PeerdroidLink>,
        @IrohTransport irohLink: dagger.Lazy<PeerdroidLink>,
        settings: RadixConnectTransportSettings,
        @ApplicationScope applicationScope: CoroutineScope
    ): PeerdroidLink = CompositePeerdroidLink(
        links = mapOf(
            PeerdroidTransport.WEBRTC to lazy { webRtcLink.get() },
            PeerdroidTransport.IROH to lazy { irohLink.get() }
        ),
        settings = settings,
        learningScope = applicationScope
    )
}

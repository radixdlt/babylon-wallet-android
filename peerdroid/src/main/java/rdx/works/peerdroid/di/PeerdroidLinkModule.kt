package rdx.works.peerdroid.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.peerdroid.data.PeerdroidLinkImpl
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.websocket.WebSocketClient

@Module
@InstallIn(ViewModelComponent::class)
object PeerdroidLinkModule {

    @Provides
    @ViewModelScoped
    internal fun providePeerdroidLink(
        webRtcManager: WebRtcManager,
        webSocketClient: WebSocketClient,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PeerdroidLink = PeerdroidLinkImpl(
        webRtcManager = webRtcManager,
        webSocketClient = webSocketClient,
        applicationScope = applicationScope,
        ioDispatcher = ioDispatcher
    )
}

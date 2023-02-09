package rdx.works.peerdroid.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.data.PeerdroidConnectorImpl
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.WebRtcManagerImpl
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.WebSocketClientImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        // CIO is a fully asynchronous coroutine-based engine
        // that can be used on JVM, Android, and Native platforms
        return HttpClient(CIO) {
            install(Logging) {
//                logger = Logger.ANDROID
//                level = LogLevel.ALL
            } // register logging plugin
            install(WebSockets) // register WebSockets plugin
            install(ContentNegotiation) {
                Json {
                    ignoreUnknownKeys = true
                } // register the JSON serializer
            }
        }
    }

    @Provides
    @Singleton
    internal fun provideWebSocketClient(
        client: HttpClient
    ): WebSocketClient = WebSocketClientImpl(
        client = client,
        // fixes https://radixdlt.atlassian.net/browse/ABW-902
        json = Json {
            ignoreUnknownKeys = true
        }
    )

    @Provides
    @Singleton
    fun providePeerConnectionFactory( // WebRTC PeerConnectionFactory
        @ApplicationContext applicationContext: Context
    ): PeerConnectionFactory {
        val options = PeerConnectionFactory
            .InitializationOptions.builder(applicationContext)
            .setEnableInternalTracer(false) // TODO true or false?
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val eglBase: EglBase = EglBase.create()

        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBase.eglBaseContext,
                    false, // enableIntelVp8Encoder
                    true // enableH264HighProfile
                )
            )
            .setOptions(
                PeerConnectionFactory.Options().apply {
                    disableEncryption = false
                    disableNetworkMonitor = false
                }
            )
            .createPeerConnectionFactory()
    }

    @Provides
    @Singleton
    internal fun provideWebRtcManager(
        peerConnectionFactory: PeerConnectionFactory
    ): WebRtcManager = WebRtcManagerImpl(
        peerConnectionFactory = peerConnectionFactory
    )

    @Provides
    @Singleton
    internal fun providePeerdroidConnector(
        webRtcManager: WebRtcManager,
        webSocketClient: WebSocketClient,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PeerdroidConnector = PeerdroidConnectorImpl(
        webRtcManager = webRtcManager,
        webSocketClient = webSocketClient,
        applicationScope = applicationScope,
        ioDispatcher = ioDispatcher
    )
}

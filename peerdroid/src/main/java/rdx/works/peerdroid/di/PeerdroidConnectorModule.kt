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
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import rdx.works.peerdroid.data.CompositePeerdroidConnector
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.data.PeerdroidConnectorImpl
import rdx.works.peerdroid.data.PeerdroidTransport
import rdx.works.peerdroid.data.RadixConnectTransportSettings
import rdx.works.peerdroid.domain.RadixConnectDiagnostics
import javax.inject.Singleton

/**
 * The Radix Connect singletons. Opaque outside peerdroid: the app gets the [PeerdroidConnector] and the
 * [RadixConnectDiagnostics] interfaces of the same instance.
 */
class RadixConnectComponents internal constructor(
    internal val connector: CompositePeerdroidConnector
)

@Module
@InstallIn(SingletonComponent::class)
object PeerdroidConnectorModule {

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
    @WebRtcTransport
    internal fun provideWebRtcPeerdroidConnector(
        @ApplicationContext applicationContext: Context,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        settings: RadixConnectTransportSettings
    ): PeerdroidConnector = PeerdroidConnectorImpl(
        applicationContext = applicationContext,
        applicationScope = applicationScope,
        ioDispatcher = ioDispatcher,
        // A link that has connected over iroh has a connector extension with the iroh build, which knows them.
        understandsWalletNotices = { connectionId -> connectionId in settings.irohCapableConnectionIds.first() }
    )

    /**
     * The connector the app uses: WebRTC and/or iroh per link, following [RadixConnectTransportSettings].
     */
    @Provides
    @Singleton
    internal fun provideRadixConnectComponents(
        @WebRtcTransport webRtcConnector: PeerdroidConnector,
        @IrohTransport irohConnector: PeerdroidConnector,
        settings: RadixConnectTransportSettings,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): RadixConnectComponents = RadixConnectComponents(
        connector = CompositePeerdroidConnector(
            connectors = mapOf(
                PeerdroidTransport.WEBRTC to webRtcConnector,
                PeerdroidTransport.IROH to irohConnector
            ),
            settings = settings,
            applicationScope = applicationScope,
            ioDispatcher = ioDispatcher
        )
    )

    @Provides
    internal fun providePeerdroidConnector(components: RadixConnectComponents): PeerdroidConnector = components.connector

    @Provides
    internal fun provideRadixConnectDiagnostics(components: RadixConnectComponents): RadixConnectDiagnostics =
        components.connector
}

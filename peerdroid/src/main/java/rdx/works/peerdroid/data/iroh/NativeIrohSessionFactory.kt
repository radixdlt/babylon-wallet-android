package rdx.works.peerdroid.data.iroh

import android.content.Context
import com.radixdlt.connect.iroh.IrohConnectionState
import com.radixdlt.connect.iroh.IrohException
import com.radixdlt.connect.iroh.IrohLogLevel
import com.radixdlt.connect.iroh.IrohLogSink
import com.radixdlt.connect.iroh.IrohPathKind
import com.radixdlt.connect.iroh.IrohRole
import com.radixdlt.connect.iroh.IrohSession
import com.radixdlt.connect.iroh.IrohSessionConfig
import com.radixdlt.connect.iroh.IrohSessionEvent
import com.radixdlt.connect.iroh.RadixConnectIrohAndroid
import com.radixdlt.connect.iroh.irohInstallLogSink
import com.radixdlt.connect.iroh.irohLinkMemoryConnectedLately
import com.radixdlt.connect.iroh.irohPublicRelayUrls
import com.radixdlt.connect.iroh.irohValidateRelayUrl
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.bytes
import rdx.works.peerdroid.BuildConfig
import rdx.works.peerdroid.data.LogThrottle
import rdx.works.peerdroid.data.redactIpAddresses
import rdx.works.peerdroid.domain.IrohPath
import rdx.works.peerdroid.domain.IrohSessionDiagnostics
import rdx.works.peerdroid.domain.MessageNotSentException
import timber.log.Timber

/**
 * [IrohSessionFactory] backed by the radix-connect-iroh native library.
 */
internal class NativeIrohSessionFactory(
    private val applicationContext: Context
) : IrohSessionFactory {

    @Volatile
    private var isLibraryInitialised = false

    // The library's logs are routed to Timber from the first call on, whichever it is.
    override fun publicRelayUrls(): List<String> = withNativeLibrary {
        initialiseLibrary()
        irohPublicRelayUrls()
    }

    override fun validateRelayUrl(relayUrl: String): String? = withNativeLibrary {
        initialiseLibrary()
        irohValidateRelayUrl(relayUrl)
    }

    override fun connectedLately(linkMemory: String): Boolean = withNativeLibrary {
        initialiseLibrary()
        irohLinkMemoryConnectedLately(linkMemory)
    }

    override suspend fun startWalletSession(
        password: RadixConnectPassword,
        relayUrl: String?,
        linkMemory: String?
    ): IrohSessionHandle {
        initialiseLibrary()
        // No relay URL: n0's public relays. A link memory the library cannot read is ignored by it.
        val config = IrohSessionConfig(relayUrl = relayUrl, linkMemory = linkMemory)
        val passwordBytes = password.toByteArray()
        val session = try {
            withNativeLibrary { IrohSession.start(password = passwordBytes, role = IrohRole.WALLET, config = config) }
        } finally {
            // The library copied and parsed the password (and wipes its own copy): do not leave ours around.
            passwordBytes.fill(0)
        }
        return NativeIrohSessionHandle(session = session, ackTimeoutMs = config.ackTimeoutMs.toLong())
    }

    // Once per process, before the first session.
    private fun initialiseLibrary() {
        if (isLibraryInitialised) return
        synchronized(this) {
            if (isLibraryInitialised) return
            withNativeLibrary {
                RadixConnectIrohAndroid.installAndroidContext(applicationContext)
                irohInstallLogSink(
                    sink = TimberIrohLogSink,
                    maxLevel = if (BuildConfig.DEBUG_MODE) IrohLogLevel.DEBUG else IrohLogLevel.WARN
                )
            }
            isLibraryInitialised = true
        }
    }

    // Sargon only exposes the password as a fresh immutable List<UByte>, which cannot be wiped: it is dropped right
    // away. The ByteArray handed to the library is wiped by the caller.
    private fun RadixConnectPassword.toByteArray(): ByteArray {
        val bytes = value.bytes
        return ByteArray(bytes.size) { index -> bytes[index].toByte() }
    }

    /**
     * A native library that cannot be loaded (e.g. a missing ABI) must fail the iroh transport, not crash the app.
     */
    private inline fun <T> withNativeLibrary(block: () -> T): T = try {
        block()
    } catch (error: LinkageError) {
        throw IllegalStateException("the radix-connect-iroh native library is not available", error)
    }
}

/**
 * @param ackTimeoutMs how long the library waits for the peer to confirm a message (its default), for the logs.
 */
private class NativeIrohSessionHandle(
    private val session: IrohSession,
    private val ackTimeoutMs: Long
) : IrohSessionHandle {

    override suspend fun nextEvent(): IrohEvent? = when (val event = session.nextEvent()) {
        null -> null
        is IrohSessionEvent.StateChanged -> IrohEvent.StateChanged(event.state.toIrohConnectionStatus())
        is IrohSessionEvent.Message -> IrohEvent.Message(event.message)
    }

    override fun status(): IrohConnectionStatus = session.state().toIrohConnectionStatus()

    override fun setInBackground(inBackground: Boolean) = session.setInBackground(inBackground)

    // The library holds no secrets in there; its errors may carry IP addresses, which are removed.
    override fun diagnostics(): IrohSessionDiagnostics = session.diagnostics().let { diagnostics ->
        IrohSessionDiagnostics(
            relayUrl = diagnostics.relayUrl,
            usesPublicRelays = diagnostics.publicRelays,
            isRelayConnected = diagnostics.relayConnected,
            relayError = diagnostics.relayError?.redactIpAddresses(),
            path = when (diagnostics.path) {
                IrohPathKind.DIRECT -> IrohPath.DIRECT
                IrohPathKind.RELAY -> IrohPath.RELAY
                null -> null
            },
            rttMs = diagnostics.rttMs?.toLong(),
            failedAttempts = diagnostics.failedAttempts.toInt(),
            lastError = diagnostics.lastError?.redactIpAddresses(),
            connectedForMs = diagnostics.connectedForMs?.toLong(),
            summary = diagnostics.summary.redactIpAddresses()
        )
    }

    override suspend fun send(message: String) {
        try {
            session.send(message)
        } catch (exception: IrohException) {
            throw when (exception) {
                // Nothing was handed to the connection: safe to send over another transport.
                is IrohException.NotConnected,
                is IrohException.Closed -> MessageNotSentException("iroh did not send the message", exception)
                // The peer may have received it.
                is IrohException.Timeout -> MessageNotConfirmedException(limitMs = ackTimeoutMs, cause = exception)
                // Rejected, ConnectionLost: the peer may have received it. MessageTooLarge: nothing was sent, but the
                // connector extension treats it as permanent (failedToPrepareMessage), so no fallback either.
                else -> exception
            }
        }
    }

    override suspend fun networkChanged() {
        session.networkChanged()
    }

    override suspend fun linkMemory(): String = session.linkMemory()

    override suspend fun shutdownAndClose() {
        try {
            session.shutdown()
        } finally {
            session.close()
        }
    }

    private fun IrohConnectionState.toIrohConnectionStatus() = when (this) {
        IrohConnectionState.CONNECTING -> IrohConnectionStatus.CONNECTING
        IrohConnectionState.CONNECTED -> IrohConnectionStatus.CONNECTED
        IrohConnectionState.DISCONNECTED -> IrohConnectionStatus.DISCONNECTED
    }
}

/**
 * Forwards the library's logs to Timber under [IROH_LIBRARY_LOG_TAG], at the same level: in particular the warnings,
 * kept in every build (e.g. another holder of the link's key took the link over). The library never logs passwords,
 * keys or message contents; IP addresses are removed here, and identical lines repeated by the same target are
 * collapsed. Never throws into the library.
 */
internal object TimberIrohLogSink : IrohLogSink {

    private val logThrottle = LogThrottle()

    override fun log(level: IrohLogLevel, target: String, message: String) {
        runCatching {
            logThrottle.log(key = "${level.name}:$target", line = "$target: ${message.redactIpAddresses()}") { line ->
                val tree = Timber.tag(IROH_LIBRARY_LOG_TAG)
                when (level) {
                    IrohLogLevel.ERROR -> tree.e(line)
                    IrohLogLevel.WARN -> tree.w(line)
                    IrohLogLevel.INFO -> tree.i(line)
                    IrohLogLevel.DEBUG -> tree.d(line)
                    IrohLogLevel.TRACE -> tree.v(line)
                }
            }
        }
    }
}

/**
 * The Timber tag of the iroh library's own logs.
 */
internal const val IROH_LIBRARY_LOG_TAG = "RadixConnectIroh"

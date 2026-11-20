package com.babylon.wallet.android.data.repository.radixconnect

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.core.domain.IrohRelay
import rdx.works.core.domain.SavedIrohRelays
import rdx.works.core.preferences.RadixConnectPreferences
import rdx.works.peerdroid.data.iroh.IrohRelayConfig
import javax.inject.Inject

/**
 * An iroh relay server, as listed to the user.
 */
sealed interface IrohRelayServer {

    /**
     * n0's public relays ([IrohRelayConfig.publicRelayUrls]): each end uses its closest one, and finds the other
     * through n0's address lookup. The default: always listed, never removed, nothing to configure.
     */
    data object Public : IrohRelayServer

    data class Custom(val url: String) : IrohRelayServer
}

/**
 * The relay servers: the one in use, then the others (the public relays included when not in use).
 */
data class IrohRelayServers(
    val current: IrohRelayServer,
    val others: List<IrohRelayServer>
)

sealed class IrohRelayServerException(message: String) : Exception(message) {

    class InvalidUrl(val reason: String) : IrohRelayServerException("invalid relay server URL: $reason")

    class AlreadyAdded : IrohRelayServerException("the relay server is already in the list")

    class NotFound : IrohRelayServerException("the relay server is not in the list")

    class PublicCannotBeRemoved : IrohRelayServerException("the public relays cannot be removed")
}

interface IrohRelaysRepository {

    val relayServers: Flow<IrohRelayServers>

    /**
     * Returns the URL to store for [url], or fails with [IrohRelayServerException.InvalidUrl].
     */
    suspend fun validate(url: String): Result<String>

    suspend fun add(url: String): Result<IrohRelayServer.Custom>

    suspend fun update(relayServer: IrohRelayServer.Custom, url: String): Result<IrohRelayServer.Custom>

    /**
     * Removes a custom relay server. When it is the current one, the public relays become current.
     */
    suspend fun remove(relayServer: IrohRelayServer): Result<Unit>

    suspend fun makeCurrent(relayServer: IrohRelayServer): Result<Unit>
}

class IrohRelaysRepositoryImpl @Inject constructor(
    private val radixConnectPreferences: RadixConnectPreferences,
    private val irohRelayConfig: IrohRelayConfig,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : IrohRelaysRepository {

    override val relayServers: Flow<IrohRelayServers> = radixConnectPreferences.irohRelays.map { saved ->
        val current = saved.current
        IrohRelayServers(
            current = current?.let { IrohRelayServer.Custom(it.url) } ?: IrohRelayServer.Public,
            others = listOfNotNull(IrohRelayServer.Public.takeIf { current != null }) +
                saved.others.map { IrohRelayServer.Custom(it.url) }
        )
    }

    override suspend fun validate(url: String): Result<String> = catching {
        val candidate = url.trim()
        // Validated by the library itself, so that the wallet accepts exactly the relays it can use.
        val error = withContext(defaultDispatcher) {
            if (candidate.isEmpty()) "the URL is empty" else irohRelayConfig.validateRelayUrl(candidate)
        }
        // The library's reason starts by saying the URL is invalid, which the message says already.
        if (error != null) throw IrohRelayServerException.InvalidUrl(error.removePrefix(LIBRARY_REASON_PREFIX))
        candidate
    }

    override suspend fun add(url: String): Result<IrohRelayServer.Custom> = validate(url).then { validUrl ->
        radixConnectPreferences.updateIrohRelays { saved ->
            if (saved.contains(validUrl)) throw IrohRelayServerException.AlreadyAdded()
            saved.copy(others = saved.others + IrohRelay(validUrl))
        }
        IrohRelayServer.Custom(validUrl)
    }

    override suspend fun update(relayServer: IrohRelayServer.Custom, url: String): Result<IrohRelayServer.Custom> =
        validate(url).then { validUrl ->
            radixConnectPreferences.updateIrohRelays { saved ->
                if (validUrl != relayServer.url && saved.contains(validUrl)) throw IrohRelayServerException.AlreadyAdded()
                when {
                    saved.current?.url == relayServer.url -> saved.copy(current = IrohRelay(validUrl))
                    saved.others.any { it.url == relayServer.url } -> saved.copy(
                        others = saved.others.map { if (it.url == relayServer.url) IrohRelay(validUrl) else it }
                    )

                    else -> throw IrohRelayServerException.NotFound()
                }
            }
            IrohRelayServer.Custom(validUrl)
        }

    override suspend fun remove(relayServer: IrohRelayServer): Result<Unit> = catching {
        val url = when (relayServer) {
            IrohRelayServer.Public -> throw IrohRelayServerException.PublicCannotBeRemoved()
            is IrohRelayServer.Custom -> relayServer.url
        }
        radixConnectPreferences.updateIrohRelays { saved ->
            if (!saved.contains(url)) throw IrohRelayServerException.NotFound()
            SavedIrohRelays(
                current = saved.current?.takeIf { it.url != url },
                others = saved.others.filter { it.url != url }
            )
        }
    }

    override suspend fun makeCurrent(relayServer: IrohRelayServer): Result<Unit> = catching {
        radixConnectPreferences.updateIrohRelays { saved ->
            val newCurrent = when (relayServer) {
                IrohRelayServer.Public -> null
                is IrohRelayServer.Custom -> IrohRelay(relayServer.url)
            }
            when {
                saved.current == newCurrent -> saved
                newCurrent != null && newCurrent !in saved.others -> throw IrohRelayServerException.NotFound()
                // The previous current relay (when custom) becomes the first of the others.
                else -> SavedIrohRelays(
                    current = newCurrent,
                    others = listOfNotNull(saved.current) + saved.others.filter { it != newCurrent }
                )
            }
        }
    }

    private fun SavedIrohRelays.contains(url: String): Boolean = current?.url == url || others.any { it.url == url }

    // Like runCatching, but lets coroutine cancellation propagate.
    private inline fun <T> catching(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        Result.failure(exception)
    }

    private inline fun <T, R> Result<T>.then(block: (T) -> R): Result<R> = fold(
        onSuccess = { value -> catching { block(value) } },
        onFailure = { error -> Result.failure(error) }
    )
}

private const val LIBRARY_REASON_PREFIX = "invalid relay URL: "

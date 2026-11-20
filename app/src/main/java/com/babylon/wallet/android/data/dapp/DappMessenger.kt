@file:Suppress("LongParameterList")

package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.extensions.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.MessageNotSentException
import timber.log.Timber
import javax.inject.Inject

/**
 * The main responsibility of this class is to
 * - build the (dapp) responses
 * - to send responses to dapp
 *
 */
interface DappMessenger {

    suspend fun sendWalletInteractionResponseFailure(
        remoteConnectorId: String,
        payload: String
    ): Result<Unit>

    /**
     * @param resendIfNotSent whether an answer that was not sent at all is sent again in the background: only when the
     * caller does not act on a failure (see [DappMessengerImpl]).
     */
    suspend fun sendTransactionWriteResponseSuccess(
        remoteConnectorId: String,
        payload: String,
        resendIfNotSent: Boolean = false
    ): Result<Unit>

    suspend fun sendWalletInteractionSuccessResponse(
        remoteConnectorId: String,
        response: WalletToDappInteractionResponse
    ): Result<Unit>
}

class DappMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    private val p2PLinksRepository: P2PLinksRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : DappMessenger {

    override suspend fun sendTransactionWriteResponseSuccess(
        remoteConnectorId: String,
        payload: String,
        resendIfNotSent: Boolean
    ): Result<Unit> = send(remoteConnectorId, payload, resendIfNotSent)

    // The dApp gets a failure either way, and no caller acts on a failure to send it.
    override suspend fun sendWalletInteractionResponseFailure(
        remoteConnectorId: String,
        payload: String
    ): Result<Unit> = send(remoteConnectorId, payload, resendIfNotSent = true)

    override suspend fun sendWalletInteractionSuccessResponse(
        remoteConnectorId: String,
        response: WalletToDappInteractionResponse
    ): Result<Unit> {
        val messageJson = try {
            response.toJson()
        } catch (e: Exception) {
            Timber.d(e)
            ""
        }
        // The caller shows a failure (e.g. a login not recorded): the answer must not arrive later behind its back.
        return send(remoteConnectorId, messageJson, resendIfNotSent = false)
    }

    /**
     * Once the person decided, the answer goes out even if the screen that asked closes meanwhile
     * (it does as soon as they approve or reject). Over iroh a send waits for the connector to
     * confirm it, which takes a round trip: tied to the screen, it was cancelled half way, and the
     * connector never got the answer. Each transport bounds this first attempt, and its result is
     * returned at once: the caller is never held by the attempts that follow.
     *
     * [resendIfNotSent] only where the caller does not act on a failure: a failure answer, and the
     * answer to a transaction already submitted. Where the caller shows the failure (a
     * pre-authorization, a login, a data request), a late copy would reach the dApp after the
     * person saw an error: a signed subintent they may sign again, accounts or a persona the
     * wallet did not record as shared.
     */
    private suspend fun send(remoteConnectorId: String, message: String, resendIfNotSent: Boolean): Result<Unit> {
        val result = withContext(NonCancellable) { peerdroidClient.sendMessage(remoteConnectorId, message) }
        if (resendIfNotSent && result.exceptionOrNull() is MessageNotSentException) {
            resendInBackground(remoteConnectorId, message)
        }
        return result
    }

    /**
     * An answer that was not sent at all ([MessageNotSentException]: e.g. the link was reconnecting)
     * is sent again in the background once the link is open, for up to [RESEND_WINDOW_MS], until the
     * link is removed. Nothing of it reached the connector extension, so the dApp cannot get it
     * twice. An answer that may have been delivered, in whole or in part, is never sent again: the
     * connector extension refuses a second copy of an answer only while it runs, so after it
     * restarted the dApp could get the answer twice. Requests never go through here.
     */
    private fun resendInBackground(remoteConnectorId: String, message: String) {
        applicationScope.launch {
            withTimeoutOrNull(RESEND_WINDOW_MS) {
                val resending = launch { resendUntilSent(remoteConnectorId, message) }
                val linkRemoval = launch {
                    p2PLinksRepository.observeP2PLinks().first { links ->
                        links.asList().none { link -> ConnectionIdHolder(link.connectionPassword).id == remoteConnectorId }
                    }
                    Timber.w("The answer is not sent again: its link was removed")
                    resending.cancel()
                }
                resending.join()
                linkRemoval.cancel()
            }
        }
    }

    private suspend fun resendUntilSent(remoteConnectorId: String, message: String) {
        var attempt = 1
        do {
            delay(RESEND_DELAY_MS)
            peerdroidClient.isConnectionOpen(remoteConnectorId).first { open -> open }
            attempt += 1
            Timber.w("Sending the answer again (attempt $attempt): the last attempt did not send it")
            val failure = peerdroidClient.sendMessage(remoteConnectorId, message).exceptionOrNull()
            if (failure != null && failure !is MessageNotSentException) {
                Timber.w("The answer is not sent again: the last attempt may have delivered it")
                return
            }
        } while (failure != null)
    }

    private companion object {
        const val RESEND_WINDOW_MS = 60_000L
        const val RESEND_DELAY_MS = 1_000L
    }
}

package com.babylon.wallet.android.data.repository.p2plink

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.model.ConnectorExtensionExchangeInteraction
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.P2pTransportProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import rdx.works.peerdroid.data.RadixConnectTransportSettings
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.shortId
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Removals of links, told to the other side so that both forget the link, even when that side is offline.
 *
 * A link removed here is kept as a [RetiredP2PLink] until its connector extension got `linkRemoved` (it may be
 * offline for days, so the notice survives restarts), or for [RetiredP2PLink.NOTICE_LIFETIME] at most, also while
 * the notice waits for the extension; only then is its connector deleted, with the password and the connection.
 * The connector extension does the same when its user forgets the wallet: its `linkRemoved` removes the link here,
 * without telling it back.
 *
 * Only connector extensions known to support iroh are told: the released one has no `linkRemoved`, and expects an
 * interaction id on every wallet message.
 */
@Singleton
class LinkRemovalNotifier @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    private val p2PLinksRepository: P2PLinksRepository,
    private val retiredP2PLinksRepository: RetiredP2PLinksRepository,
    private val transportSettings: RadixConnectTransportSettings,
    private val json: Json
) {

    internal var retryDelay: Duration = RETRY_DELAY

    internal var deliveryGrace: Duration = DELIVERY_GRACE

    private val linkRemovedMessage: String by lazy {
        json.encodeToString<ConnectorExtensionExchangeInteraction>(ConnectorExtensionExchangeInteraction.LinkRemoved)
    }

    /**
     * Tells the connector extension of a retired link: opens the link, sends `linkRemoved` whenever it is connected
     * until it is sent, then deletes its connector. Returns once done, or once the notice expired.
     */
    suspend fun deliver(link: RetiredP2PLink, p2pTransportProfile: P2pTransportProfile, nowMillis: Long = System.currentTimeMillis()) {
        val logId = link.logId()
        if (link.isExpired(nowMillis)) {
            Timber.tag(LOG_TAG).i("🤖 $logId the removal notice expired: deleting the connector untold")
            finish(link)
            return
        }
        if (link.connectorId !in transportSettings.irohCapableConnectionIds.first()) {
            Timber.tag(LOG_TAG).i("🤖 $logId the connector extension does not know linkRemoved: deleting the connector untold")
            finish(link)
            return
        }

        peerdroidClient.connect(p2pTransportProfile, link.connectionPassword).onFailure { error ->
            Timber.tag(LOG_TAG).w("🤖 $logId the removal notice could not open the link: ${error.javaClass.simpleName}")
        }
        // The notice ends with its lifetime, also while it waits: the password and the connection are kept no longer.
        val told = withTimeoutOrNull(link.millisUntilExpiry(nowMillis)) { tellUntilSent(link, logId) }
        if (told == null) {
            Timber.tag(LOG_TAG).i("🤖 $logId the removal notice expired while waiting for the connector: deleting it untold")
        }
        finish(link)
    }

    private suspend fun tellUntilSent(link: RetiredP2PLink, logId: String) {
        while (true) {
            peerdroidClient.isConnectionOpen(link.connectorId).first { it }
            val sent = peerdroidClient.sendMessage(link.connectorId, linkRemovedMessage)
            if (sent.isSuccess) {
                Timber.tag(LOG_TAG).i("🤖 $logId the connector was told that the link was removed")
                // Over WebRTC "sent" is not "received": the connection stays up a moment, so that the
                // message arrives before deleting the connector closes it.
                delay(deliveryGrace)
                return
            }
            Timber.tag(LOG_TAG).w(
                "🤖 $logId the removal notice was not sent (${sent.exceptionOrNull()?.javaClass?.simpleName}): " +
                    "trying again in $retryDelay"
            )
            delay(retryDelay)
        }
    }

    /**
     * The connector extension sent `linkRemoved`. If the link was removed here too, its notice is done; otherwise
     * the link is removed here as well, and returned so that the user can be told.
     */
    suspend fun onLinkRemovedByConnector(connectorId: String): P2pLink? {
        val logId = "link=${ConnectionIdHolder(connectorId).shortId}"
        val retired = retiredP2PLinksRepository.observeRetiredP2PLinks().first().find { it.connectorId == connectorId }
        if (retired != null) {
            Timber.tag(LOG_TAG).i("🤖 $logId removed on both sides: the removal notice is done")
            finish(retired)
            return null
        }
        return p2PLinksRepository.removeP2PLinkRemovedByConnector(connectorId).also { removed ->
            if (removed != null) {
                Timber.tag(LOG_TAG).i("🤖 $logId removed, as its connector forgot this wallet")
            } else {
                Timber.tag(LOG_TAG).w("🤖 $logId the connector removed a link that is not linked here")
            }
        }
    }

    private suspend fun finish(link: RetiredP2PLink) {
        peerdroidClient.deleteLink(link.connectionPassword)
        retiredP2PLinksRepository.forget(link.connectorId)
    }

    private fun RetiredP2PLink.logId() = "link=${ConnectionIdHolder(connectorId).shortId}"

    private companion object {
        const val LOG_TAG = "RadixConnect"
        val RETRY_DELAY = 5.seconds
        val DELIVERY_GRACE = 3.seconds
    }
}

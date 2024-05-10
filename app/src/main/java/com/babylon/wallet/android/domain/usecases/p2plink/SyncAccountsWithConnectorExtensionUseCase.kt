@file:Suppress("LongParameterList")

package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.model.Account
import com.babylon.wallet.android.data.dapp.model.ConnectorExtensionExchangeInteraction
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.radixdlt.sargon.Hash
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.hash
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.profile.data.model.apppreferences.P2PLinkPurpose
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

class SyncAccountsWithConnectorExtensionUseCase @Inject constructor(
    private val p2pLinksRepository: P2PLinksRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val peerdroidConnector: PeerdroidConnector,
    private val peerdroidClient: PeerdroidClient,
    private val preferencesManager: PreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) {

    suspend operator fun invoke(): Flow<Unit> {
        val connectionIdsFlow = peerdroidConnector.peerConnectionStatus
            .filter { connectionStatuses ->
                connectionStatuses.isNotEmpty() &&
                    !connectionStatuses.any { it.value == PeerConnectionStatus.CONNECTING }
            }
            .map { statuses ->
                val generalPurposeConnectionIds = p2pLinksRepository.getP2PLinks()
                    .filter { it.purpose == P2PLinkPurpose.General }
                    .map { it.connectionPassword.value.bytes.hash().hex }
                val openConnectionIds = statuses.filter { it.value == PeerConnectionStatus.OPEN }.keys

                generalPurposeConnectionIds.filter { connectionId -> connectionId in openConnectionIds }
            }
            .filter { connectionIds -> connectionIds.isNotEmpty() }

        val accountListMessageFlow = getProfileUseCase.flow.map {
            val accounts = it.activeAccountsOnCurrentNetwork

            val accountListExchangeInteraction = ConnectorExtensionExchangeInteraction.AccountList(
                accounts = accounts.map { account ->
                    Account(
                        address = account.address.string,
                        label = account.displayName.value,
                        appearanceId = account.appearanceId.value.toInt()
                    )
                }
            )
            json.encodeToString<ConnectorExtensionExchangeInteraction>(accountListExchangeInteraction)
        }

        return combine(connectionIdsFlow, accountListMessageFlow) { connectionIds, accountListMessage ->
            val messageHash = accountListMessage.encodeToByteArray()
                .hash()
                .hex
            val lastSyncedMessageHash = preferencesManager.lastSyncedAccountsWithCE.firstOrNull()
                ?.let { Hash.init(it) }
                ?.hex

            if (messageHash != lastSyncedMessageHash) {
                Timber.d("Accounts sync with CE is required")

                sendAccountListMessage(connectionIds, accountListMessage)
                    .fold(
                        onSuccess = {
                            Timber.d("Successfully synced accounts with CE")
                            preferencesManager.updateLastSyncedAccountsWithCE(messageHash)
                        },
                        onFailure = { throwable ->
                            Timber.e("Failed to sync accounts with CE. Error: ${throwable.message}")
                        }
                    )
            } else {
                Timber.d("Accounts sync triggered, but nothing changed since last sync, ignoring..")
            }
        }.flowOn(ioDispatcher)
    }

    private suspend fun sendAccountListMessage(connectionIds: List<String>, message: String): Result<Unit> {
        return withContext(ioDispatcher) {
            val jobs = connectionIds.map { connectionId ->
                async { peerdroidClient.sendMessage(connectionId, message) }
            }
            val results = jobs.awaitAll()

            if (results.all { it.isSuccess }) {
                Result.success(Unit)
            } else {
                val resultException = results.firstNotNullOfOrNull { it.exceptionOrNull() }
                    ?: Throwable("Failed to send message to all general purpose links")
                Result.failure(resultException)
            }
        }
    }
}

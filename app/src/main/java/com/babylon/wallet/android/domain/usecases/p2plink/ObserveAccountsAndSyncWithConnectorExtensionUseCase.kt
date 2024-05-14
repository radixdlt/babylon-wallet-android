@file:Suppress("LongParameterList")

package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.model.Account
import com.babylon.wallet.android.data.dapp.model.ConnectorExtensionExchangeInteraction
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.radixdlt.sargon.Hash
import com.radixdlt.sargon.RadixConnectPurpose
import com.radixdlt.sargon.extensions.clientID
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.hash
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

class ObserveAccountsAndSyncWithConnectorExtensionUseCase @Inject constructor(
    private val p2pLinksRepository: P2PLinksRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val peerdroidClient: PeerdroidClient,
    private val preferencesManager: PreferencesManager,
    private val json: Json
) {

    suspend operator fun invoke() {
        combine(
            observeOpenGeneralPurposeConnectionIds(),
            observeAccountsOnCurrentNetworkInteractionMessage()
        ) { connectionIds, accountListMessage ->
            connectionIds to accountListMessage
        }.collect { connectionIdsAndMessage ->
            val messageHash = connectionIdsAndMessage.second.encodeToByteArray()
                .hash()
                .hex
            val lastSyncedMessageHash = preferencesManager.lastSyncedAccountsWithCE.firstOrNull()
                ?.let { Hash.init(it) }
                ?.hex

            if (messageHash != lastSyncedMessageHash) {
                Timber.d("Accounts sync with CE is required")

                sendAccountListMessage(connectionIdsAndMessage.first, connectionIdsAndMessage.second)
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
        }
    }

    private fun observeOpenGeneralPurposeConnectionIds(): Flow<Set<String>> {
        return peerdroidClient.openConnectionIds
            .map { openConnectionIds ->
                val generalPurposeConnectionIds = p2pLinksRepository.getP2PLinks(RadixConnectPurpose.GENERAL)
                    .asList()
                    .map { it.clientID().hex }
                generalPurposeConnectionIds.filter { connectionId -> connectionId in openConnectionIds }
                    .toSet()
            }
            .filter { connectionIds -> connectionIds.isNotEmpty() }
    }

    private fun observeAccountsOnCurrentNetworkInteractionMessage(): Flow<String> {
        return getProfileUseCase.flow.map {
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
    }

    private suspend fun sendAccountListMessage(connectionIds: Set<String>, message: String): Result<Unit> {
        val results = connectionIds.map { connectionId ->
            peerdroidClient.sendMessage(connectionId, message)
        }

        return if (results.all { it.isSuccess }) {
            Result.success(Unit)
        } else {
            val resultException = results.firstNotNullOfOrNull { it.exceptionOrNull() }
                ?: Throwable("Failed to send message to all general purpose links")
            Result.failure(resultException)
        }
    }
}

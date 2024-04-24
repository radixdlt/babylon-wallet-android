package com.babylon.wallet.android.domain.usecases.p2plink

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.model.Account
import com.babylon.wallet.android.data.dapp.model.ConnectorExtensionExchangeInteraction
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.radixdlt.sargon.extensions.hex
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import rdx.works.core.decodeHex
import rdx.works.core.hash
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.toHexString
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.activeAccountsOnCurrentNetwork
import timber.log.Timber
import javax.inject.Inject

class SyncAccountsWithConnectorExtensionUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val peerdroidClient: PeerdroidClient,
    private val preferencesManager: PreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Unit> {
        return peerdroidClient.hasAtLeastOneConnection
            .filter { connected -> connected }
            .flatMapMerge { getProfileUseCase.activeAccountsOnCurrentNetwork }
            .map { accounts ->
                val accountListExchangeInteraction = ConnectorExtensionExchangeInteraction.AccountList(
                    accounts = accounts.map { account ->
                        Account(
                            address = account.address,
                            label = account.displayName,
                            appearanceId = account.appearanceID
                        )
                    }
                )
                val message = Serializer.kotlinxSerializationJson.encodeToString(accountListExchangeInteraction)
                val messageHash = message.encodeToByteArray()
                    .hash()
                    .hex
                val lastSyncedMessageHash = preferencesManager.lastSyncedAccountsWithCE.firstOrNull()
                    ?.decodeHex()
                    ?.toHexString()

                if (messageHash != lastSyncedMessageHash) {
                    Timber.d("Accounts sync with CE is required")

                    peerdroidClient.sendMessage(message)
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
            .flowOn(ioDispatcher)
    }
}

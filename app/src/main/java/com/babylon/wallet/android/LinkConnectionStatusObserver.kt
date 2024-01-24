package com.babylon.wallet.android

import androidx.compose.ui.graphics.Color
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.utils.Constants
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import rdx.works.core.preferences.PreferencesManager
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.domain.PeerConnectionStatus
import javax.inject.Inject

class LinkConnectionStatusObserver @Inject constructor(
    peerdroidConnector: PeerdroidConnector,
    preferencesManager: PreferencesManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    val isEnabled = preferencesManager
        .isLinkConnectionStatusIndicatorEnabled
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
            initialValue = true
        )

    val currentStatus = peerdroidConnector
        .peerConnectionStatus
        .map { mapOfPeerConnectionStatus ->
            LinkConnectionsStatus(
                peerConnectionStatus = mapOfPeerConnectionStatus.values.toPersistentList()
            )
        }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
            initialValue = LinkConnectionsStatus()
        )

    data class LinkConnectionsStatus(
        private val peerConnectionStatus: ImmutableList<PeerConnectionStatus> = persistentListOf()
    ) {

        fun currentStatus() = peerConnectionStatus
            .map { state ->
                when (state) {
                    PeerConnectionStatus.OPEN -> {
                        Color.Green
                    }

                    PeerConnectionStatus.CLOSED -> {
                        Color.Red
                    }

                    PeerConnectionStatus.CONNECTING -> {
                        Color.Yellow
                    }
                }
            }
    }
}

package com.babylon.wallet.android.presentation.p2plinksmigration.upgrade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LinkedConnectorsUpgradeViewModel @Inject constructor(
    private val p2PLinksRepository: P2PLinksRepository
) : ViewModel() {

    fun acknowledgeMessage() {
        viewModelScope.launch {
            p2PLinksRepository.acknowledgeP2PLinkMigration()
        }
    }
}
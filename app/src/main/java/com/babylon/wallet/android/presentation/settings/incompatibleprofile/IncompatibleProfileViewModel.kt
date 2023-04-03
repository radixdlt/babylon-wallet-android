package com.babylon.wallet.android.presentation.settings.incompatibleprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.MainEvent
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

@HiltViewModel
class IncompatibleProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val profileDataSource: ProfileDataSource,
    private val peerdroidClient: PeerdroidClient,
) : ViewModel(), OneOffEventHandler<MainEvent> by OneOffEventHandlerImpl() {

    fun deleteProfile() {
        viewModelScope.launch {
            profileDataSource.clear()
            preferencesManager.clear()
            peerdroidClient.terminate()
        }
    }
}

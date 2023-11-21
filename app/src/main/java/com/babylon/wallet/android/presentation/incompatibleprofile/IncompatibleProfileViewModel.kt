package com.babylon.wallet.android.presentation.incompatibleprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.usecases.DeleteWalletUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.domain.DeleteProfileUseCase
import javax.inject.Inject

@HiltViewModel
class IncompatibleProfileViewModel @Inject constructor(
    private val deleteWalletUseCase: DeleteWalletUseCase
) : ViewModel(), OneOffEventHandler<IncompatibleProfileEvent> by OneOffEventHandlerImpl() {

    fun deleteProfile() {
        viewModelScope.launch {
            deleteWalletUseCase()
            sendEvent(IncompatibleProfileEvent.ProfileDeleted)
        }
    }
}

internal sealed interface IncompatibleProfileEvent : OneOffEvent {
    object ProfileDeleted : IncompatibleProfileEvent
}

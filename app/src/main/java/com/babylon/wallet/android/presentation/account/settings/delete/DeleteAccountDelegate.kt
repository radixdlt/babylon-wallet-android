package com.babylon.wallet.android.presentation.account.settings.delete

import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.radixdlt.sargon.Account
import kotlinx.coroutines.flow.update
import javax.inject.Inject

interface DeleteAccountDelegate {

    fun onDeleteAccountRequest()

    fun onDeleteConfirm()

    fun onPickAccountToTransferResources(account: Account)

    fun onSkipAccountToTransferResources()

}

class DeleteAccountDelegateImpl @Inject constructor(

) : DeleteAccountDelegate, ViewModelDelegate<AccountSettingsViewModel.State>() {
    override fun onDeleteAccountRequest() {
        _state.update { it.copy(bottomSheetContent = AccountSettingsViewModel.State.BottomSheetContent.DeleteAccount) }
    }

    override fun onDeleteConfirm() {
        _state.update { it.copy(bottomSheetContent = AccountSettingsViewModel.State.BottomSheetContent.None) }
    }

    override fun onPickAccountToTransferResources(account: Account) {
        TODO("Not yet implemented")
    }

    override fun onSkipAccountToTransferResources() {
        TODO("Not yet implemented")
    }

}
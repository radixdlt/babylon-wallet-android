package com.babylon.wallet.android.presentation.account.settings.delete

import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.radixdlt.sargon.Account
import javax.inject.Inject

interface DeleteAccountDelegate {

    fun onDeleteConfirm()

    fun onPickAccountToTransferResources(account: Account)

    fun onSkipAccountToTransferResources()

}

class DeleteAccountDelegateImpl @Inject constructor(

) : DeleteAccountDelegate, ViewModelDelegate<AccountSettingsViewModel.State>() {

    override fun onDeleteConfirm() {
        TODO("Not yet implemented")
    }

    override fun onPickAccountToTransferResources(account: Account) {
        TODO("Not yet implemented")
    }

    override fun onSkipAccountToTransferResources() {
        TODO("Not yet implemented")
    }

}
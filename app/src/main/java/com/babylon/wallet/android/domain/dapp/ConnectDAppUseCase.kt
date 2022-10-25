package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppAccountUiState
import com.babylon.wallet.android.data.dapp.PersonaEntityUiState
import com.babylon.wallet.android.data.dapp.RequestMethodWalletRequest
import com.babylon.wallet.android.data.profile.Account
import com.babylon.wallet.android.data.profile.PersonaEntity
import com.babylon.wallet.android.domain.profile.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectDAppUseCase @Inject constructor(
    private val dAppRepository: DAppRepository
) {

    suspend operator fun invoke(connectionId: String): DAppRequestPermission {
        val request = dAppRepository.getDAppRequest(connectionId)
        val payloadFields = mutableListOf<String>()
        var payloadAddresses = 0
        request.payload.forEach { payload ->
            when (payload) {
                is RequestMethodWalletRequest.AccountAddressesRequestMethodWalletRequest -> {
                    payload.numberOfAddresses?.let { numberOfAddresses ->
                        payloadAddresses = numberOfAddresses
                    }
                }
                is RequestMethodWalletRequest.PersonaDataRequestMethodWalletRequest -> {
                    payloadFields.addAll(payload.fields)
                }
            }
        }

        // Show first connect request screen if payload.fields is not empty
        // Show personas screen if payload.fields is not empty
        // Show accounts screen if payload.numberOfAddresses greater than 0
        return DAppRequestPermission(payloadFields, payloadAddresses)
    }
}

data class DAppRequestPermission(
    val payloadFields: List<String>,
    val addresses: Int
)
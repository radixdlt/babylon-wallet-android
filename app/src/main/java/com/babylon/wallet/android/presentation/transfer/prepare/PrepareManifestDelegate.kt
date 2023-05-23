package com.babylon.wallet.android.presentation.transfer.prepare

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase

class PrepareManifestDelegate(
    private val state: MutableStateFlow<TransferViewModel.State>,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend fun onSubmit() {
        val currentNetworkId = getProfileUseCase().first().currentNetwork.networkID

        val incomingRequest = MessageFromDataChannel.IncomingRequest.TransactionRequest(
            dappId = "",
            requestId = UUIDGenerator.uuid().toString(),
            transactionManifestData = TransactionManifestData(
                instructions = "",
                version = 0,
                networkId = currentNetworkId
            ),
            requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
                networkId = currentNetworkId,
                origin = "",
                dAppDefinitionAddress = "",
                isInternal = true
            )
        )
        incomingRequestRepository.add(incomingRequest)
    }

}

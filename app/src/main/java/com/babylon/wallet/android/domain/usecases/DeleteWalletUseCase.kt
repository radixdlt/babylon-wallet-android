package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.profile.domain.DeleteProfileUseCase
import javax.inject.Inject

class DeleteWalletUseCase @Inject constructor(
    private val stateRepository: StateRepository,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val peerdroidClient: PeerdroidClient
) {

    suspend operator fun invoke() {
        deleteProfileUseCase()
        peerdroidClient.terminate()
        stateRepository.clearCachedState()
    }
}

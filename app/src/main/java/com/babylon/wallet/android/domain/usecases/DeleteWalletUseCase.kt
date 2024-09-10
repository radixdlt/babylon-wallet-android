package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.core.KeystoreManager
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber
import javax.inject.Inject

class DeleteWalletUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val keystoreManager: KeystoreManager,
    private val googleSignInManager: GoogleSignInManager,
    private val stateRepository: StateRepository,
    private val peerdroidClient: PeerdroidClient,
    private val homeCardsRepository: HomeCardsRepository
) {

    suspend operator fun invoke() {
        googleSignInManager.signOut()
        peerdroidClient.terminate()
        stateRepository.clearCachedState()
        homeCardsRepository.walletReset()
        profileRepository.clearAllWalletData()
        keystoreManager.resetKeySpecs().onFailure {
            Timber.d(it, "Failed to delete encryption keys")
        }
    }
}

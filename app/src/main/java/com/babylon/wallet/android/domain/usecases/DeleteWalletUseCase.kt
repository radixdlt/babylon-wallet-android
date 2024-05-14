package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.core.KeystoreManager
import rdx.works.profile.cloudbackup.GoogleSignInManager
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber
import javax.inject.Inject

class DeleteWalletUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val keystoreManager: KeystoreManager,
    private val googleSignInManager: GoogleSignInManager,
    private val stateRepository: StateRepository,
    private val peerdroidClient: PeerdroidClient
) {

    suspend operator fun invoke() {
        googleSignInManager.signOut()
        googleSignInManager.revokeAccess()
        peerdroidClient.terminate()
        stateRepository.clearCachedState()
        profileRepository.clearAllWalletData()
        keystoreManager.removeKeys().onFailure {
            Timber.d(it, "Failed to delete encryption keys")
        }
    }
}

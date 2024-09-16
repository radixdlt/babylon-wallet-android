package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class DeleteWalletUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val profileRepository: ProfileRepository,
    private val googleSignInManager: GoogleSignInManager,
    private val stateRepository: StateRepository,
    private val peerdroidClient: PeerdroidClient,
    private val homeCardsRepository: HomeCardsRepository,
    private val preferencesManager: PreferencesManager,
) {

    suspend operator fun invoke() {
        val inOnboarding = getProfileUseCase.finishedOnboardingProfile() == null

        peerdroidClient.terminate()
        stateRepository.clearCachedState()
        homeCardsRepository.walletReset()
        profileRepository.deleteWallet()
        if (!inOnboarding) {
            googleSignInManager.signOut()
            preferencesManager.clear()
        }
    }
}

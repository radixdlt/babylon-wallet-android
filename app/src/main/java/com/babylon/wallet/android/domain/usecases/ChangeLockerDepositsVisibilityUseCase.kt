package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.AuthorizedDapp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.changeDAppLockersVisibility
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class ChangeLockerDepositsVisibilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(dApp: AuthorizedDapp, isVisible: Boolean) {
        withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.changeDAppLockersVisibility(dApp, isVisible)
            profileRepository.saveProfile(updatedProfile)
        }
    }
}

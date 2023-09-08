package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.common.asKotlinResult
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

// TODO To remove when mainnet becomes default
class MainnetAvailabilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val networkInfoRepository: NetworkInfoRepository
) {

    fun checkForceToMainnetMandatory() = preferencesManager.getMainnetMigrationOngoing().map { isOngoing ->
        // If profile does not exist yet, then we don't need to force the user. Let onboarding do the trick
        val profile = profileRepository.profile.firstOrNull() ?: return@map false

        isOngoing && !profile.mainnetNetworkExistsWithAccount()
    }

    suspend operator fun invoke() {
        val profileState = profileRepository.profileState.firstOrNull()

        if (profileState == null || profileState !is ProfileState.Restored) {
            if (isMainnetAvailable()) {
                preferencesManager.setMainnetMigrationOngoing(true)
            }
        } else if (!profileState.profile.mainnetNetworkExistsWithAccount()) {
            if (isMainnetAvailable()) {
                preferencesManager.setMainnetMigrationOngoing(true)

                if (profileState.profile.appPreferences.gateways.saved.none { it.network.id == Radix.Gateway.mainnet.network.id }) {
                    profileRepository.saveProfile(profileState.profile.addMainnetGateway())
                }
            }
        }
    }

    suspend fun onMainnetMigrationCompleted() = preferencesManager.setMainnetMigrationOngoing(false)

    fun isMainnetMigrationOngoing() = preferencesManager.getMainnetMigrationOngoing()

    suspend fun isMainnetAvailable() = networkInfoRepository.getMainnetAvailability().asKotlinResult().fold(
        onSuccess = { it },
        onFailure = { false }
    )

    private fun Profile.mainnetNetworkExistsWithAccount() = appPreferences.gateways.saved.any {
        it.network.id == Radix.Network.mainnet.id
    } && networks.find { it.networkID == Radix.Network.mainnet.id }?.accounts?.isNotEmpty() == true

    private fun Profile.addMainnetGateway() = copy(
        appPreferences = appPreferences.copy(gateways = appPreferences.gateways.add(Radix.Gateway.mainnet))
    )
}

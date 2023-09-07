package com.babylon.wallet.android.domain.usecases

import android.content.Context
import android.widget.Toast
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.common.asKotlinResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class MainnetAvailabilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val networkInfoRepository: NetworkInfoRepository,
    @ApplicationContext private val context: Context
) {

    fun forceToMainnetMandatory() = preferencesManager.getMainnetAvailableFlow().map { isAvailable ->
        // If profile does not exist yet, then we don't need to force the user.
        val profile = profileRepository.profile.firstOrNull() ?: return@map false

        isAvailable && !profile.mainnetNetworkExists()
    }

    suspend operator fun invoke() {
        val profileState = profileRepository.profileState.firstOrNull()

        if (profileState == null || profileState !is ProfileState.Restored) {
            if (isMainnetAvailable()) {
                Radix.Gateway.default = Radix.Gateway.mainnet
                preferencesManager.setMainnetAvailable()
                Toast.makeText(context, "Profile does not exist, Mainnet is live", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Profile does not exist, Hammunet is live", Toast.LENGTH_LONG).show()
            }
        } else if (!profileState.profile.mainnetNetworkExists()){
            if (isMainnetAvailable()) {
                Radix.Gateway.default = Radix.Gateway.mainnet
                preferencesManager.setMainnetAvailable()
                profileRepository.saveProfile(profileState.profile.addMainnetGateway())
                Toast.makeText(context, "Profile exists, Mainnet is live", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Profile exists, Hammunet is live", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun isMainnetAvailable() = networkInfoRepository.getMainnetAvailability().asKotlinResult().fold(
        onSuccess = { it },
        onFailure = { false }
    )

    private suspend fun isMainnetKnown() = preferencesManager.getMainnetAvailableFlow().firstOrNull() ?: false

    private suspend fun changeToMainnet(profile: Profile) {
        if (isMainnetAvailable()) {
            Radix.Gateway.default = Radix.Gateway.mainnet
            profileRepository.saveProfile(profile.addMainnetGateway())
        }
    }

    private fun Profile.mainnetNetworkExists() = appPreferences.gateways.saved.any {
        it.network.id == Radix.Network.mainnet.id
    }

    private fun Profile.addMainnetGateway() = copy(
        appPreferences = appPreferences.copy(gateways = appPreferences.gateways.add(Radix.Gateway.mainnet))
    )
}

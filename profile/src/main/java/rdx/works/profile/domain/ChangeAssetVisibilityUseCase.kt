package rdx.works.profile.domain

import com.radixdlt.sargon.AssetAddress
import com.radixdlt.sargon.extensions.AssetPreferences
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.hideAsset
import com.radixdlt.sargon.extensions.unhideAsset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import timber.log.Timber
import javax.inject.Inject

class ChangeAssetVisibilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun hide(assetAddress: AssetAddress) {
        return updateAssetPreferences { assetPreferences ->
            assetPreferences.hideAsset(assetAddress)
        }
    }

    suspend fun unhide(assetAddress: AssetAddress) {
        return updateAssetPreferences { assetPreferences ->
            assetPreferences.unhideAsset(assetAddress)
        }
    }

    private suspend fun updateAssetPreferences(operation: (AssetPreferences) -> AssetPreferences) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.copy(
                appPreferences = profile.appPreferences.copy(
                    assets = runCatching { operation(profile.appPreferences.assets.asIdentifiable()).asList() }
                        .onFailure { Timber.w("Failed to update resource preferences. Error: ${it.message}") }
                        .getOrNull() ?: profile.appPreferences.assets
                )
            )
            profileRepository.saveProfile(updatedProfile)
        }
    }
}

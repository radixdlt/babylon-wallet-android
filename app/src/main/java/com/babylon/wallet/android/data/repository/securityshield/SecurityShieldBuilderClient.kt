package com.babylon.wallet.android.data.repository.securityshield

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.RoleKind
import com.radixdlt.sargon.SecurityShieldBuilder
import com.radixdlt.sargon.SelectedFactorSourcesForRoleStatus
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

@ActivityRetainedScoped
class SecurityShieldBuilderClient @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    private val securityShieldBuilder by lazy { SecurityShieldBuilder() }

    suspend fun getFactorSources(): List<FactorSource> = withContext(dispatcher) {
        val factorSources = profileRepository.profile.first().factorSources
        securityShieldBuilder.sortedFactorSourcesForPrimaryThresholdSelection(factorSources)
    }

    fun updateFactorSourceSelection(id: FactorSourceId, isSelected: Boolean) {
        if (isSelected) {
            securityShieldBuilder.addFactorSourceToPrimaryThreshold(id)
        } else {
            securityShieldBuilder.removeFactorFromPrimary(id)
        }
    }

    fun validateFactorSourceSelection(): SelectedFactorSourcesForRoleStatus {
        return securityShieldBuilder.selectedFactorSourcesForRoleStatus(RoleKind.PRIMARY)
    }
}

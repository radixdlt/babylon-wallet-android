package com.babylon.wallet.android.data.repository.securityshield

import com.babylon.wallet.android.data.repository.securityshield.model.PrimaryRoleSelection
import com.babylon.wallet.android.data.repository.securityshield.model.RecoveryRoleSelection
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.RoleKind
import com.radixdlt.sargon.SecurityShieldBuilder
import com.radixdlt.sargon.SelectedFactorSourcesForRoleStatus
import com.radixdlt.sargon.extensions.id
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

    private lateinit var securityShieldBuilder: SecurityShieldBuilder
    private var allFactorSources = emptyList<FactorSource>()

    suspend fun newSecurityShieldBuilder() {
        securityShieldBuilder = SecurityShieldBuilder()
        allFactorSources = profileRepository.profile.first().factorSources
    }

    suspend fun getSortedPrimaryThresholdFactorSources(): List<FactorSource> = withContext(dispatcher) {
        securityShieldBuilder.sortedFactorSourcesForPrimaryThresholdSelection(allFactorSources)
    }

    suspend fun updatePrimaryRoleThresholdFactorSourceSelection(
        id: FactorSourceId,
        isSelected: Boolean
    ): List<FactorSourceId> = withContext(dispatcher) {
        if (isSelected) {
            securityShieldBuilder.addFactorSourceToPrimaryThreshold(id)
        } else {
            securityShieldBuilder.removeFactorFromPrimary(id)
        }
        securityShieldBuilder.getPrimaryThresholdFactors()
            .also { securityShieldBuilder.setThreshold(it.size.toUByte()) }
    }

    suspend fun validatePrimaryRoleFactorSourceSelection(): SelectedFactorSourcesForRoleStatus = withContext(dispatcher) {
        securityShieldBuilder.selectedFactorSourcesForRoleStatus(RoleKind.PRIMARY)
    }

    suspend fun autoAssignSelectedFactors(): Result<Unit> = withContext(dispatcher) {
        val selectedFactorSourceIds = securityShieldBuilder.getPrimaryThresholdFactors()
        val selectedFactorSources = allFactorSources.filter { it.id in selectedFactorSourceIds }
        runCatching {
            securityShieldBuilder.autoAssignFactorsToRecoveryAndConfirmationBasedOnPrimary(selectedFactorSources)
        }
    }

    suspend fun getPrimaryRoleSelection(): PrimaryRoleSelection = withContext(dispatcher) {
        PrimaryRoleSelection(
            threshold = securityShieldBuilder.getPrimaryThreshold().toInt(),
            thresholdFactors = securityShieldBuilder.getPrimaryThresholdFactors().toFactorSources(),
            overrideFactors = securityShieldBuilder.getPrimaryOverrideFactors().toFactorSources(),
            loginFactor = null // TODO update when login factor support is added to the shield builder in Sargon
        )
    }

    suspend fun getRecoveryRoleSelection(): RecoveryRoleSelection = withContext(dispatcher) {
        RecoveryRoleSelection(
            startRecoveryFactors = securityShieldBuilder.getRecoveryFactors().toFactorSources(),
            confirmationFactors = securityShieldBuilder.getConfirmationFactors().toFactorSources(),
            numberOfDaysUntilAutoConfirm = securityShieldBuilder.getNumberOfDaysUntilAutoConfirm().toInt()
        )
    }

    private fun List<FactorSourceId>.toFactorSources(): List<FactorSource> =
        mapNotNull { id -> allFactorSources.firstOrNull { it.id == id } }
}

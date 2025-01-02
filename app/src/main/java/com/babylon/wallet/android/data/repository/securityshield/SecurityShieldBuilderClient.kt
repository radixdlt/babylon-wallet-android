package com.babylon.wallet.android.data.repository.securityshield

import com.babylon.wallet.android.data.repository.securityshield.model.PrimaryRoleSelection
import com.babylon.wallet.android.data.repository.securityshield.model.RecoveryRoleSelection
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.RoleKind
import com.radixdlt.sargon.SecurityShieldBuilder
import com.radixdlt.sargon.extensions.id
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
@ActivityRetainedScoped
class SecurityShieldBuilderClient @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    private lateinit var securityShieldBuilder: SecurityShieldBuilder
    private var allFactorSources = emptyList<FactorSource>()

    private val primaryRoleSelection = MutableSharedFlow<PrimaryRoleSelection>(1)
    private val recoveryRoleSelection = MutableSharedFlow<RecoveryRoleSelection>(1)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun newSecurityShieldBuilder() {
        securityShieldBuilder = SecurityShieldBuilder()
        allFactorSources = profileRepository.profile.first().factorSources
        primaryRoleSelection.resetReplayCache()
        recoveryRoleSelection.resetReplayCache()
    }

    fun primaryRoleSelection(): Flow<PrimaryRoleSelection> = primaryRoleSelection

    fun recoveryRoleSelection(): Flow<RecoveryRoleSelection> = recoveryRoleSelection

    suspend fun getSortedPrimaryThresholdFactorSources(): List<FactorSource> = withContext(dispatcher) {
        securityShieldBuilder.sortedFactorSourcesForPrimaryThresholdSelection(allFactorSources)
    }

    suspend fun updatePrimaryRoleThresholdFactorSourceSelection(
        id: FactorSourceId,
        isSelected: Boolean
    ) = withContext(dispatcher) {
        val currentFactorCount = (primaryRoleSelection.replayCache.firstOrNull()?.thresholdFactors?.size ?: 0).toUByte()

        if (isSelected) {
            executeMutatingFunction { securityShieldBuilder.addFactorSourceToPrimaryThreshold(id) }
            executeMutatingFunction { securityShieldBuilder.setThreshold(currentFactorCount.inc()) }
        } else {
            executeMutatingFunction { securityShieldBuilder.removeFactorFromPrimary(id) }
            executeMutatingFunction { securityShieldBuilder.setThreshold(currentFactorCount.dec()) }
        }

        onPrimaryRoleSelectionUpdate()
    }

    private fun executeMutatingFunction(function: SecurityShieldBuilder.() -> SecurityShieldBuilder) {
        securityShieldBuilder = securityShieldBuilder.function()
    }

    suspend fun autoAssignSelectedFactors() = withContext(dispatcher) {
        val selectedFactorSourceIds = securityShieldBuilder.getPrimaryThresholdFactors()
        val selectedFactorSources = allFactorSources.filter { it.id in selectedFactorSourceIds }
        executeMutatingFunction { securityShieldBuilder.autoAssignFactorsToRecoveryAndConfirmationBasedOnPrimary(selectedFactorSources) }
        onPrimaryRoleSelectionUpdate()
        onRecoveryRoleSelectionUpdate()
    }

    suspend fun setThreshold(threshold: Int) = withContext(dispatcher) {
        executeMutatingFunction { securityShieldBuilder.setThreshold(threshold.toUByte()) }
        onPrimaryRoleSelectionUpdate()
    }

    suspend fun removeFactorsFromPrimary(ids: List<FactorSourceId>) = withContext(dispatcher) {
        ids.forEach { id -> securityShieldBuilder.removeFactorFromPrimary(id) }
        onPrimaryRoleSelectionUpdate()
    }

    suspend fun removeAuthenticationFactor() = withContext(dispatcher) {
        securityShieldBuilder.setAuthenticationSigningFactor(null)
        onPrimaryRoleSelectionUpdate()
    }

    suspend fun removeFactorFromRecovery(id: FactorSourceId) = withContext(dispatcher) {
        securityShieldBuilder.removeFactorFromRecovery(id)
        onRecoveryRoleSelectionUpdate()
    }

    suspend fun removeFactorFromConfirmation(id: FactorSourceId) = withContext(dispatcher) {
        securityShieldBuilder.removeFactorFromConfirmation(id)
        onRecoveryRoleSelectionUpdate()
    }

    private suspend fun onPrimaryRoleSelectionUpdate() = withContext(dispatcher) {
        primaryRoleSelection.emit(
            PrimaryRoleSelection(
                threshold = securityShieldBuilder.getPrimaryThreshold().toInt(),
                thresholdFactors = securityShieldBuilder.getPrimaryThresholdFactors().toFactorSources(),
                overrideFactors = securityShieldBuilder.getPrimaryOverrideFactors().toFactorSources(),
                authenticationFactor = securityShieldBuilder.getAuthenticationSigningFactor()?.toFactorSource(),
                primaryRoleStatus = securityShieldBuilder.selectedFactorSourcesForRoleStatus(RoleKind.PRIMARY),
                shieldStatus = securityShieldBuilder.validate().also { Timber.w("Security shield builder invalid reason: $it") }
            )
        )
    }

    private suspend fun onRecoveryRoleSelectionUpdate() = withContext(dispatcher) {
        recoveryRoleSelection.emit(
            RecoveryRoleSelection(
                startRecoveryFactors = securityShieldBuilder.getRecoveryFactors().toFactorSources(),
                confirmationFactors = securityShieldBuilder.getConfirmationFactors().toFactorSources(),
                numberOfDaysUntilAutoConfirm = securityShieldBuilder.getNumberOfDaysUntilAutoConfirm().toInt(),
                shieldStatus = securityShieldBuilder.validate()
            )
        )
    }

    private fun List<FactorSourceId>.toFactorSources(): List<FactorSource> =
        mapNotNull { id -> id.toFactorSource() }

    private fun FactorSourceId.toFactorSource(): FactorSource? = allFactorSources.firstOrNull { it.id == this }
}

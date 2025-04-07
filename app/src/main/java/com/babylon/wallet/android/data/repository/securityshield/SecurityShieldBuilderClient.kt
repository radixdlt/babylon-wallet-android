package com.babylon.wallet.android.data.repository.securityshield

import com.babylon.wallet.android.data.repository.securityshield.model.ChooseFactorSourceContext
import com.babylon.wallet.android.data.repository.securityshield.model.PrimaryRoleSelection
import com.babylon.wallet.android.data.repository.securityshield.model.RecoveryRoleSelection
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.FactorSourceValidationStatus
import com.radixdlt.sargon.FactorSourceValidationStatusReasonIfInvalid
import com.radixdlt.sargon.SecurityShieldBuilder
import com.radixdlt.sargon.SecurityShieldBuilderRuleViolation
import com.radixdlt.sargon.SecurityShieldBuilderStatus
import com.radixdlt.sargon.SecurityStructureOfFactorSourceIDs
import com.radixdlt.sargon.extensions.id
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import rdx.works.peerdroid.di.ApplicationScope
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import timber.log.Timber
import javax.inject.Inject

@ActivityRetainedScoped
@Suppress("TooManyFunctions")
class SecurityShieldBuilderClient @Inject constructor(
    profileRepository: ProfileRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    private lateinit var securityShieldBuilder: SecurityShieldBuilder
    private val allFactorSources = profileRepository.profile.map { it.factorSources }.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val primaryRoleSelection = MutableSharedFlow<PrimaryRoleSelection>(1)
    private val recoveryRoleSelection = MutableSharedFlow<RecoveryRoleSelection>(1)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun newSecurityShieldBuilder() {
        securityShieldBuilder = SecurityShieldBuilder()
        primaryRoleSelection.resetReplayCache()
        recoveryRoleSelection.resetReplayCache()
        initSelection()
    }

    fun primaryRoleSelection(): Flow<PrimaryRoleSelection> = primaryRoleSelection

    fun recoveryRoleSelection(): Flow<RecoveryRoleSelection> = recoveryRoleSelection

    suspend fun getSortedPrimaryThresholdFactorSources(): List<FactorSource> = withContext(dispatcher) {
        securityShieldBuilder.sortedFactorSourcesForPrimaryThresholdSelection(allFactorSources.first())
    }

    suspend fun autoAssignFactors() = withContext(dispatcher) {
        executeMutatingFunction {
            securityShieldBuilder.removeAllFactorsFromPrimaryOverride()
                .resetRecoveryAndConfirmationRoleState()
                .autoAssignFactorsToRecoveryAndConfirmationBasedOnPrimary(allFactorSources.first())
        }
    }

    suspend fun buildShield(name: String): SecurityStructureOfFactorSourceIDs = withContext(dispatcher) {
        executeMutatingFunction { securityShieldBuilder.setName(name) }
        securityShieldBuilder.build().also { Timber.w("Shield created: $it") }
    }

    suspend fun getUnusableFactorSourceKinds(
        context: ChooseFactorSourceContext
    ): List<FactorSourceKind> = FactorSourceKind.entries.filterNot { kind ->
        isFactorSourceKindValidOrCanBe(context, kind)
    }

    suspend fun findAlreadySelectedFactorSourceIds(context: ChooseFactorSourceContext): List<FactorSourceId> = withContext(dispatcher) {
        validationForAdditionOfFactorSources(context).mapNotNull { status ->
            status.factorSourceId.takeIf {
                val nonBasicInvalidReason = status.reasonIfInvalid as? FactorSourceValidationStatusReasonIfInvalid.NonBasic
                nonBasicInvalidReason?.v1 is SecurityShieldBuilderRuleViolation.FactorSourceAlreadyPresent
            }
        }
    }

    suspend fun executeMutatingFunction(function: suspend SecurityShieldBuilder.() -> SecurityShieldBuilder) = withContext(dispatcher) {
        securityShieldBuilder = securityShieldBuilder.function()
        initSelection()
    }

    private suspend fun initSelection() {
        val status = securityShieldBuilder.status().also { Timber.w("Security shield builder status: $it") }
        initPrimaryRoleSelection(status)
        initRecoveryAndConfirmationRoleSelection(status)
    }

    private suspend fun initPrimaryRoleSelection(status: SecurityShieldBuilderStatus) = withContext(dispatcher) {
        primaryRoleSelection.emit(
            PrimaryRoleSelection(
                threshold = securityShieldBuilder.getPrimaryThreshold(),
                thresholdValues = securityShieldBuilder.getPrimaryThresholdValues(),
                thresholdFactors = securityShieldBuilder.getPrimaryThresholdFactors().toFactorSources(),
                overrideFactors = securityShieldBuilder.getPrimaryOverrideFactors().toFactorSources(),
                authenticationFactor = securityShieldBuilder.getAuthenticationSigningFactor()?.toFactorSource(),
                primaryRoleStatus = securityShieldBuilder.selectedPrimaryThresholdFactorsStatus(),
                shieldStatus = status
            )
        )
    }

    private suspend fun initRecoveryAndConfirmationRoleSelection(status: SecurityShieldBuilderStatus) = withContext(dispatcher) {
        recoveryRoleSelection.emit(
            RecoveryRoleSelection(
                startRecoveryFactors = securityShieldBuilder.getRecoveryFactors().toFactorSources(),
                confirmationFactors = securityShieldBuilder.getConfirmationFactors().toFactorSources(),
                timePeriodUntilAutoConfirm = securityShieldBuilder.getTimeUntilTimedConfirmationIsCallable(),
                shieldStatus = status
            )
        )
    }

    private suspend fun isFactorSourceKindValidOrCanBe(
        context: ChooseFactorSourceContext,
        kind: FactorSourceKind
    ): Boolean = withContext(dispatcher) {
        when (context) {
            ChooseFactorSourceContext.PrimaryThreshold -> {
                securityShieldBuilder.additionOfFactorSourceOfKindToPrimaryThresholdIsValidOrCanBe(kind)
            }
            ChooseFactorSourceContext.PrimaryOverride -> {
                securityShieldBuilder.additionOfFactorSourceOfKindToPrimaryOverrideIsValidOrCanBe(kind)
            }
            ChooseFactorSourceContext.Recovery -> {
                securityShieldBuilder.additionOfFactorSourceOfKindToRecoveryIsValidOrCanBe(kind)
            }
            ChooseFactorSourceContext.Confirmation -> {
                securityShieldBuilder.additionOfFactorSourceOfKindToConfirmationIsValidOrCanBe(kind)
            }
            ChooseFactorSourceContext.AuthenticationSigning -> {
                securityShieldBuilder.isAllowedFactorSourceKindForAuthenticationSigning(kind)
            }
        }
    }

    private suspend fun validationForAdditionOfFactorSources(context: ChooseFactorSourceContext): List<FactorSourceValidationStatus> {
        val factorSourceIds = allFactorSources.first().map { it.id }

        return when (context) {
            ChooseFactorSourceContext.PrimaryThreshold -> {
                securityShieldBuilder.validationForAdditionOfFactorSourceToPrimaryThresholdForEach(factorSourceIds)
            }
            ChooseFactorSourceContext.PrimaryOverride -> {
                securityShieldBuilder.validationForAdditionOfFactorSourceToPrimaryOverrideForEach(factorSourceIds)
            }
            ChooseFactorSourceContext.Recovery -> {
                securityShieldBuilder.validationForAdditionOfFactorSourceToRecoveryOverrideForEach(factorSourceIds)
            }
            ChooseFactorSourceContext.Confirmation -> {
                securityShieldBuilder.validationForAdditionOfFactorSourceToConfirmationOverrideForEach(factorSourceIds)
            }
            ChooseFactorSourceContext.AuthenticationSigning -> {
                emptyList()
            }
        }
    }

    private fun List<FactorSourceId>.toFactorSources(): List<FactorSource> = mapNotNull { id -> id.toFactorSource() }

    private fun FactorSourceId.toFactorSource(): FactorSource? = allFactorSources.value.firstOrNull { it.id == this }
}

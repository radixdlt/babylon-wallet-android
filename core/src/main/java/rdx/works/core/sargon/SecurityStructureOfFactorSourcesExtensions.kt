package rdx.works.core.sargon

import com.radixdlt.sargon.ConfirmationRoleWithFactorSourceIDs
import com.radixdlt.sargon.ConfirmationRoleWithFactorSources
import com.radixdlt.sargon.MatrixOfFactorSourceIDs
import com.radixdlt.sargon.MatrixOfFactorSources
import com.radixdlt.sargon.PrimaryRoleWithFactorSourceIDs
import com.radixdlt.sargon.PrimaryRoleWithFactorSources
import com.radixdlt.sargon.RecoveryRoleWithFactorSourceIDs
import com.radixdlt.sargon.RecoveryRoleWithFactorSources
import com.radixdlt.sargon.SecurityStructureOfFactorSourceIDs
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.extensions.id

fun SecurityStructureOfFactorSources.toIds() = SecurityStructureOfFactorSourceIDs(
    metadata = metadata,
    matrixOfFactors = matrixOfFactors.toIds(),
    authenticationSigningFactor = authenticationSigningFactor.id
)

fun MatrixOfFactorSources.toIds() = MatrixOfFactorSourceIDs(
    primaryRole = primaryRole.toIds(),
    recoveryRole = recoveryRole.toIds(),
    confirmationRole = confirmationRole.toIds(),
    timeUntilDelayedConfirmationIsCallable = timeUntilDelayedConfirmationIsCallable
)

fun PrimaryRoleWithFactorSources.toIds() = PrimaryRoleWithFactorSourceIDs(
    threshold = threshold,
    thresholdFactors = thresholdFactors.map { it.id },
    overrideFactors = overrideFactors.map { it.id }
)

fun RecoveryRoleWithFactorSources.toIds() = RecoveryRoleWithFactorSourceIDs(
    threshold = threshold,
    thresholdFactors = thresholdFactors.map { it.id },
    overrideFactors = overrideFactors.map { it.id }
)

fun ConfirmationRoleWithFactorSources.toIds() = ConfirmationRoleWithFactorSourceIDs(
    threshold = threshold,
    thresholdFactors = thresholdFactors.map { it.id },
    overrideFactors = overrideFactors.map { it.id }
)

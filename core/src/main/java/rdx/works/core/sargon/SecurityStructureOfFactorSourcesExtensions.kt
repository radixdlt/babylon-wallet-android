package rdx.works.core.sargon

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SecurityStructureOfFactorSources

fun SecurityStructureOfFactorSources.factorSources(): List<FactorSource> =
    (
        matrixOfFactors.primaryRole.thresholdFactors +
            matrixOfFactors.primaryRole.overrideFactors +
            listOf(authenticationSigningFactor) +
            matrixOfFactors.recoveryRole.thresholdFactors +
            matrixOfFactors.confirmationRole.thresholdFactors
        ).toSet().toList()

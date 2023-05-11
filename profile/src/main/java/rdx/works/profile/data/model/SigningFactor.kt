package rdx.works.profile.data.model

import rdx.works.profile.data.model.factorsources.FactorSource

data class SigningFactor(
    val factorSource: FactorSource,
    val signingEntities: List<SigningEntity>
)

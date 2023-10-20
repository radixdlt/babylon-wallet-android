package rdx.works.profile.domain

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.extensions.usesCurve25519
import javax.inject.Inject

class IsAnyEntityCreatedWithOlympiaUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(): Boolean {
        if (getProfileUseCase.isInitialized().not()) return false
        val accounts = getProfileUseCase.accountsOnCurrentNetwork()
        val personas = getProfileUseCase.personasOnCurrentNetwork()
        val olympiaFactorSourceIds = getProfileUseCase.deviceFactorSources.firstOrNull()?.filter {
            it.isOlympia
        }?.map { it.id }.orEmpty()
        val accountsCreatedWithOlympia = accounts.count { it.usesCurve25519() && olympiaFactorSourceIds.contains(it.factorSourceId()) }
        val personasCreatedWithOlympia = personas.count { it.usesCurve25519() && olympiaFactorSourceIds.contains(it.factorSourceId()) }
        return accountsCreatedWithOlympia > 0 || personasCreatedWithOlympia > 0
    }
}

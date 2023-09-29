package rdx.works.profile.domain

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.data.utils.usesCurve25519
import javax.inject.Inject

class CheckAccountsOrPersonasWereCreatedWithOlympia @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(): Boolean {
        if (getProfileUseCase.isInitialized().not()) return false
        val accounts = getProfileUseCase.accountsOnCurrentNetwork()
        val personas = getProfileUseCase.personasOnCurrentNetwork()
        val olympiaFactorSourceIds = getProfileUseCase.deviceFactorSources.firstOrNull()?.filter { it.isOlympia }?.map { it.id }.orEmpty()
        val wrongAccounts = accounts.count { it.usesCurve25519() && olympiaFactorSourceIds.contains(it.factorSourceId()) }
        val wrongPersonas = personas.count { it.usesCurve25519() && olympiaFactorSourceIds.contains(it.factorSourceId()) }
        return wrongAccounts > 1 || wrongPersonas > 1
    }
}

package rdx.works.profile.domain

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.profile.data.model.SeedPhraseLength
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
        // after chat with Alex we figured out that we most of Olympia users used
        // 12 words mnemonic so we will catch most of users that have problem that this use case is suppose to detect
        val olympiaFactorSourceIds = getProfileUseCase.deviceFactorSources.firstOrNull()?.filter {
            it.supportsOlympia && it.hint.mnemonicWordCount < SeedPhraseLength.TWENTY_FOUR.words
        }?.map { it.id }.orEmpty()
        val accountsCreatedWithOlympia = accounts.count { it.usesCurve25519 && olympiaFactorSourceIds.contains(it.factorSourceId) }
        val personasCreatedWithOlympia = personas.count { it.usesCurve25519 && olympiaFactorSourceIds.contains(it.factorSourceId) }
        return accountsCreatedWithOlympia > 0 || personasCreatedWithOlympia > 0
    }
}

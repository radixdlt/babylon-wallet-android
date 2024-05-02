package rdx.works.profile.domain

import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.supportsOlympia
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.usesEd25519
import javax.inject.Inject

class IsAnyEntityCreatedWithOlympiaUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(): Boolean {
        if (getProfileUseCase.isInitialized().not()) return false
        val accounts = getProfileUseCase().activeAccountsOnCurrentNetwork
        val personas = getProfileUseCase().activePersonasOnCurrentNetwork
        // after chat with Alex we figured out that we most of Olympia users used
        // 12 words mnemonic so we will catch most of users that have problem that this use case is suppose to detect
        val olympiaFactorSourceIds = getProfileUseCase().deviceFactorSources.filter {
            it.supportsOlympia && it.value.hint.mnemonicWordCount.value < Bip39WordCount.TWENTY_FOUR.value
        }.map { it.value.id.asGeneral() }
        val accountsCreatedWithOlympia = accounts.count { it.usesEd25519 && it.factorSourceId in olympiaFactorSourceIds }
        val personasCreatedWithOlympia = personas.count { it.usesEd25519 && it.factorSourceId in olympiaFactorSourceIds }
        return accountsCreatedWithOlympia > 0 || personasCreatedWithOlympia > 0
    }
}

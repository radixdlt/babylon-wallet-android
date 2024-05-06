package rdx.works.profile.domain

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.asGeneral
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.supportsOlympia
import rdx.works.core.sargon.usesEd25519
import javax.inject.Inject

class CheckEntitiesCreatedWithOlympiaUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(): BDFSErrorCheckResult {
        if (getProfileUseCase.isInitialized().not()) return BDFSErrorCheckResult()
        val accounts = getProfileUseCase().activeAccountsOnCurrentNetwork
        val personas = getProfileUseCase().activePersonasOnCurrentNetwork
        // after chat with Alex we figured out that we most of Olympia users used
        // 12 words mnemonic so we will catch most of users that have problem that this use case is suppose to detect
        val olympiaFactorSourceIds = getProfileUseCase().deviceFactorSources.filter {
            it.supportsOlympia && it.value.hint.mnemonicWordCount.value < Bip39WordCount.TWENTY_FOUR.value
        }.map { it.value.id.asGeneral() }
        val accountsCreatedWithOlympia = accounts.filter { it.usesEd25519 && it.factorSourceId in olympiaFactorSourceIds }
        val personasCreatedWithOlympia = personas.filter { it.usesEd25519 && it.factorSourceId in olympiaFactorSourceIds }
        return BDFSErrorCheckResult(
            affectedAccounts = accountsCreatedWithOlympia,
            affectedPersonas = personasCreatedWithOlympia
        )
    }
}

data class BDFSErrorCheckResult(
    val affectedAccounts: List<Account> = emptyList(),
    val affectedPersonas: List<Persona> = emptyList()
) {
    val isAnyEntityCreatedWithOlympia: Boolean
        get() = affectedAccounts.isNotEmpty() || affectedPersonas.isNotEmpty()
}

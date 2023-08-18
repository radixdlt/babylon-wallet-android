package com.babylon.wallet.android.domain.usecases

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.factorSourceById
import javax.inject.Inject

class GetAccountsForSecurityPromptUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val mnemonicRepository: MnemonicRepository
) {

    operator fun invoke() = combine(
        getProfileUseCase.accountsOnCurrentNetwork,
        preferencesManager.getBackedUpFactorSourceIds().distinctUntilChanged()
    ) { accounts, backedUpFactorSourceIds ->

        accounts.mapNotNull { account ->
            val factorSourceId = account.factorSourceId() as? FactorSourceID.FromHash ?: return@mapNotNull null
            val factorSource = getProfileUseCase.factorSourceById(factorSourceId) as? DeviceFactorSource ?: return@mapNotNull null

            if (mnemonicRepository.readMnemonic(factorSource.id) == null) {
                AccountWithSecurityPrompt(
                    account = account,
                    prompt = SecurityPromptType.NEEDS_RESTORE
                )
            } else if (!backedUpFactorSourceIds.contains(factorSourceId.body.value)) {
                AccountWithSecurityPrompt(
                    account = account,
                    prompt = SecurityPromptType.NEEDS_BACKUP
                )
            } else {
                null
            }
        }
    }
}

data class AccountWithSecurityPrompt(
    val account: Network.Account,
    val prompt: SecurityPromptType
)

enum class SecurityPromptType {
    NEEDS_BACKUP,
    NEEDS_RESTORE
}

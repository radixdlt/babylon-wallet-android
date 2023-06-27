package com.babylon.wallet.android.domain.usecases

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.factorSourceById
import javax.inject.Inject

class GetAccountsForSecurityPromptUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager
) {

    operator fun invoke() = combine(
        getProfileUseCase.accountsOnCurrentNetwork,
        preferencesManager.getBackedUpFactorSourceIds().distinctUntilChanged()
    ) { accounts, backedUpFactorSourceIds ->

        accounts.filter { account ->
            val factorSourceId = account.factorSourceId() as FactorSourceID.FromHash

            if (backedUpFactorSourceIds.contains(factorSourceId.body.value)) {
                return@filter false
            }

            val factorSource = getProfileUseCase.factorSourceById(factorSourceId) ?: return@filter false
            factorSource.id.kind == FactorSourceKind.DEVICE
        }
    }
}

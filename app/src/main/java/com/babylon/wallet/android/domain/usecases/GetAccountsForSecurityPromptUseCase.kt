package com.babylon.wallet.android.domain.usecases

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.utils.factorSourceId
import javax.inject.Inject

class GetAccountsForSecurityPromptUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val profileRepository: ProfileRepository
) {

    operator fun invoke() = combine(
        profileRepository.profile.map { it.currentNetwork.accounts },
        preferencesManager.getBackedUpFactorSourceIds().distinctUntilChanged()
    ) { accounts, backedUpFactorSourceIds ->
        accounts.filter { account ->
            val factorSourceId = account.factorSourceId()

            if (backedUpFactorSourceIds.contains(factorSourceId.value)) {
                return@filter false
            }

            val factorSource = profileRepository.profile.first().factorSources.find { it.id == factorSourceId } ?: return@filter false
            factorSource.kind == FactorSourceKind.DEVICE
        }
    }
}

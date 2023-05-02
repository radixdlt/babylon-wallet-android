package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.AccountResources
import kotlinx.coroutines.flow.first
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountFactorSourceIDOfDeviceKind
import javax.inject.Inject

class GetFactorSourceStateForAccountUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository
) {
    suspend operator fun invoke(accountAddress: String): AccountResources.FactorSourceState {
        val backedUpFactorSourceIds = preferencesManager.getBackedUpFactorSourceIds().first()
        val accountFactorSourceIDOfDeviceKind = getProfileUseCase.accountFactorSourceIDOfDeviceKind(accountAddress)
        val mnemonic = accountFactorSourceIDOfDeviceKind?.let { mnemonicRepository.readMnemonic(it) }
        val needMnemonicRecovery = accountFactorSourceIDOfDeviceKind != null && mnemonic == null
        val needMnemonicBackup = accountFactorSourceIDOfDeviceKind != null && mnemonic != null &&
            !backedUpFactorSourceIds.contains(accountFactorSourceIDOfDeviceKind.value)
        return when {
            needMnemonicRecovery -> AccountResources.FactorSourceState.NeedMnemonicRecovery
            needMnemonicBackup -> AccountResources.FactorSourceState.NeedMnemonicBackup
            else -> AccountResources.FactorSourceState.Valid
        }
    }
}

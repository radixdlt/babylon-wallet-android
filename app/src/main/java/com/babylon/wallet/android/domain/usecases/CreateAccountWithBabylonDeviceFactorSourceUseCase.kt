package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.ResolveAccountsLedgerStateRepository
import com.babylon.wallet.android.data.transaction.InteractionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.mainBabylonFactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import javax.inject.Inject

class CreateAccountWithBabylonDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val profileRepository: ProfileRepository,
    private val resolveAccountsLedgerStateRepository: ResolveAccountsLedgerStateRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private val _interactionState = MutableStateFlow<InteractionState?>(null)
    val interactionState: Flow<InteractionState?> = _interactionState.asSharedFlow()

    suspend operator fun invoke(displayName: String): Network.Account {
        return withContext(defaultDispatcher) {
            val profile = ensureBabylonFactorSourceExistUseCase()
            val factorSource = profile.mainBabylonFactorSource() ?: error("Babylon factor source is not present")
            _interactionState.update { InteractionState.Device.DerivingAccounts(factorSource) }

            val mnemonicWithPassphrase = requireNotNull(mnemonicRepository.readMnemonic(factorSource.id).getOrNull())

            val newAccount = profile.currentNetwork.createAccountWithBabylon(
                displayName = displayName,
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                deviceFactorSource = factorSource,
                onLedgerSettings = Network.Account.OnLedgerSettings.init()
            )
            val resolveResult = resolveAccountsLedgerStateRepository.invoke(listOf(newAccount))
            // Add account to the profile
            val accountToAdd = if (resolveResult.isSuccess) {
                resolveResult.getOrThrow().first().account
            } else {
                newAccount
            }
            val updatedProfile = profile.addAccounts(
                accounts = listOf(accountToAdd),
                onNetwork = NetworkId.from(profile.currentNetwork.networkID)
            )
            // Save updated profile
            profileRepository.saveProfile(updatedProfile)
            _interactionState.update { null }
            // Return new account
            accountToAdd
        }
    }
}

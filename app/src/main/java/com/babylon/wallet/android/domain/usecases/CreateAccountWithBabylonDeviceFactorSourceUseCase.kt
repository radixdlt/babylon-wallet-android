package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.ResolveAccountsLedgerStateRepository
import com.babylon.wallet.android.data.transaction.InteractionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.mainBabylonFactorSource
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.Companion.initAccountWithBabylonDeviceFactorSource
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.model.pernetwork.nextAccountIndex
import rdx.works.profile.data.model.pernetwork.nextAppearanceId
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

    suspend operator fun invoke(
        displayName: String,
        networkID: NetworkId? = null
    ): Network.Account {
        return withContext(defaultDispatcher) {
            val profile = ensureBabylonFactorSourceExistUseCase()
            val factorSource = profile.mainBabylonFactorSource()
                ?: error("Babylon factor source is not present")
            _interactionState.update { InteractionState.Device.DerivingAccounts(factorSource) }

            // Construct new account
            val networkId = networkID ?: profile.currentNetwork?.knownNetworkId ?: Radix.Gateway.default.network.networkId()
            val nextAccountIndex = profile.nextAccountIndex(DerivationPathScheme.CAP_26, networkId, factorSource.id)
            val nextAppearanceId = profile.nextAppearanceId(networkId)
            val mnemonicWithPassphrase = requireNotNull(mnemonicRepository.readMnemonic(factorSource.id).getOrNull())
            val newAccount = initAccountWithBabylonDeviceFactorSource(
                entityIndex = nextAccountIndex,
                displayName = displayName,
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                deviceFactorSource = factorSource,
                networkId = networkId,
                appearanceID = nextAppearanceId
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
                onNetwork = networkId
            )
            // Save updated profile
            profileRepository.saveProfile(updatedProfile)
            _interactionState.update { null }
            // Return new account
            accountToAdd
        }
    }
}

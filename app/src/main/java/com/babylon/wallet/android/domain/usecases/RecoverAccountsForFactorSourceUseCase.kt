package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.domain.model.AccountWithOnLedgerStatus
import com.babylon.wallet.android.presentation.account.recover.RecoveryFactorSource
import com.babylon.wallet.android.utils.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import rdx.works.core.UUIDGenerator
import rdx.works.core.then
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.derivationPathEntityIndex
import rdx.works.profile.data.model.pernetwork.usedAccountDerivationIndices
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.IoDispatcher
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class RecoverAccountsForFactorSourceUseCase @Inject constructor(
    private val resolveAccountsLedgerStateUseCase: ResolveAccountsLedgerStateUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    @IoDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private var nextDerivationPathOffset: Int = 0

    private val _interactionState = MutableStateFlow<InteractionState?>(null)
    val interactionState: Flow<InteractionState?> = _interactionState.asSharedFlow()

    fun reset() {
        nextDerivationPathOffset = 0
    }

    suspend operator fun invoke(
        recoveryFS: RecoveryFactorSource
    ): Result<List<AccountWithOnLedgerStatus>> {
        return withContext(defaultDispatcher) {
            _interactionState.update { null }
            val profile = if (getProfileUseCase.isInitialized()) getProfileUseCase.invoke().firstOrNull() else null
            val networkId = profile?.currentNetwork?.knownNetworkId ?: Radix.Gateway.mainnet.network.networkId()
            val indicesToScan =
                computeIndicesToScan(
                    derivationPathScheme = recoveryFS.derivationPathScheme,
                    networkId = networkId,
                    factorSourceID = recoveryFS.factorSourceId,
                    profile = profile
                )
            val derivedAccounts = when (recoveryFS) {
                is RecoveryFactorSource.Device -> {
                    deriveAccountsForDeviceFactorSource(recoveryFS, indicesToScan, networkId)
                }

                is RecoveryFactorSource.Ledger -> {
                    val derivedAccountsResult = derivedAccountsForLedger(recoveryFS, indicesToScan, networkId)
                    if (derivedAccountsResult.isSuccess) {
                        derivedAccountsResult.getOrNull().orEmpty()
                    } else {
                        _interactionState.update { null }
                        return@withContext Result.failure(
                            derivedAccountsResult.exceptionOrNull() ?: Exception("Failed to derive public keys with ledger")
                        )
                    }
                }

                is RecoveryFactorSource.VirtualDeviceFactorSource -> {
                    deriveAccountsForVirtualDeviceFactorSource(recoveryFS, indicesToScan, networkId)
                }
            }
            val resolvedAccounts = resolveAccountsLedgerStateUseCase(derivedAccounts)
            nextDerivationPathOffset = indicesToScan.last() + 1
            _interactionState.update { null }
            return@withContext resolvedAccounts
        }
    }

    private suspend fun derivedAccountsForLedger(
        recoveryFS: RecoveryFactorSource.Ledger,
        indicesToScan: Set<Int>,
        networkId: NetworkId
    ): Result<List<Network.Account>> {
        _interactionState.update { InteractionState.Ledger.DerivingPublicKey(recoveryFS.factorSource) }
        val curve = if (recoveryFS.isOlympia) Curve.Secp256k1 else Curve.Curve25519
        val derivationPaths = indicesToScan.map { index ->
            if (recoveryFS.isOlympia) {
                DerivationPath.forLegacyOlympia(index)
            } else {
                DerivationPath.forAccount(networkId, index, KeyType.TRANSACTION_SIGNING)
            }
        }
        val derivedAccountsResult = ledgerMessenger.sendDerivePublicKeyRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = derivationPaths.map { LedgerInteractionRequest.KeyParameters(curve, it.path) },
            ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(recoveryFS.factorSource)
        ).mapCatching { derivePublicKeyResponse ->
            derivePublicKeyResponse.publicKeysHex
        }.then { derivedPublicKeys ->
            val accounts = derivedPublicKeys.map { derivedPublicKey ->
                val derivationPath = derivationPaths.first { it.path == derivedPublicKey.derivationPath }
                Network.Account.initAccountWithLedgerFactorSource(
                    entityIndex = derivationPath.derivationPathEntityIndex(),
                    displayName = Constants.DEFAULT_ACCOUNT_NAME,
                    networkId = networkId,
                    appearanceID = derivationPath.derivationPathEntityIndex() % AccountGradientList.count(),
                    ledgerFactorSource = recoveryFS.factorSource,
                    derivedPublicKeyHex = derivedPublicKey.publicKeyHex,
                    derivationPath = derivationPath,
                    isOlympia = recoveryFS.isOlympia
                )
            }
            Result.success(accounts)
        }
        return derivedAccountsResult
    }

    private fun deriveAccountsForVirtualDeviceFactorSource(
        recoveryFS: RecoveryFactorSource.VirtualDeviceFactorSource,
        indicesToScan: Set<Int>,
        networkId: NetworkId
    ): List<Network.Account> {
        _interactionState.update { InteractionState.Device.DerivingAccounts(recoveryFS.virtualDeviceFactorSource) }
        return indicesToScan.map { index ->
            Network.Account.initAccountWithBabylonDeviceFactorSource(
                entityIndex = index,
                displayName = Constants.DEFAULT_ACCOUNT_NAME,
                mnemonicWithPassphrase = recoveryFS.mnemonicWithPassphrase,
                deviceFactorSource = recoveryFS.virtualDeviceFactorSource,
                networkId = networkId,
                appearanceID = index % AccountGradientList.count()
            )
        }
    }

    private fun deriveAccountsForDeviceFactorSource(
        recoveryFS: RecoveryFactorSource.Device,
        indicesToScan: Set<Int>,
        networkId: NetworkId
    ): List<Network.Account> {
        _interactionState.update { InteractionState.Device.DerivingAccounts(recoveryFS.factorSource) }
        val mnemonic = recoveryFS.mnemonicWithPassphrase
        return indicesToScan.map { index ->
            if (recoveryFS.isOlympia) {
                Network.Account.initAccountWithOlympiaDeviceFactorSource(
                    entityIndex = index,
                    displayName = Constants.DEFAULT_ACCOUNT_NAME,
                    mnemonicWithPassphrase = mnemonic,
                    deviceFactorSource = recoveryFS.factorSource,
                    networkId = networkId,
                    appearanceID = index % AccountGradientList.count(),
                )
            } else {
                Network.Account.initAccountWithBabylonDeviceFactorSource(
                    entityIndex = index,
                    displayName = Constants.DEFAULT_ACCOUNT_NAME,
                    mnemonicWithPassphrase = recoveryFS.mnemonicWithPassphrase,
                    deviceFactorSource = recoveryFS.factorSource,
                    networkId = networkId,
                    appearanceID = index % AccountGradientList.count()
                )
            }
        }
    }

    private fun computeIndicesToScan(
        derivationPathScheme: DerivationPathScheme,
        profile: Profile?,
        networkId: NetworkId,
        factorSourceID: FactorSource.FactorSourceID
    ): Set<Int> {
        val usedIndices = profile?.usedAccountDerivationIndices(derivationPathScheme, networkId, factorSourceID).orEmpty()
        val indicesToScan = mutableSetOf<Int>()
        val startIndex = nextDerivationPathOffset
        var currentIndex = startIndex
        do {
            if (currentIndex !in usedIndices) {
                indicesToScan.add(currentIndex)
            }
            currentIndex++
        } while (indicesToScan.size < accountsPerScanPage)
        return indicesToScan
    }

    companion object {
        private const val accountsPerScanPage = 50
    }
}

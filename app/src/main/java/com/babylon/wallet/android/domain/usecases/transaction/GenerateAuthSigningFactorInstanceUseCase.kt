package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.Cap26Path
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.UUIDGenerator
import rdx.works.core.mapError
import rdx.works.core.sargon.authenticationSigningFactorInstance
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.derivePublicKey
import rdx.works.core.sargon.factorSourceById
import rdx.works.core.sargon.toAccountAuthSigningDerivationPath
import rdx.works.core.sargon.toAuthSigningDerivationPath
import rdx.works.core.sargon.toIdentityAuthSigningDerivationPath
import rdx.works.core.sargon.transactionSigningFactorInstance
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

class GenerateAuthSigningFactorInstanceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val ledgerMessenger: LedgerMessenger
) {

    private val _interactionState = MutableStateFlow<InteractionState?>(null)
    val interactionState: Flow<InteractionState?> = _interactionState.asSharedFlow()

    suspend operator fun invoke(entity: ProfileEntity): Result<HierarchicalDeterministicFactorInstance> {
        if (entity.securityState.authenticationSigningFactorInstance != null) {
            return Result.failure(ProfileException.AuthenticationSigningAlreadyExist(entity))
        }

        val transactionSigning = entity.securityState.transactionSigningFactorInstance
        val authSigningDerivationPath = when (val path = transactionSigning.publicKey.derivationPath) {
            is DerivationPath.Bip44Like -> {
                val networkId = getProfileUseCase().currentGateway.network.id
                when (entity) {
                    is ProfileEntity.AccountEntity -> path.value.toAccountAuthSigningDerivationPath(networkId = networkId)
                    is ProfileEntity.PersonaEntity -> path.value.toIdentityAuthSigningDerivationPath(networkId = networkId)
                }
            }
            is DerivationPath.Cap26 -> when (val cap26Path = path.value) {
                is Cap26Path.Account -> cap26Path.toAuthSigningDerivationPath()
                is Cap26Path.Identity -> cap26Path.toAuthSigningDerivationPath()
                is Cap26Path.GetId -> error("Entity should not contain Identity Path")
            }
        }

        val factorSourceId = transactionSigning.factorSourceId.asGeneral()
        return when (val factorSource = requireNotNull(getProfileUseCase().factorSourceById(factorSourceId))) {
            is FactorSource.Device -> createAuthSigningFactorInstanceForDevice(factorSource, authSigningDerivationPath)
            is FactorSource.Ledger -> createAuthSigningFactorInstanceForLedger(
                factorSource, authSigningDerivationPath
            )
        }
    }

    private suspend fun createAuthSigningFactorInstanceForLedger(
        ledgerHardwareWalletFactorSource: FactorSource.Ledger,
        authSigningDerivationPath: DerivationPath
    ): Result<HierarchicalDeterministicFactorInstance> {
        _interactionState.update {
            InteractionState.Ledger.DerivingPublicKey(ledgerHardwareWalletFactorSource)
        }
        val deriveResult = ledgerMessenger.sendDerivePublicKeyRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = listOf(
                LedgerInteractionRequest.KeyParameters(
                    curve = Curve.Curve25519, // TODO integration
                    derivationPath = authSigningDerivationPath.string
                )
            ),
            ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(ledgerHardwareWalletFactorSource)
        ).mapCatching { derivePublicKeyResponse ->
            derivePublicKeyResponse.publicKeysHex.first().publicKeyHex
        }
        return deriveResult.mapCatching { hex ->
            HierarchicalDeterministicFactorInstance(
                factorSourceId = ledgerHardwareWalletFactorSource.value.id,
                publicKey = HierarchicalDeterministicPublicKey(
                    publicKey = PublicKey.Ed25519.init(hex = hex), // TODO integration,
                    derivationPath = authSigningDerivationPath
                )
            )
        }.onSuccess {
            _interactionState.update { null }
        }.onFailure {
            _interactionState.update { null }
        }.mapError {
            RadixWalletException.LedgerCommunicationException.FailedToDerivePublicKeys
        }
    }

    private suspend fun createAuthSigningFactorInstanceForDevice(
        deviceFactorSource: FactorSource.Device,
        authSigningDerivationPath: DerivationPath
    ): Result<HierarchicalDeterministicFactorInstance> {
        val mnemonic = mnemonicRepository.readMnemonic(deviceFactorSource.value.id.asGeneral()).getOrNull()
        requireNotNull(mnemonic)
        val authSigningPublicKey = mnemonic.derivePublicKey(derivationPath = authSigningDerivationPath) // TODO integration
        return Result.success(
            HierarchicalDeterministicFactorInstance(
                factorSourceId = deviceFactorSource.value.id,
                publicKey = HierarchicalDeterministicPublicKey(
                    publicKey = authSigningPublicKey,
                    derivationPath = authSigningDerivationPath
                )
            )
        )
    }
}

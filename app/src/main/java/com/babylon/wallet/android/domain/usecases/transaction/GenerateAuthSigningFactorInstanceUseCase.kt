package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.Cap26Path
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.string
import rdx.works.core.UUIDGenerator
import rdx.works.core.mapError
import rdx.works.core.sargon.authenticationSigningFactorInstance
import rdx.works.core.sargon.currentGateway
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
                factorSource,
                authSigningDerivationPath
            )
            else -> Result.failure(IllegalStateException("FactorSourceKind ${factorSource.kind} not supported."))
        }
    }

    private suspend fun createAuthSigningFactorInstanceForLedger(
        ledgerHardwareWalletFactorSource: FactorSource.Ledger,
        authSigningDerivationPath: DerivationPath
    ): Result<HierarchicalDeterministicFactorInstance> {
        val deriveResult = ledgerMessenger.sendDerivePublicKeyRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = listOf(
                LedgerInteractionRequest.KeyParameters(
                    curve = Curve.Curve25519, // To whoever works on this feature in the future, this curve should not
                    // be 25519 hardcoded. (Concluded when scouting this part of code during sargon integration)
                    // We have to calculate the curve from the derivation path. This feature is not yet
                    // used by the public so it can stay as a reference.
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
                    publicKey = PublicKey.Ed25519.init(hex = hex), // To whoever works on this please read the above statement
                    derivationPath = authSigningDerivationPath
                )
            )
        }.mapError {
            RadixWalletException.LedgerCommunicationException.FailedToDerivePublicKeys
        }
    }

    private suspend fun createAuthSigningFactorInstanceForDevice(
        deviceFactorSource: FactorSource.Device,
        authSigningDerivationPath: DerivationPath
    ): Result<HierarchicalDeterministicFactorInstance> {
        return mnemonicRepository.readMnemonic(deviceFactorSource.value.id.asGeneral()).mapCatching { mnemonic ->
            val authSigningHDPublicKey = mnemonic.derivePublicKey(path = authSigningDerivationPath)
            HierarchicalDeterministicFactorInstance(
                factorSourceId = deviceFactorSource.value.id,
                publicKey = authSigningHDPublicKey
            )
        }
    }
}

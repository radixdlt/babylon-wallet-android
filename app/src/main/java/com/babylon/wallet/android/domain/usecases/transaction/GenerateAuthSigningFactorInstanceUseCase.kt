package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.utils.getLedgerDeviceModel
import com.radixdlt.extensions.removeLeadingZero
import kotlinx.coroutines.flow.first
import rdx.works.core.UUIDGenerator
import rdx.works.core.toHexString
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.factorSource
import javax.inject.Inject

class GenerateAuthSigningFactorInstanceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val ledgerMessenger: LedgerMessenger
) {

    suspend operator fun invoke(entity: Entity): Result<FactorInstance> {
        val factorSourceId: FactorSource.ID
        val authSigningDerivationPath = when (val securityState = entity.securityState) {
            is SecurityState.Unsecured -> {
                if (securityState.unsecuredEntityControl.authenticationSigning != null) {
                    throw ProfileException.AuthenticationSigningAlreadyExist(entity)
                }
                val transactionSigning = securityState.unsecuredEntityControl.transactionSigning
                val signingEntityDerivationPath = transactionSigning.derivationPath
                requireNotNull(signingEntityDerivationPath)
                factorSourceId = transactionSigning.factorSourceId
                if (transactionSigning.publicKey.curve == Slip10Curve.CURVE_25519) {
                    DerivationPath.authSigningDerivationPathFromCap26Path(signingEntityDerivationPath)
                } else {
                    val profile = getProfileUseCase.invoke().first()
                    val networkId = requireNotNull(profile.currentNetwork.knownNetworkId)
                    DerivationPath.authSigningDerivationPathFromBip44LikePath(networkId, signingEntityDerivationPath)
                }
            }
        }
        val factorSource = requireNotNull(getProfileUseCase.factorSource(factorSourceId))
        return when (factorSource.kind) {
            FactorSourceKind.DEVICE -> {
                createAuthSigningFactorInstanceForDevice(factorSource, authSigningDerivationPath)
            }

            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> {
                createAuthSigningFactorInstanceForLedger(factorSource, authSigningDerivationPath)
            }
        }
    }

    private suspend fun createAuthSigningFactorInstanceForLedger(
        factorSource: FactorSource,
        authSigningDerivationPath: DerivationPath
    ): Result<FactorInstance> {
        val deviceModel = requireNotNull(factorSource.getLedgerDeviceModel())
        val deriveResult = ledgerMessenger.sendDerivePublicKeyRequest(
            UUIDGenerator.uuid().toString(),
            listOf(DerivePublicKeyRequest.KeyParameters(Curve.Curve25519, authSigningDerivationPath.path)),
            DerivePublicKeyRequest.LedgerDevice(
                factorSource.label,
                deviceModel,
                factorSource.id.value
            )
        ).mapCatching {
            it.publicKeysHex.first().publicKeyHex
        }
        return if (deriveResult.isSuccess) {
            Result.success(
                FactorInstance(
                    authSigningDerivationPath,
                    factorSource.id,
                    FactorInstance.PublicKey.curve25519PublicKey(deriveResult.getOrThrow())
                )
            )
        } else {
            Result.failure(DappRequestFailure.LedgerCommunicationFailure.FailedToDerivePublicKeys)
        }
    }

    private suspend fun createAuthSigningFactorInstanceForDevice(
        factorSource: FactorSource,
        authSigningDerivationPath: DerivationPath
    ): Result<FactorInstance> {
        val mnemonic = mnemonicRepository.readMnemonic(factorSource.id)
        requireNotNull(mnemonic)
        val authSigningPublicKey = mnemonic.compressedPublicKey(
            curve = Slip10Curve.CURVE_25519,
            derivationPath = authSigningDerivationPath
        ).removeLeadingZero().toHexString()
        return Result.success(
            FactorInstance(
                authSigningDerivationPath,
                factorSource.id,
                FactorInstance.PublicKey.curve25519PublicKey(authSigningPublicKey)
            )
        )
    }
}

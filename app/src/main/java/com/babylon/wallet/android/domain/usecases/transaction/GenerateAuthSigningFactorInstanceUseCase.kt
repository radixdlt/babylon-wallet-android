package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel.Companion.getLedgerDeviceModel
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.radixdlt.extensions.removeLeadingZero
import kotlinx.coroutines.flow.first
import rdx.works.core.UUIDGenerator
import rdx.works.core.toHexString
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.factorSourceById
import javax.inject.Inject

class GenerateAuthSigningFactorInstanceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val ledgerMessenger: LedgerMessenger
) {

    suspend operator fun invoke(entity: Entity): Result<FactorInstance> {
        val factorSourceId: FactorSourceID.FromHash
        val authSigningDerivationPath = when (val securityState = entity.securityState) {
            is SecurityState.Unsecured -> {
                if (securityState.unsecuredEntityControl.authenticationSigning != null) {
                    throw ProfileException.AuthenticationSigningAlreadyExist(entity)
                }
                val transactionSigning = securityState.unsecuredEntityControl.transactionSigning
                val signingEntityDerivationPath = transactionSigning.derivationPath
                requireNotNull(signingEntityDerivationPath)
                factorSourceId = transactionSigning.factorSourceId as FactorSourceID.FromHash
                if (transactionSigning.publicKey.curve == Slip10Curve.CURVE_25519) {
                    DerivationPath.authSigningDerivationPathFromCap26Path(signingEntityDerivationPath)
                } else {
                    val profile = getProfileUseCase.invoke().first()
                    val networkId = requireNotNull(profile.currentNetwork.knownNetworkId)
                    DerivationPath.authSigningDerivationPathFromBip44LikePath(networkId, signingEntityDerivationPath)
                }
            }
        }
        val factorSource = requireNotNull(getProfileUseCase.factorSourceById(factorSourceId))
        return when (factorSource.id.kind) {
            FactorSourceKind.DEVICE -> {
                createAuthSigningFactorInstanceForDevice(factorSource as DeviceFactorSource, authSigningDerivationPath)
            }

            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> {
                createAuthSigningFactorInstanceForLedger(
                    factorSource as LedgerHardwareWalletFactorSource,
                    authSigningDerivationPath
                )
            }

            FactorSourceKind.OFF_DEVICE_MNEMONIC -> Result.failure(Throwable("factor source is neither device nor ledger"))
            FactorSourceKind.TRUSTED_CONTACT -> Result.failure(Throwable("factor source is neither device nor ledger"))
        }
    }

    private suspend fun createAuthSigningFactorInstanceForLedger(
        ledgerHardwareWalletFactorSource: LedgerHardwareWalletFactorSource,
        authSigningDerivationPath: DerivationPath
    ): Result<FactorInstance> {
        val deviceModel = requireNotNull(ledgerHardwareWalletFactorSource.getLedgerDeviceModel())
        val deriveResult = ledgerMessenger.sendDerivePublicKeyRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = listOf(
                DerivePublicKeyRequest.KeyParameters(
                    Curve.Curve25519,
                    authSigningDerivationPath.path
                )
            ),
            ledgerDevice = DerivePublicKeyRequest.LedgerDevice(
                name = ledgerHardwareWalletFactorSource.hint.name,
                model = deviceModel,
                id = ledgerHardwareWalletFactorSource.id.body.value
            )
        ).mapCatching { derivePublicKeyResponse ->
            derivePublicKeyResponse.publicKeysHex.first().publicKeyHex
        }
        return if (deriveResult.isSuccess) {
            Result.success(
                FactorInstance(
                    derivationPath = authSigningDerivationPath,
                    factorSourceId = ledgerHardwareWalletFactorSource.id,
                    publicKey = FactorInstance.PublicKey.curve25519PublicKey(deriveResult.getOrThrow())
                )
            )
        } else {
            Result.failure(DappRequestFailure.LedgerCommunicationFailure.FailedToDerivePublicKeys)
        }
    }

    private suspend fun createAuthSigningFactorInstanceForDevice(
        deviceFactorSource: DeviceFactorSource,
        authSigningDerivationPath: DerivationPath
    ): Result<FactorInstance> {
        val mnemonic = mnemonicRepository.readMnemonic(deviceFactorSource.id)
        requireNotNull(mnemonic)
        val authSigningPublicKey = mnemonic.compressedPublicKey(
            curve = Slip10Curve.CURVE_25519,
            derivationPath = authSigningDerivationPath
        ).removeLeadingZero().toHexString()
        return Result.success(
            FactorInstance(
                derivationPath = authSigningDerivationPath,
                factorSourceId = deviceFactorSource.id,
                publicKey = FactorInstance.PublicKey.curve25519PublicKey(authSigningPublicKey)
            )
        )
    }
}

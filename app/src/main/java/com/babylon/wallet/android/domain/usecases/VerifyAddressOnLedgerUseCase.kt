package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.factorSourceById
import javax.inject.Inject

class VerifyAddressOnLedgerUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger
) {

    @Suppress("ReturnCount")
    suspend operator fun invoke(address: String): Result<Unit> {
        val account = getProfileUseCase.accountOnCurrentNetwork(
            withAddress = address
        ) ?: return Result.failure(Exception("No account with address: $address"))

        val factorInstance = (account.securityState as? SecurityState.Unsecured)?.unsecuredEntityControl?.transactionSigning
        if (factorInstance == null || factorInstance.factorSourceId.kind != FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET) {
            return Result.failure(Exception("Account $address is not a Ledger backed account"))
        }

        val ledgerFactorSource = (getProfileUseCase.factorSourceById(factorInstance.factorSourceId) as? LedgerHardwareWalletFactorSource)
            ?: return Result.failure(Exception("Ledger factor source not found for $factorInstance"))

        val ledgerFactorInstanceBadge = (factorInstance.badge as? FactorInstance.Badge.VirtualSource.HierarchicalDeterministic)
            ?: return Result.failure(Exception("Factor source is not a virtual badge"))

        return ledgerMessenger.deriveAndDisplayAddressRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = LedgerInteractionRequest.KeyParameters(
                curve = Curve.from(ledgerFactorInstanceBadge.publicKey.curve),
                derivationPath = ledgerFactorInstanceBadge.derivationPath.path
            ),
            ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(ledgerFactorSource)
        ).fold(
            onSuccess = { response ->
                if (response.derivedAddress.derivedKey.publicKeyHex != ledgerFactorInstanceBadge.publicKey.compressedData) {
                    return Result.failure(Exception("Public key does not match the derived pub key from Ledger"))
                }

                if (response.derivedAddress.address != account.address) {
                    return Result.failure(Exception("Account address does not match the derived address from Ledger"))
                }

                Result.success(Unit)
            },
            onFailure = { Result.failure(it) }
        )
    }
}

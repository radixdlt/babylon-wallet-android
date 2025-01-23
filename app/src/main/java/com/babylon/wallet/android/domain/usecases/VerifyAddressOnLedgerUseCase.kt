package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.string
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.factorSourceById
import rdx.works.core.sargon.transactionSigningFactorInstance
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class VerifyAddressOnLedgerUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger
) {

    @Suppress("ReturnCount")
    suspend operator fun invoke(address: AccountAddress): Result<Unit> {
        val profile = getProfileUseCase()
        val account = profile.activeAccountOnCurrentNetwork(
            withAddress = address
        ) ?: return Result.failure(Exception("No account with address: $address"))

        val factorInstance = account.securityState.transactionSigningFactorInstance
        if (factorInstance.factorSourceId.kind != FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET) {
            return Result.failure(Exception("Account $address is not a Ledger backed account"))
        }

        val ledgerFactorSource = (profile.factorSourceById(factorInstance.factorSourceId.asGeneral()) as? FactorSource.Ledger)
            ?: return Result.failure(Exception("Ledger factor source not found for $factorInstance"))

        return ledgerMessenger.deriveAndDisplayAddressRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = LedgerInteractionRequest.KeyParameters.from(factorInstance.publicKey.derivationPath),
            ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(ledgerFactorSource)
        ).fold(
            onSuccess = { response ->
                if (response.derivedAddress.derivedKey.publicKeyHex != factorInstance.publicKey.publicKey.hex) {
                    return Result.failure(Exception("Public key does not match the derived pub key from Ledger"))
                }

                if (response.derivedAddress.address != account.address.string) {
                    return Result.failure(Exception("Account address does not match the derived address from Ledger"))
                }

                Result.success(Unit)
            },
            onFailure = { Result.failure(it) }
        )
    }
}

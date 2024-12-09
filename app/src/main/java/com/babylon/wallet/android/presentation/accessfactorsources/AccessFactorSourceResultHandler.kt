package com.babylon.wallet.android.presentation.accessfactorsources

import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.SecureStorageKey
import com.radixdlt.sargon.os.driver.BiometricsFailure
import rdx.works.profile.domain.ProfileException

sealed interface AccessFactorSourceError {

    data class Fatal(
        val commonException: CommonException
    ) : AccessFactorSourceError

    data class NonFatal(
        val ledgerError: RadixWalletException.LedgerCommunicationException
    ) : AccessFactorSourceError

    companion object {

        fun from(
            error: Throwable,
            factorSourceId: FactorSourceIdFromHash
        ) = when (error) {
            // Error received from BiometricsHandler
            is BiometricsFailure -> Fatal(
                commonException = error.toCommonException(SecureStorageKey.DeviceFactorSourceMnemonic(factorSourceId = factorSourceId))
            )
            // Error received from MnemonicRepository
            is ProfileException.SecureStorageAccess -> Fatal(
                commonException = CommonException.SecureStorageReadException()
            )

            is RadixWalletException.LedgerCommunicationException.FailedToSignTransaction -> {
                if (error.reason == LedgerErrorCode.UserRejectedSigningOfTransaction) {
                    Fatal(
                        commonException = CommonException.SigningRejected()
                    )
                } else {
                    NonFatal(ledgerError = error)
                }
            }

            // Any other fatal error is resolved as rejected
            else -> Fatal(
                commonException = CommonException.SigningRejected()
            )
        }

    }
}
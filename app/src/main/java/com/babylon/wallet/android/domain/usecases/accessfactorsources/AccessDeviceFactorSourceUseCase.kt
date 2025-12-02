package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.signedAuth
import com.babylon.wallet.android.presentation.accessfactorsources.signedSubintent
import com.babylon.wallet.android.presentation.accessfactorsources.signedTransaction
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.PerFactorSourceInputOfAuthIntent
import com.radixdlt.sargon.PerFactorSourceInputOfSubintent
import com.radixdlt.sargon.PerFactorSourceInputOfTransactionIntent
import com.radixdlt.sargon.SecureStorageKey
import com.radixdlt.sargon.SpotCheckInput
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.getAuthSignatures
import com.radixdlt.sargon.extensions.getSubintentSignatures
import com.radixdlt.sargon.extensions.getTransactionSignatures
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.spotCheck
import com.radixdlt.sargon.os.driver.BiometricsFailure
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.UpdateFactorSourceLastUsedUseCase
import javax.inject.Inject

class AccessDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val updateFactorSourceLastUsedUseCase: UpdateFactorSourceLastUsedUseCase
) : AccessFactorSource<FactorSource.Device> {

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.Device,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> = readMnemonic(factorSourceId = factorSource.value.id)
        .mapCatching { mnemonicWithPassphrase ->
            input.derivationPaths.map { derivationPath ->
                HierarchicalDeterministicFactorInstance(
                    factorSourceId = factorSource.value.id,
                    publicKey = mnemonicWithPassphrase.derivePublicKey(path = derivationPath)
                )
            }
        }.onSuccess {
            updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
        }

    override suspend fun signMono(
        factorSource: FactorSource.Device,
        input: AccessFactorSourcesInput.Sign
    ): Result<AccessFactorSourcesOutput.Sign> {
        return readMnemonic(factorSourceId = factorSource.value.id)
            .mapCatching { mnemonic ->
                when (input) {
                    is AccessFactorSourcesInput.SignTransaction -> mnemonic.signTransaction(input.input)
                    is AccessFactorSourcesInput.SignSubintent -> mnemonic.signSubintent(input.input)
                    is AccessFactorSourcesInput.SignAuth -> mnemonic.signAuth(input.input)
                }
            }.onSuccess {
                updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
            }
    }

    override suspend fun spotCheck(factorSource: FactorSource.Device): Result<Boolean> = readMnemonic(
        factorSourceId = factorSource.value.id
    ).mapCatching { mnemonicWithPassphrase ->
        factorSource.spotCheck(
            input = SpotCheckInput.Software(mnemonicWithPassphrase = mnemonicWithPassphrase)
        )
    }.onSuccess {
        updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
    }

    private suspend fun readMnemonic(factorSourceId: FactorSourceIdFromHash): Result<MnemonicWithPassphrase> {
        if (!mnemonicRepository.mnemonicExist(key = factorSourceId.asGeneral())) {
            return Result.failure(CommonException.UnableToLoadMnemonicFromSecureStorage(badValue = factorSourceId.body.hex))
        }

        return mnemonicRepository.readMnemonic(key = factorSourceId.asGeneral())
            .mapError { error ->
                when (error) {
                    is BiometricsFailure -> error.toCommonException(
                        key = SecureStorageKey.DeviceFactorSourceMnemonic(factorSourceId)
                    )

                    ProfileException.NoMnemonic -> CommonException.UnableToLoadMnemonicFromSecureStorage(
                        badValue = factorSourceId.body.hex
                    )

                    ProfileException.SecureStorageAccess -> CommonException.SecureStorageReadException(error.toString())
                    else -> CommonException.Unknown("Device factor source access error: $error")
                }
            }
    }
}

fun MnemonicWithPassphrase.signTransaction(
    input: PerFactorSourceInputOfTransactionIntent
): AccessFactorSourcesOutput.Sign = AccessFactorSourcesOutput.Sign.signedTransaction(
    factorSourceId = input.factorSourceId,
    signatures = getTransactionSignatures(input)
)

fun MnemonicWithPassphrase.signSubintent(
    input: PerFactorSourceInputOfSubintent
): AccessFactorSourcesOutput.Sign = AccessFactorSourcesOutput.Sign.signedSubintent(
    factorSourceId = input.factorSourceId,
    signatures = getSubintentSignatures(input)
)

fun MnemonicWithPassphrase.signAuth(
    input: PerFactorSourceInputOfAuthIntent
): AccessFactorSourcesOutput.Sign = AccessFactorSourcesOutput.Sign.signedAuth(
    factorSourceId = input.factorSourceId,
    signatures = getAuthSignatures(input)
)

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
import com.radixdlt.sargon.extensions.isManualCancellation
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.spotCheck
import com.radixdlt.sargon.os.driver.BiometricsFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    ): Result<List<HierarchicalDeterministicFactorInstance>> = loadMnemonic(factorSource)
        .mapError { it.toAccessError(factorSource.value.id) }
        .mapCatching { mnemonicWithPassphrase ->
            derivePublicKeys(factorSource, input, mnemonicWithPassphrase).getOrThrow()
        }
        .rethrowCancellation()

    suspend fun derivePublicKeys(
        factorSource: FactorSource.Device,
        input: KeyDerivationRequestPerFactorSource,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<List<HierarchicalDeterministicFactorInstance>> = runCatching {
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
        return loadMnemonic(factorSource)
            .mapError { it.toAccessError(factorSource.value.id) }
            .mapCatching { mnemonic ->
                signMono(factorSource, input, mnemonic).getOrThrow()
            }
            .rethrowCancellation()
    }

    suspend fun signMono(
        factorSource: FactorSource.Device,
        input: AccessFactorSourcesInput.Sign,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<AccessFactorSourcesOutput.Sign> = runCatching {
        when (input) {
            is AccessFactorSourcesInput.SignTransaction -> mnemonicWithPassphrase.signTransaction(input.input)
            is AccessFactorSourcesInput.SignSubintent -> mnemonicWithPassphrase.signSubintent(input.input)
            is AccessFactorSourcesInput.SignAuth -> mnemonicWithPassphrase.signAuth(input.input)
        }
    }.onSuccess {
        updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
    }

    override suspend fun spotCheck(factorSource: FactorSource.Device): Result<Boolean> = loadMnemonic(factorSource)
        .mapError { it.toAccessError(factorSource.value.id) }
        .mapCatching { mnemonicWithPassphrase ->
            spotCheck(factorSource, mnemonicWithPassphrase).getOrThrow()
        }
        .rethrowCancellation()

    suspend fun spotCheck(
        factorSource: FactorSource.Device,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<Boolean> = runCatching {
        factorSource.spotCheck(
            input = SpotCheckInput.Software(mnemonicWithPassphrase = mnemonicWithPassphrase)
        )
    }.onSuccess {
        updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
    }

    suspend fun loadMnemonic(factorSource: FactorSource.Device): Result<MnemonicWithPassphrase> {
        val factorSourceId = factorSource.value.id
        val mnemonicExists = try {
            mnemonicRepository.mnemonicExist(key = factorSourceId.asGeneral())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return Result.failure(DeviceMnemonicLoadError.ReadFailure(error))
        }
        currentCoroutineContext().ensureActive()
        if (!mnemonicExists) {
            return Result.failure(DeviceMnemonicLoadError.Missing)
        }

        val result = mnemonicRepository.readMnemonic(key = factorSourceId.asGeneral())
            .rethrowCancellation()
        currentCoroutineContext().ensureActive()
        return result
            .mapError { error ->
                when (error) {
                    is BiometricsFailure -> {
                        val accessError = error.toCommonException(
                            key = SecureStorageKey.DeviceFactorSourceMnemonic(factorSourceId)
                        )
                        if (
                            accessError is CommonException.SecureStorageAccessException &&
                            accessError.errorKind.isManualCancellation()
                        ) {
                            DeviceMnemonicLoadError.CancelledByUser(accessError)
                        } else {
                            DeviceMnemonicLoadError.ReadFailure(accessError)
                        }
                    }

                    ProfileException.NoMnemonic -> DeviceMnemonicLoadError.Missing

                    ProfileException.SecureStorageAccess -> DeviceMnemonicLoadError.ReadFailure(
                        CommonException.SecureStorageReadException(error.toString())
                    )

                    else -> DeviceMnemonicLoadError.ReadFailure(
                        CommonException.Unknown("Device factor source access error: $error")
                    )
                }
            }
    }

    suspend fun saveMnemonic(
        factorSource: FactorSource.Device,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<Unit> {
        val result = mnemonicRepository.saveMnemonic(
            key = factorSource.value.id.asGeneral(),
            mnemonicWithPassphrase = mnemonicWithPassphrase
        ).rethrowCancellation()
        currentCoroutineContext().ensureActive()
        return result
    }
}

sealed class DeviceMnemonicLoadError(cause: Throwable? = null) : Exception(cause) {
    data object Missing : DeviceMnemonicLoadError()

    class CancelledByUser(cause: Throwable) : DeviceMnemonicLoadError(cause)

    class ReadFailure(cause: Throwable) : DeviceMnemonicLoadError(cause)
}

private fun Throwable.toAccessError(factorSourceId: FactorSourceIdFromHash): Throwable = when (this) {
    DeviceMnemonicLoadError.Missing -> CommonException.UnableToLoadMnemonicFromSecureStorage(
        badValue = factorSourceId.body.hex
    )

    is DeviceMnemonicLoadError.CancelledByUser,
    is DeviceMnemonicLoadError.ReadFailure -> cause ?: this

    else -> this
}

private fun <T> Result<T>.rethrowCancellation(): Result<T> = onFailure { error ->
    if (error is CancellationException) throw error
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

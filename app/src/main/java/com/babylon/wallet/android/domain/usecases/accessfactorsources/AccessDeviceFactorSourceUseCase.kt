package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.SecureStorageKey
import com.radixdlt.sargon.SpotCheckInput
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.spotCheck
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.os.driver.BiometricsFailure
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import rdx.works.core.sargon.signInteractorInput
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.UpdateFactorSourceLastUsedUseCase
import javax.inject.Inject

class AccessDeviceFactorSourceUseCase @Inject constructor(
    private val biometricsAuthenticateUseCase: BiometricsAuthenticateUseCase,
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
        input: PerFactorSourceInput<out Signable.Payload, out Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> = readMnemonic(factorSourceId = factorSource.value.id)
        .mapCatching { mnemonic ->
            PerFactorOutcome(
                factorSourceId = input.factorSourceId,
                outcome = FactorOutcome.Signed(mnemonic.signInteractorInput(input))
            )
        }.onSuccess {
            updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
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

        return biometricsAuthenticateUseCase
            .asResult()
            .then {
                mnemonicRepository.readMnemonic(key = factorSourceId.asGeneral())
            }.mapError { error ->
                when (error) {
                    is BiometricsFailure -> error.toCommonException(
                        key = SecureStorageKey.DeviceFactorSourceMnemonic(factorSourceId)
                    )

                    ProfileException.NoMnemonic -> CommonException.UnableToLoadMnemonicFromSecureStorage(
                        badValue = factorSourceId.body.hex
                    )

                    ProfileException.SecureStorageAccess -> CommonException.SecureStorageReadException()
                    else -> CommonException.Unknown()
                }
            }
    }
}

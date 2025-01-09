package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.SecureStorageKey
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.sign
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.os.driver.BiometricsFailure
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.HdSignature
import com.radixdlt.sargon.os.signing.HdSignatureInput
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import rdx.works.core.sargon.updateLastUsed
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

class SignWithDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val profileRepository: ProfileRepository,
    private val biometricsAuthenticateUseCase: BiometricsAuthenticateUseCase,
) {

    /**
     * Guarantees to return a `CommonException` in case of an error
     */
    suspend fun mono(
        deviceFactorSource: FactorSource.Device,
        input: PerFactorSourceInput<Signable.Payload, Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> {
        if (!mnemonicRepository.mnemonicExist(key = deviceFactorSource.value.id.asGeneral())) {
            return Result.failure(
                CommonException.UnableToLoadMnemonicFromSecureStorage(
                    badValue = deviceFactorSource.value.id.body.hex
                )
            )
        }

        return biometricsAuthenticateUseCase.asResult().then {
            mnemonicRepository.readMnemonic(
                key = deviceFactorSource.value.id.asGeneral()
            )
        }.mapError { error ->
            when (error) {
                is BiometricsFailure -> error.toCommonException(
                    key = SecureStorageKey.DeviceFactorSourceMnemonic(deviceFactorSource.value.id)
                )
                ProfileException.NoMnemonic -> CommonException.UnableToLoadMnemonicFromSecureStorage(
                    badValue = deviceFactorSource.value.id.body.hex
                )
                ProfileException.SecureStorageAccess -> CommonException.SecureStorageReadException()
                else -> CommonException.Unknown()
            }
        }.mapCatching { mnemonic ->
            PerFactorOutcome(
                factorSourceId = input.factorSourceId,
                outcome = FactorOutcome.Signed(mnemonic.sign(input))
            )
        }.onSuccess {
            val updatedProfile = getProfileUseCase().updateLastUsed(deviceFactorSource.id)
            profileRepository.saveProfile(updatedProfile)
        }
    }

    private fun MnemonicWithPassphrase.sign(
        input: PerFactorSourceInput<Signable.Payload, Signable.ID>,
    ) = input.perTransaction.map { perTransaction ->
        perTransaction.ownedFactorInstances.map { perFactorInstance ->
            val signatureWithPublicKey = sign(
                hash = perTransaction.payload.getSignable().hash(),
                path = perFactorInstance.factorInstance.publicKey.derivationPath
            )

            HdSignature(
                input = HdSignatureInput(
                    payloadId = perTransaction.payload.getSignable().getId(),
                    ownedFactorInstance = perFactorInstance
                ),
                signature = signatureWithPublicKey
            )
        }
    }.flatten()

    /**
     * Guarantees to return a `CommonException` in case of an error
     */
    suspend fun poly(
        deviceFactorSources: List<FactorSource.Device>,
        inputs: List<PerFactorSourceInput<Signable.Payload, Signable.ID>>
    ): Result<List<PerFactorOutcome<Signable.ID>>> {
        deviceFactorSources.forEach { deviceFactorSource ->
            if (!mnemonicRepository.mnemonicExist(key = deviceFactorSource.value.id.asGeneral())) {
                return Result.failure(
                    CommonException.UnableToLoadMnemonicFromSecureStorage(
                        badValue = deviceFactorSource.value.id.body.hex
                    )
                )
            }
        }

        return biometricsAuthenticateUseCase
            .asResult()
            .mapCatching {
                deviceFactorSources.associate { deviceFactorSource ->
                    val mnemonic = mnemonicRepository.readMnemonic(
                        key = deviceFactorSource.value.id.asGeneral()
                    ).mapError { error ->
                        val commonException = when (error) {
                            ProfileException.NoMnemonic -> CommonException.UnableToLoadMnemonicFromSecureStorage(
                                badValue = deviceFactorSource.value.id.body.hex
                            )
                            ProfileException.SecureStorageAccess -> CommonException.SecureStorageReadException()
                            else -> CommonException.Unknown()
                        }
                        return Result.failure(commonException)
                    }.getOrThrow()

                    deviceFactorSource.value.id to mnemonic
                }
            }.map { deviceFactorSourcesWithMnemonics ->
                inputs.map { input ->
                    val mnemonic = deviceFactorSourcesWithMnemonics.getValue(input.factorSourceId)

                    PerFactorOutcome(
                        factorSourceId = input.factorSourceId,
                        outcome = FactorOutcome.Signed(mnemonic.sign(input))
                    )
                }
            }.mapError { error ->
                when (error) {
                    is BiometricsFailure -> error.toCommonException(
                        key = SecureStorageKey.DeviceFactorSourceMnemonic(deviceFactorSources.first().value.id)
                    )
                    else -> error
                }
            }
    }
}

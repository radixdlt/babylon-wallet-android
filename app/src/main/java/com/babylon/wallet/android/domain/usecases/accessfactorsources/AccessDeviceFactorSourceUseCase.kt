package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.payloadId
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.FactorOutcomeOfSubintentHash
import com.radixdlt.sargon.FactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.HdSignatureInputOfAuthIntentHash
import com.radixdlt.sargon.HdSignatureInputOfSubintentHash
import com.radixdlt.sargon.HdSignatureInputOfTransactionIntentHash
import com.radixdlt.sargon.HdSignatureOfAuthIntentHash
import com.radixdlt.sargon.HdSignatureOfSubintentHash
import com.radixdlt.sargon.HdSignatureOfTransactionIntentHash
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.PerFactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.PerFactorOutcomeOfSubintentHash
import com.radixdlt.sargon.PerFactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.PerFactorSourceInputOfAuthIntent
import com.radixdlt.sargon.PerFactorSourceInputOfSubintent
import com.radixdlt.sargon.PerFactorSourceInputOfTransactionIntent
import com.radixdlt.sargon.SecureStorageKey
import com.radixdlt.sargon.SpotCheckInput
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.decompile
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.sign
import com.radixdlt.sargon.extensions.spotCheck
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.os.driver.BiometricsFailure
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import com.radixdlt.sargon.os.signing.into
import com.radixdlt.sargon.os.signing.intoSargon
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
        input: AccessFactorSourcesInput.Sign
    ): Result<AccessFactorSourcesOutput.Signing> {
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

    fun MnemonicWithPassphrase.signTransaction(
        input: PerFactorSourceInputOfTransactionIntent
    ): AccessFactorSourcesOutput.Signing {
        val signatures =  input.perTransaction.map { transaction ->
            val payloadId = transaction.payloadId()

            transaction.ownedFactorInstances.map { instance ->
                val signatureWithPublicKey = sign(
                    hash = payloadId.hash,
                    path = instance.factorInstance.publicKey.derivationPath
                )

                HdSignatureOfTransactionIntentHash(
                    input = HdSignatureInputOfTransactionIntentHash(
                        payloadId = payloadId,
                        ownedFactorInstance = instance
                    ),
                    signature = signatureWithPublicKey
                )
            }
        }
            .flatten()

        return AccessFactorSourcesOutput.SignTransaction(
            outcome = PerFactorOutcomeOfTransactionIntentHash(
                factorSourceId = input.factorSourceId,
                outcome = FactorOutcomeOfTransactionIntentHash.Signed(signatures)
            )
        )
    }

    fun MnemonicWithPassphrase.signSubintent(
        input: PerFactorSourceInputOfSubintent
    ): AccessFactorSourcesOutput.Signing {
        val signatures =  input.perTransaction.map { transaction ->
            val payloadId = transaction.payloadId()

            transaction.ownedFactorInstances.map { instance ->
                val signatureWithPublicKey = sign(
                    hash = payloadId.hash,
                    path = instance.factorInstance.publicKey.derivationPath
                )

                HdSignatureOfSubintentHash(
                    input = HdSignatureInputOfSubintentHash(
                        payloadId = payloadId,
                        ownedFactorInstance = instance
                    ),
                    signature = signatureWithPublicKey
                )
            }
        }
            .flatten()

        return AccessFactorSourcesOutput.SignSubintent(
            outcome = PerFactorOutcomeOfSubintentHash(
                factorSourceId = input.factorSourceId,
                outcome = FactorOutcomeOfSubintentHash.Signed(signatures)
            )
        )
    }

    fun MnemonicWithPassphrase.signAuth(
        input: PerFactorSourceInputOfAuthIntent
    ): AccessFactorSourcesOutput.Signing {
        val signatures =  input.perTransaction.map { transaction ->
            val payloadId = transaction.payloadId()

            transaction.ownedFactorInstances.map { instance ->
                val signatureWithPublicKey = sign(
                    hash = payloadId.payload.hash(),
                    path = instance.factorInstance.publicKey.derivationPath
                )

                HdSignatureOfAuthIntentHash(
                    input = HdSignatureInputOfAuthIntentHash(
                        payloadId = payloadId,
                        ownedFactorInstance = instance
                    ),
                    signature = signatureWithPublicKey
                )
            }
        }
            .flatten()

        return AccessFactorSourcesOutput.SignAuth(
            outcome = PerFactorOutcomeOfAuthIntentHash(
                factorSourceId = input.factorSourceId,
                outcome = FactorOutcomeOfAuthIntentHash.Signed(signatures)
            )
        )
    }
}

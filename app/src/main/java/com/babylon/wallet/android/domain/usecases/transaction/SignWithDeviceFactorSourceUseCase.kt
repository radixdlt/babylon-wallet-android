package com.babylon.wallet.android.domain.usecases.transaction

import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.sign
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.updateLastUsed
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

class SignWithDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        deviceFactorSource: FactorSource.Device,
        signers: List<ProfileEntity>,
        signRequest: SignRequest
    ): Result<List<SignatureWithPublicKey>> {
        val result = mutableListOf<SignatureWithPublicKey>()

        signers.forEach { signer ->
            when (val securityState = signer.securityState) {
                is EntitySecurityState.Unsecured -> {
                    val factorInstance = when (signRequest) {
                        is SignRequest.SignAuthChallengeRequest ->
                            securityState.value.authenticationSigning
                                ?: securityState.value.transactionSigning

                        is SignRequest.SignTransactionRequest -> securityState.value.transactionSigning
                    }
                    val mnemonicExist = mnemonicRepository.mnemonicExist(deviceFactorSource.value.id.asGeneral())
                    if (mnemonicExist.not()) return Result.failure(ProfileException.NoMnemonic)
                    mnemonicRepository.readMnemonic(deviceFactorSource.value.id.asGeneral()).mapCatching { mnemonic ->
                        val signatureWithPublicKey = mnemonic.sign(
                            hash = signRequest.hashedDataToSign,
                            path = factorInstance.publicKey.derivationPath
                        )
                        result.add(signatureWithPublicKey)
                        val profile = profileRepository.profile.first()
                        profileRepository.saveProfile(profile.updateLastUsed(deviceFactorSource.id))
                    }.onFailure {
                        return Result.failure(it)
                    }
                }
            }
        }
        return Result.success(result)
    }
}

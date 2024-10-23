package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.domain.model.signing.EntityWithSignature
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSource
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
    ): Result<List<EntityWithSignature>> {
        val entitiesWithSignaturesList = mutableListOf<EntityWithSignature>()

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
                        entitiesWithSignaturesList.add(
                            EntityWithSignature(
                                entity = signer,
                                signatureWithPublicKey = signatureWithPublicKey
                            )
                        )
                        val profile = profileRepository.profile.first()
                        profileRepository.saveProfile(profile.updateLastUsed(deviceFactorSource.id))
                    }.onFailure {
                        return Result.failure(it)
                    }
                }
                is EntitySecurityState.Securified -> TODO("Securified state is not yet supported.")
            }
        }
        return Result.success(entitiesWithSignaturesList)
    }
}

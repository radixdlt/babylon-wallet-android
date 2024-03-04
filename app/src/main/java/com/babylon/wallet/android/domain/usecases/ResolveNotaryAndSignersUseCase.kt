package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException.FailedToFindSigningEntities
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.personasOnCurrentNetwork
import rdx.works.profile.ret.crypto.PrivateKey
import javax.inject.Inject

class ResolveNotaryAndSignersUseCase @Inject constructor(
    private val profileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(
        accountsRequiringAuth: List<String>,
        personasRequiringAuth: List<String>,
        notary: PrivateKey
    ) = runCatching {
        val accounts = profileUseCase.accountsOnCurrentNetwork()
        val personas = profileUseCase.personasOnCurrentNetwork()

        accountsRequiringAuth.map { address ->
            accounts.find { it.address == address } ?: throw FailedToFindSigningEntities
        } + personasRequiringAuth.map { address ->
            personas.find { it.address == address } ?: throw FailedToFindSigningEntities
        }
    }.map {
        NotaryAndSigners(
            signers = it,
            ephemeralNotaryPrivateKey = notary
        )
    }
}

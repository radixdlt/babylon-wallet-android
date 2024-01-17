package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException.FailedToFindSigningEntities
import com.radixdlt.ret.ManifestSummary
import rdx.works.core.ret.crypto.PrivateKey
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.personasOnCurrentNetwork
import javax.inject.Inject

class ResolveNotaryAndSignersUseCase @Inject constructor(
    private val profileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(summary: ManifestSummary, notary: PrivateKey) = invoke(summary = summary).map {
        NotaryAndSigners(
            signers = it,
            ephemeralNotaryPrivateKey = notary
        )
    }

    private suspend operator fun invoke(summary: ManifestSummary): Result<List<Entity>> = runCatching {
        val requiredAccountAddresses = summary.accountsRequiringAuth.map { it.addressString() }
        val requiredPersonaAddresses = summary.identitiesRequiringAuth.map { it.addressString() }

        val accounts = profileUseCase.accountsOnCurrentNetwork()
        val personas = profileUseCase.personasOnCurrentNetwork()

        requiredAccountAddresses.map { address ->
            accounts.find { it.address == address } ?: throw FailedToFindSigningEntities
        } + requiredPersonaAddresses.map { address ->
            personas.find { it.address == address } ?: throw FailedToFindSigningEntities
        }
    }
}

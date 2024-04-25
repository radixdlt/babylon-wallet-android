package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException.FailedToFindSigningEntities
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.extensions.asProfileEntity
import rdx.works.core.crypto.PrivateKey
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class ResolveNotaryAndSignersUseCase @Inject constructor(
    private val profileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(
        accountsAddressesRequiringAuth: List<AccountAddress>,
        personaAddressesRequiringAuth: List<IdentityAddress>,
        notary: PrivateKey
    ) = runCatching {
        val accounts = profileUseCase().activeAccountsOnCurrentNetwork
        val personas = profileUseCase().activePersonasOnCurrentNetwork

        accountsAddressesRequiringAuth.map { address ->
            accounts.find { it.address == address }?.asProfileEntity() ?: throw FailedToFindSigningEntities
        } + personaAddressesRequiringAuth.map { address ->
            personas.find { it.address == address }?.asProfileEntity() ?: throw FailedToFindSigningEntities
        }
    }.map {
        NotaryAndSigners(
            signers = it,
            ephemeralNotaryPrivateKey = notary
        )
    }
}

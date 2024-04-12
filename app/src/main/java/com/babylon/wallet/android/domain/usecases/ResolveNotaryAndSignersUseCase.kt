package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException.FailedToFindSigningEntities
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.extensions.string
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.personasOnCurrentNetwork
import rdx.works.profile.ret.crypto.PrivateKey
import javax.inject.Inject

class ResolveNotaryAndSignersUseCase @Inject constructor(
    private val profileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(
        accountsAddressesRequiringAuth: List<AccountAddress>,
        personaAddressesRequiringAuth: List<IdentityAddress>,
        notary: PrivateKey
    ) = runCatching {
        val accounts = profileUseCase.accountsOnCurrentNetwork()
        val personas = profileUseCase.personasOnCurrentNetwork()

        accountsAddressesRequiringAuth.map { address ->
            accounts.find { it.address == address.string } ?: throw FailedToFindSigningEntities
        } + personaAddressesRequiringAuth.map { address ->
            personas.find { it.address == address.string } ?: throw FailedToFindSigningEntities
        }
    }.map {
        NotaryAndSigners(
            signers = it,
            ephemeralNotaryPrivateKey = notary
        )
    }
}

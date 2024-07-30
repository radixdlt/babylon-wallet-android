package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.asProfileEntity
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
        notary: Curve25519SecretKey
    ) = runCatching {
        val accounts = profileUseCase().activeAccountsOnCurrentNetwork
        val personas = profileUseCase().activePersonasOnCurrentNetwork

        accountsAddressesRequiringAuth.mapNotNull { address ->
            accounts.find { it.address == address }?.asProfileEntity()
        } + personaAddressesRequiringAuth.mapNotNull { address ->
            personas.find { it.address == address }?.asProfileEntity()
        }
    }.map {
        NotaryAndSigners(
            signers = it,
            ephemeralNotaryPrivateKey = notary
        )
    }
}

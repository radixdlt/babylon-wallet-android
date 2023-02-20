package rdx.works.profile.data.model.pernetwork

import com.radixdlt.model.PrivateKey

data class AccountSigner(
    val account: OnNetwork.Account,
    val privateKey: PrivateKey
)

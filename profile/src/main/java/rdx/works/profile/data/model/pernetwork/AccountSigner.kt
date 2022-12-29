package rdx.works.profile.data.model.pernetwork

import com.radixdlt.model.PrivateKey

data class AccountSigner(
    val account: Account,
    val privateKey: PrivateKey,
    val signers: List<PrivateKey>,
)

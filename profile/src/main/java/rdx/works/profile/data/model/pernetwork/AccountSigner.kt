package rdx.works.profile.data.model.pernetwork

import com.radixdlt.model.PrivateKey
import com.radixdlt.model.PublicKey

data class AccountSigner(
    val account: Account,
    val notaryPublicKey: PublicKey,
    val notaryPrivateKey: PrivateKey,
    val signers: List<PrivateKey>,
)
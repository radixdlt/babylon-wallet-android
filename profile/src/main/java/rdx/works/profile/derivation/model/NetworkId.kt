package rdx.works.profile.derivation.model

/**
 * Full list of networks is documented here -> https://github.com/radixdlt/babylon-node/blob/main/common/src/main/java
 * /com/radixdlt/networks/Network.java
 */
@Suppress("MagicNumber")
enum class NetworkId(val value: Int) {
    Mainnet(1),
    Adapanet(10),
    Nebunet(11),
    Kisharnet(12),
    Gilganet(32),
    Enkinet(33),
    Hammunet(34),
    Nergalnet(35),
    Mardunet(36);

    companion object {
        fun from(id: Int) = values().find { it.value == id } ?: error("Unknown network id $id")
    }
}

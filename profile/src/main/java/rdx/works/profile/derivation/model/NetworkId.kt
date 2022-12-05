package rdx.works.profile.derivation.model

/**
 * Full list of networks is documented here -> https://github.com/radixdlt/babylon-node/blob/main/common/src/main/java
 * /com/radixdlt/networks/Network.java
 */
enum class NetworkId(val value: Int) {
    Mainnet(1),
    Adapanet(10),
    Enkinet(33),
    Hammunet(34),
    Mardunet(36)
}
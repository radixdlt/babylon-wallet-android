package rdx.works.core

import com.radixdlt.ret.OlympiaAddress

private typealias EngineAddress = com.radixdlt.ret.Address

@Suppress("TooManyFunctions")
object AddressHelper {

    fun isValid(address: String, checkNetworkId: Int? = null): Boolean {
        val retAddress = address.toAddressOrNull() ?: return false

        return if (checkNetworkId != null) {
            retAddress.networkId() == checkNetworkId.toUByte()
        } else {
            true
        }
    }

    fun publicKeyHash(accountAddress: String) =
        accountAddress.toAddressOrNull()?.bytes()?.takeLast(PUBLIC_KEY_HASH_LENGTH)?.toByteArray()

    fun accountAddressFromOlympia(olympiaAddress: String, forNetworkId: Int) = EngineAddress.virtualAccountAddressFromOlympiaAddress(
        olympiaAccountAddress = OlympiaAddress(olympiaAddress),
        networkId = forNetworkId.toUByte()
    ).addressString()

    private fun String.toAddressOrNull() = runCatching { EngineAddress(this) }
        .getOrNull()
}

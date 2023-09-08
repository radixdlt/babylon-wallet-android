package rdx.works.core

import com.radixdlt.ret.Address

object AddressValidator {

    fun isValid(address: String, networkId: Int): Boolean = runCatching {
        Address(address)
    }.getOrNull()?.let {
        it.networkId() == networkId.toUByte()
    } ?: false

    fun getValidNetworkId(address: String) = runCatching {
        Address(address)
    }.getOrNull()?.networkId()?.toInt()
}

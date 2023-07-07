package rdx.works.core

import com.radixdlt.ret.Address

object AddressValidator {
    private const val MIN_ADDRESS_SIZE = 26

    /**
     * As per [REP 39 Bech32m and Addressing](https://radixdlt.atlassian.net/wiki/spaces/S/pages/2781839425/REP+39+Bech32m+and+Addressing)
     * The address need to be at least 26 chars long, and should be validated against KET.
     */
    fun isValid(address: String): Boolean {
        return address.length >= MIN_ADDRESS_SIZE && runCatching { Address(address) }.isSuccess
    }
}

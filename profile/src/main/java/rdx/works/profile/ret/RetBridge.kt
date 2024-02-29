package rdx.works.profile.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.EntityType
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.OlympiaAddress
import com.radixdlt.ret.knownAddresses
import rdx.works.core.PUBLIC_KEY_HASH_LENGTH
import timber.log.Timber

private typealias EngineAddress = Address

object RetBridge {

    private const val LOG_TAG = "RetBridge"

    object Address {

        fun networkIdOrNull(fromAddress: String): Int? = fromAddress.toAddressOrNull()?.networkId()?.toInt()

        fun networkId(fromAddress: String): Int = EngineAddress(fromAddress).networkId().toInt()

        fun isResource(address: String): Boolean = address.toAddressOrNull()?.isGlobalResourceManager() == true

        fun isValidResource(address: String, networkId: Int) = isValid(address, networkId) && isResource(address)

        fun isPool(address: String): Boolean {
            val entityType = address.toAddressOrNull()?.entityType()

            return entityType == EntityType.GLOBAL_ONE_RESOURCE_POOL ||
                    entityType == EntityType.GLOBAL_TWO_RESOURCE_POOL ||
                    entityType == EntityType.GLOBAL_MULTI_RESOURCE_POOL
        }

        fun isValidator(address: String): Boolean {
            val entityType = address.toAddressOrNull()?.entityType()

            return entityType == EntityType.GLOBAL_VALIDATOR
        }

        fun isValid(address: String, checkNetworkId: Int? = null): Boolean {
            val retAddress = address.toAddressOrNull() ?: return false

            return if (checkNetworkId != null) {
                retAddress.networkId() == checkNetworkId.toUByte()
            } else {
                true
            }
        }

        fun isValidNFT(address: String) = address.toNonFungibleGlobalId() != null

        fun globalId(address: String) = NonFungibleGlobalId(address).resourceAddress().addressString()

        fun publicKeyHash(accountAddress: String) =
            accountAddress.toAddressOrNull()?.bytes()?.takeLast(PUBLIC_KEY_HASH_LENGTH)?.toByteArray()

        fun xrdAddress(forNetworkId: Int): String = knownAddresses(
            networkId = forNetworkId.toUByte()
        ).resourceAddresses.xrd.addressString()

        fun accountAddressFromOlympia(olympiaAddress: String, forNetworkId: Int) = EngineAddress.virtualAccountAddressFromOlympiaAddress(
            olympiaAccountAddress = OlympiaAddress(olympiaAddress),
            networkId = forNetworkId.toUByte()
        ).addressString()

        private fun String.toAddressOrNull() = runCatching { EngineAddress(this) }
            .onFailure { Timber.tag(LOG_TAG).w(it) }
            .getOrNull()

        private fun String.toNonFungibleGlobalId() = runCatching { NonFungibleGlobalId(this) }
            .onFailure { Timber.tag(LOG_TAG).w(it) }
            .getOrNull()
    }

}
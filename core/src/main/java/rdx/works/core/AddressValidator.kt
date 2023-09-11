package rdx.works.core

import com.radixdlt.ret.Address
import com.radixdlt.ret.EntityType
import com.radixdlt.ret.NonFungibleGlobalId

object AddressValidator {
    fun isValid(address: String, networkId: Int): Boolean = runCatching {
        Address(address)
    }.getOrNull()?.let {
        it.networkId() == networkId.toUByte()
    } ?: false

    fun isValidForTypes(address: String, networkId: Int, allowedEntityTypes: Set<EntityType>): Boolean = runCatching {
        Address(address)
    }.getOrNull()?.let {
        val allowedEntity = allowedEntityTypes.isEmpty() || allowedEntityTypes.contains(it.entityType())
        it.networkId() == networkId.toUByte() && allowedEntity
    } ?: false

    fun isValidNft(address: String): Boolean = runCatching {
        NonFungibleGlobalId(address)
    }.getOrNull() != null

    fun getValidNetworkId(address: String) = runCatching {
        Address(address)
    }.getOrNull()?.networkId()?.toInt()
}

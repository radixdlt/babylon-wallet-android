package rdx.works.core

import com.radixdlt.ret.Address
import com.radixdlt.ret.EntityType

object AddressValidator {

    fun isValid(address: String, networkId: Int, allowedEntityTypes: Set<EntityType> = emptySet()): Boolean = runCatching {
        Address(address)
    }.getOrNull()?.let {
        val allowedEntity = allowedEntityTypes.isEmpty() || allowedEntityTypes.contains(it.entityType())
        it.networkId() == networkId.toUByte() && allowedEntity
    } ?: false

    fun getValidNetworkId(address: String) = runCatching {
        Address(address)
    }.getOrNull()?.networkId()?.toInt()

    fun hasResourcePrefix(address: String): Boolean = address.lowercase().startsWith("resource_")
}

package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.VaultAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.domain.assets.AssetBehaviour
import rdx.works.core.domain.resources.metadata.Metadata
import java.time.Instant

@Serializable
data class BehavioursColumn(val behaviours: Set<AssetBehaviour>)

@Serializable
data class MetadataColumn(val metadata: List<Metadata>)

@Suppress("TooManyFunctions")
@ProvidedTypeConverter
class StateDatabaseConverters {

    private val json = Json {
        allowStructuredMapKeys = true
    }

    // Behaviours
    @TypeConverter
    fun stringToBehaviours(string: String?): BehavioursColumn? {
        return string?.let { BehavioursColumn(behaviours = json.decodeFromString(string)) }
    }

    @TypeConverter
    fun behavioursToString(column: BehavioursColumn?): String? {
        return column?.let { json.encodeToString(it.behaviours) }
    }

    // Metadata
    @TypeConverter
    fun stringToMetadata(string: String?): MetadataColumn? {
        return string?.let { MetadataColumn(metadata = json.decodeFromString(string)) }
    }

    @TypeConverter
    fun metadataToString(column: MetadataColumn?): String? {
        return column?.let { json.encodeToString(it.metadata) }
    }

    // Decimal192
    @TypeConverter
    fun stringToDecimal192(string: String?): Decimal192? {
        return string?.toDecimal192OrNull()
    }

    @TypeConverter
    fun decimal192ToString(decimal: Decimal192?): String? {
        return decimal?.string
    }

    // Instant
    @TypeConverter
    fun longToInstant(date: Long?): Instant? {
        return date?.let { Instant.ofEpochMilli(date) }
    }

    @TypeConverter
    fun instantToLong(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    // ResourceAddress
    @TypeConverter
    fun stringToResourceAddress(resourceAddress: String?): ResourceAddress? {
        return resourceAddress?.let { ResourceAddress.init(it) }
    }

    @TypeConverter
    fun resourceAddressToString(resourceAddress: ResourceAddress?): String? {
        return resourceAddress?.string
    }

    // AccountAddress
    @TypeConverter
    fun stringToAccountAddress(accountAddress: String?): AccountAddress? {
        return accountAddress?.let { AccountAddress.init(it) }
    }

    @TypeConverter
    fun accountAddressToString(accountAddress: AccountAddress?): String? {
        return accountAddress?.string
    }

    // ValidatorAddress
    @TypeConverter
    fun stringToValidatorAddress(validatorAddress: String?): ValidatorAddress? {
        return validatorAddress?.let { ValidatorAddress.init(it) }
    }

    @TypeConverter
    fun validatorAddressToString(validatorAddress: ValidatorAddress?): String? {
        return validatorAddress?.string
    }

    // PoolAddress
    @TypeConverter
    fun stringToPoolAddress(poolAddress: String?): PoolAddress? {
        return poolAddress?.let { PoolAddress.init(it) }
    }

    @TypeConverter
    fun poolAddressToString(poolAddress: PoolAddress?): String? {
        return poolAddress?.string
    }

    // NonFungibleLocalId
    @TypeConverter
    fun stringToNonFungibleLocalId(localId: String?): NonFungibleLocalId? {
        return localId?.let { NonFungibleLocalId.init(it) }
    }

    @TypeConverter
    fun nonFungibleLocalIdToString(localId: NonFungibleLocalId?): String? {
        return localId?.string
    }

    // VaultAddress
    @TypeConverter
    fun stringToVaultAddress(vaultAddress: String?): VaultAddress? {
        return vaultAddress?.let { VaultAddress.init(it) }
    }

    @TypeConverter
    fun vaultAddressToString(vaultAddress: VaultAddress?): String? {
        return vaultAddress?.string
    }
}

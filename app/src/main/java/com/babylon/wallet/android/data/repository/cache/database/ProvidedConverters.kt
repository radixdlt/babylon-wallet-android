package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.repository.cache.database.MetadataColumn.ImplicitMetadataState
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.LockerAddress
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.VaultAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.domain.assets.AssetBehaviour
import rdx.works.core.domain.resources.Divisibility
import rdx.works.core.domain.resources.metadata.Metadata
import java.time.Instant

@Serializable
data class BehavioursColumn(val behaviours: Set<AssetBehaviour>)

@Serializable
data class MetadataColumn(
    /**
     * A union of explicit and **currently known** implicit metadata
     * As a client we don't need to expose the difference between explicit
     * and implicit metadata.
     */
    val metadata: List<Metadata>,

    /**
     * The state of the next page of the implicit metadata. See [ImplicitMetadataState]
     */
    @SerialName("implicit_state")
    val implicitState: ImplicitMetadataState
) {

    val nextCursor: String?
        get() = (implicitState as? ImplicitMetadataState.Incomplete)?.nextCursor

    @Serializable
    sealed interface ImplicitMetadataState {
        /**
         * When we have no information yet regarding the existence of implicit metadata
         * An example is when we query account information. In this request we receive
         * resource data but with no information about implicit metadata. In order to make
         * sure we received all metadata available we need to fetch details of this specific
         * resource
         */
        @Serializable
        @SerialName("unknown")
        data object Unknown : ImplicitMetadataState

        /**
         * We have received an answer from a details request and we know that the [MetadataColumn.metadata]
         * are complete.
         */
        @Serializable
        @SerialName("complete")
        data object Complete : ImplicitMetadataState

        /**
         * We have received an answer from a details request and we know that the [MetadataColumn.metadata]
         * are incomplete. We need to query [nextCursor] to receive more.
         */
        @Serializable
        @SerialName("incomplete")
        data class Incomplete(
            @SerialName("next_cursor")
            val nextCursor: String
        ) : ImplicitMetadataState
    }

    companion object {
        fun from(
            explicitMetadata: EntityMetadataCollection?,
            implicitMetadata: EntityMetadataCollection
        ): MetadataColumn {
            val explicit = explicitMetadata?.toMetadata().orEmpty().toSet()
            val implicit = implicitMetadata.toMetadata().toSet()

            val all = explicit union implicit
            return MetadataColumn(
                metadata = all.toList(),
                implicitState = implicitMetadata.nextCursor?.let {
                    ImplicitMetadataState.Incomplete(nextCursor = it)
                } ?: ImplicitMetadataState.Complete
            )
        }
    }
}

@Suppress("TooManyFunctions")
@ProvidedTypeConverter
class StateDatabaseConverters {

    private val json = Json {
        allowStructuredMapKeys = true
    }

    // Behaviours
    @TypeConverter
    fun stringToBehaviours(string: String?): BehavioursColumn? {
        return string?.let { json.decodeFromString(string) }
    }

    @TypeConverter
    fun behavioursToString(column: BehavioursColumn?): String? {
        return column?.let { json.encodeToString(it) }
    }

    // Metadata
    @TypeConverter
    fun stringToMetadata(string: String?): MetadataColumn? {
        return string?.let { json.decodeFromString(string) }
    }

    @TypeConverter
    fun metadataToString(column: MetadataColumn?): String? {
        return column?.let { json.encodeToString(it) }
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

    // Divisibility
    @TypeConverter
    fun intToDivisibility(divisibility: Int?): Divisibility? {
        return divisibility?.let { Divisibility(it.toUByte()) }
    }

    @TypeConverter
    fun divisibilityToInt(divisibility: Divisibility?): Int? {
        return divisibility?.value?.toInt()
    }

    // LockerAddress
    @TypeConverter
    fun stringToLockerAddress(lockerAddress: String?): LockerAddress? {
        return lockerAddress?.let { LockerAddress.init(it) }
    }

    @TypeConverter
    fun lockerAddressToString(lockerAddress: LockerAddress?): String? {
        return lockerAddress?.string
    }
}

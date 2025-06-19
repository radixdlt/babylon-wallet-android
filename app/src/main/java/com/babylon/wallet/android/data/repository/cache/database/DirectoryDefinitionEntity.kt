package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.domain.model.DirectoryDefinition
import com.radixdlt.sargon.AccountAddress
import kotlinx.serialization.json.Json
import java.time.Instant

@Entity
data class DirectoryDefinitionEntity(
    @PrimaryKey
    val address: AccountAddress,
    val name: String,
    val tags: String,
    val isHighlighted: Boolean,
    val category: String,
    val synced: Instant
) {

    fun toDirectoryDefinition() = DirectoryDefinition(
        name = name,
        dAppDefinitionAddress = address,
        tags = Json.decodeFromString(tags),
        category = category.takeIf { it.isNotEmpty() },
    )

    companion object {

        fun from(
            definition: DirectoryDefinition,
            isHighlighted: Boolean,
            synced: Instant
        ) = DirectoryDefinitionEntity(
            address = definition.dAppDefinitionAddress,
            name = definition.name,
            tags = Json.encodeToString(definition.tags),
            category = definition.category.orEmpty(),
            isHighlighted = isHighlighted,
            synced = synced
        )
    }
}

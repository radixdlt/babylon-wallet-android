package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItemFungible
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItemNonFungible
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItemType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AccountLockerVaultCollectionItemSerializer : JsonContentPolymorphicSerializer<AccountLockerVaultCollectionItem>(
    AccountLockerVaultCollectionItem::class
) {
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<AccountLockerVaultCollectionItem> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            AccountLockerVaultCollectionItemType.Fungible.value -> AccountLockerVaultCollectionItemFungible.serializer()
            AccountLockerVaultCollectionItemType.NonFungible.value -> AccountLockerVaultCollectionItemNonFungible.serializer()
            else -> error("AccountLockerVaultCollectionItemType not found.")
        }
    }
}

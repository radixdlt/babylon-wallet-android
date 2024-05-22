package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.AccountAuthorizedDepositorBadgeType
import com.babylon.wallet.android.data.gateway.generated.models.AccountAuthorizedDepositorsNonFungibleBadge
import com.babylon.wallet.android.data.gateway.generated.models.AccountAuthorizedDepositorsResourceBadge
import com.babylon.wallet.android.data.gateway.generated.models.AccountAuthorizedDepositorsResponseItem
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AccountAuthorizedDepositorsResponseItemSerializer :
    JsonContentPolymorphicSerializer<AccountAuthorizedDepositorsResponseItem>(
        AccountAuthorizedDepositorsResponseItem::class
    ) {

    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<AccountAuthorizedDepositorsResponseItem> {
        return when (element.jsonObject["badge_type"]?.jsonPrimitive?.content) {
            AccountAuthorizedDepositorBadgeType.ResourceBadge.value -> AccountAuthorizedDepositorsResourceBadge.serializer()
            AccountAuthorizedDepositorBadgeType.NonFungibleBadge.value -> AccountAuthorizedDepositorsNonFungibleBadge.serializer()
            else -> error("")
        }
    }
}

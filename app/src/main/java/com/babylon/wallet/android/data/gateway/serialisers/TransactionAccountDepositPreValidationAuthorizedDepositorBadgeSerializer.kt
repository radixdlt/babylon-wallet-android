package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.AccountAuthorizedDepositorBadgeType
import com.babylon.wallet.android.data.gateway.generated.models.AccountDepositPreValidationNonFungibleBadge
import com.babylon.wallet.android.data.gateway.generated.models.AccountDepositPreValidationResourceBadge
import com.babylon.wallet.android.data.gateway.generated.models.TransactionAccountDepositPreValidationAuthorizedDepositorBadge
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object TransactionAccountDepositPreValidationAuthorizedDepositorBadgeSerializer :
    JsonContentPolymorphicSerializer<TransactionAccountDepositPreValidationAuthorizedDepositorBadge>(
        TransactionAccountDepositPreValidationAuthorizedDepositorBadge::class
    ) {

    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<TransactionAccountDepositPreValidationAuthorizedDepositorBadge> {
        return when (element.jsonObject["badge_type"]?.jsonPrimitive?.content) {
            AccountAuthorizedDepositorBadgeType.ResourceBadge.value -> AccountDepositPreValidationResourceBadge.serializer()
            AccountAuthorizedDepositorBadgeType.NonFungibleBadge.value -> AccountDepositPreValidationNonFungibleBadge.serializer()
            else -> error("")
        }
    }
}

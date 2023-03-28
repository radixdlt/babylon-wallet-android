package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseComponentDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleVaultDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleVaultDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponsePackageDetails
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object StateEntityDetailsResponseItemDetailsSerializer :
    JsonContentPolymorphicSerializer<StateEntityDetailsResponseItemDetails>(StateEntityDetailsResponseItemDetails::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<StateEntityDetailsResponseItemDetails> {
        return when (val type = element.jsonObject["type"]?.jsonPrimitive?.content) {
            "FungibleResource" -> StateEntityDetailsResponseFungibleResourceDetails.serializer()
            "NonFungibleResource" -> StateEntityDetailsResponseNonFungibleResourceDetails.serializer()
            "FungibleVault" -> StateEntityDetailsResponseFungibleVaultDetails.serializer()
            "NonFungibleVault" -> StateEntityDetailsResponseNonFungibleVaultDetails.serializer()
            "Package" -> StateEntityDetailsResponsePackageDetails.serializer()
            "Component" -> StateEntityDetailsResponseComponentDetails.serializer()
            else -> error("No serializer found for type $type. Update the StateEntityDetailsResponseItemDetailsSerializer")
        }
    }
}

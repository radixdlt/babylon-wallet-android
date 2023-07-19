@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.extensions.ResourceRole.Burn
import com.babylon.wallet.android.data.gateway.extensions.ResourceRole.Deposit
import com.babylon.wallet.android.data.gateway.extensions.ResourceRole.Mint
import com.babylon.wallet.android.data.gateway.extensions.ResourceRole.Recall
import com.babylon.wallet.android.data.gateway.extensions.ResourceRole.UpdateMetadata
import com.babylon.wallet.android.data.gateway.extensions.ResourceRole.UpdateNonFungibleData
import com.babylon.wallet.android.data.gateway.extensions.ResourceRole.Withdraw
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityAccessRules
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.babylon.wallet.android.domain.model.behaviours.ResourceBehaviour

fun StateEntityDetailsResponseItemDetails.totalSupply(): String? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> details.totalSupply
        is StateEntityDetailsResponseNonFungibleResourceDetails -> details.totalSupply
        else -> null
    }
}

@Suppress("ComplexCondition", "TooManyFunctions", "LongMethod", "CyclomaticComplexMethod")
fun StateEntityDetailsResponseItemDetails.calculateResourceBehaviours(): List<ResourceBehaviour> = if (isUsingDefaultRules()) {
    listOf(ResourceBehaviour.DEFAULT_RESOURCE)
} else {
    val accessRulesChain = toAccessRulesChain()
    val behaviors = mutableListOf<ResourceBehaviour>()

    if (accessRulesChain?.isDefaultPerform(Mint) == false && !accessRulesChain.isDefaultPerform(Burn)) {
        behaviors.add(ResourceBehaviour.PERFORM_MINT_BURN)
    } else if (accessRulesChain?.isDefaultPerform(Mint) == false) {
        behaviors.add(ResourceBehaviour.PERFORM_MINT)
    } else if (accessRulesChain?.isDefaultPerform(Burn) == false) {
        behaviors.add(ResourceBehaviour.PERFORM_BURN)
    }

    if (accessRulesChain?.isDefaultChange(Mint) == false && !accessRulesChain.isDefaultChange(Burn)) {
        behaviors.add(ResourceBehaviour.CHANGE_MINT_BURN)
    } else if (accessRulesChain?.isDefaultChange(Mint) == false) {
        behaviors.add(ResourceBehaviour.CHANGE_MINT)
    } else if (accessRulesChain?.isDefaultChange(Burn) == false) {
        behaviors.add(ResourceBehaviour.CHANGE_BURN)
    }

    if (accessRulesChain?.isDefaultPerform(Withdraw) == false && !accessRulesChain.isDefaultPerform(Deposit)) {
        behaviors.add(ResourceBehaviour.CANNOT_PERFORM_WITHDRAW_DEPOSIT)
    }

    // when both withdraw and deposit perform are set to defaults, but either withdraw or deposit for change
    // is set to not just a non default but specifically AllowAll (highly unusual, but it's possible)
    if (
        (accessRulesChain?.isDefaultPerform(Withdraw) == true && accessRulesChain.isDefaultPerform(Deposit)) &&
        accessRulesChain.change(Withdraw) == AccessRule.AllowAll ||
        accessRulesChain?.change(Deposit) == AccessRule.AllowAll
    ) {
        behaviors.add(ResourceBehaviour.CHANGE_WITHDRAW_DEPOSIT)
    }

    // 1. when both withdraw and deposit perform are set to defaults,
    // 2. but either withdraw or deposit change is set to non default
    // 3. (but neither set to AllowAll since that would be covered in the one above)
    val bothPerformSetToDefault = accessRulesChain?.isDefaultPerform(Withdraw) == true && accessRulesChain.isDefaultPerform(Deposit)
    val eitherChangeSetToNonDefault = accessRulesChain?.isDefaultChange(Deposit) == false ||
        accessRulesChain?.isDefaultChange(Withdraw) == false
    val neitherChangeSetToAllowAll = accessRulesChain?.change(Deposit) != AccessRule.AllowAll &&
        accessRulesChain?.change(Withdraw) != AccessRule.AllowAll
    if (bothPerformSetToDefault && eitherChangeSetToNonDefault && neitherChangeSetToAllowAll) {
        behaviors.add(ResourceBehaviour.FUTURE_MOVEMENT_WITHDRAW_DEPOSIT)
    }

    if (accessRulesChain?.isDefaultPerform(UpdateMetadata) == false) {
        behaviors.add(ResourceBehaviour.PERFORM_UPDATE_METADATA)
    }
    if (accessRulesChain?.isDefaultChange(UpdateMetadata) == false) {
        behaviors.add(ResourceBehaviour.CHANGE_UPDATE_METADATA)
    }

    if (accessRulesChain?.isDefaultPerform(Recall) == false) {
        behaviors.add(ResourceBehaviour.PERFORM_RECALL)
    }
    if (accessRulesChain?.isDefaultChange(Recall) == false) {
        behaviors.add(ResourceBehaviour.CHANGE_RECALL)
    }

    if (type == StateEntityDetailsResponseItemDetailsType.nonFungibleResource) {
        if (accessRulesChain?.isDefaultPerform(UpdateNonFungibleData) == false) {
            behaviors.add(ResourceBehaviour.PERFORM_UPDATE_NON_FUNGIBLE_DATA)
        }
        if (accessRulesChain?.isDefaultChange(UpdateNonFungibleData) == false) {
            behaviors.add(ResourceBehaviour.CHANGE_UPDATE_NON_FUNGIBLE_DATA)
        }
    }

    behaviors
}

/**
 * The rules to determine behaviours was taken from here ->
 * https://radixdlt.atlassian.net/wiki/spaces/AT/pages/3007840284/Proposal+for+Resource+Behavior+Summarization
 */
enum class AccessRule(val value: String) {
    DenyAll("DenyAll"),
    AllowAll("AllowAll")
}

sealed interface ResourceRole {
    val perform: String

    val change: String
        get() = "${perform}_updater"

    val defaultPerformRule: AccessRule
    val defaultChangeRule: AccessRule

    object Mint : ResourceRole {
        override val perform: String = "minter"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultChangeRule: AccessRule = AccessRule.DenyAll
    }

    object Burn : ResourceRole {
        override val perform: String = "burner"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultChangeRule: AccessRule = AccessRule.DenyAll
    }

    object Withdraw : ResourceRole {
        override val perform: String = "withdrawer"
        override val defaultPerformRule: AccessRule = AccessRule.AllowAll
        override val defaultChangeRule: AccessRule = AccessRule.DenyAll
    }

    object Deposit : ResourceRole {
        override val perform: String = "depositor"
        override val defaultPerformRule: AccessRule = AccessRule.AllowAll
        override val defaultChangeRule: AccessRule = AccessRule.DenyAll
    }

    object Recall : ResourceRole {
        override val perform: String = "recaller"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultChangeRule: AccessRule = AccessRule.DenyAll
    }

    object UpdateMetadata : ResourceRole {
        override val perform: String = "metadata_setter"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultChangeRule: AccessRule = AccessRule.DenyAll
    }

    object UpdateNonFungibleData : ResourceRole {
        override val perform: String = "non_fungible_data_updater"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultChangeRule: AccessRule = AccessRule.DenyAll
    }

    companion object {
        val rolesForFungibles = listOf(
            Burn,
            Mint,
            Deposit,
            Withdraw,
            UpdateMetadata,
            Recall
        )

        val rolesForNonFungibles = listOf(
            Burn,
            Mint,
            Deposit,
            Withdraw,
            UpdateMetadata,
            Recall,
            UpdateNonFungibleData
        )
    }
}

private fun StateEntityDetailsResponseItemDetails.toAccessRulesChain(): ComponentEntityAccessRules? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> details.accessRules
        is StateEntityDetailsResponseNonFungibleResourceDetails -> details.accessRules
        else -> null
    }
}

private fun ComponentEntityAccessRules.perform(action: ResourceRole): AccessRule? = propertyEntries.find { entry ->
    entry.key == action.perform
}?.value?.type?.let { type ->
    AccessRule.values().find { it.value == type }
}

private fun ComponentEntityAccessRules.change(action: ResourceRole): AccessRule? = propertyEntries.find { entry ->
    entry.key == action.change
}?.value?.type?.let { type ->
    AccessRule.values().find { it.value == type }
}

private fun ComponentEntityAccessRules.isDefaultPerform(action: ResourceRole): Boolean = perform(action) == action.defaultPerformRule

private fun ComponentEntityAccessRules.isDefaultChange(action: ResourceRole): Boolean = change(action) == action.defaultChangeRule

private fun ComponentEntityAccessRules.isDefault(actions: List<ResourceRole>) = actions.all { action ->
    isDefaultPerform(action = action) && isDefaultChange(action = action)
}

private fun StateEntityDetailsResponseItemDetails.isUsingDefaultRules(): Boolean {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> details.accessRules.isDefault(ResourceRole.rolesForFungibles)
        is StateEntityDetailsResponseNonFungibleResourceDetails -> details.accessRules.isDefault(ResourceRole.rolesForNonFungibles)
        else -> false
    }
}

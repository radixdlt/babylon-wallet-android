@file:Suppress("CommentSpacing", "UnusedPrivateMember", "NoUnusedImports", "TooManyFunctions")

package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignments
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseComponentDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.behaviours.ResourceBehaviour
import java.math.BigDecimal

fun StateEntityDetailsResponseItemDetails.totalSupply(): String? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> details.totalSupply
        is StateEntityDetailsResponseNonFungibleResourceDetails -> details.totalSupply
        else -> null
    }
}

fun StateEntityDetailsResponseItem.getXRDVaultAmount(vaultAddress: String): BigDecimal? {
    return when (
        val resource = fungibleResources?.items?.find {
            Resource.FungibleResource.officialXrdResourceAddresses().contains(it.resourceAddress)
        }
    ) {
        is FungibleResourcesCollectionItemVaultAggregated -> {
            resource.vaults.items.find { it.vaultAddress == vaultAddress }?.amount?.toBigDecimal()
        }

        else -> null
    }
}

fun StateEntityDetailsResponseItemDetails.xrdVaultAddress(): String? {
    return when (val details = this) {
        is StateEntityDetailsResponseComponentDetails -> details.state?.value?.stakeXrdVault?.entityAddress
        else -> null
    }
}

fun StateEntityDetailsResponseItemDetails.stakeUnitResourceAddress(): String? {
    return when (val details = this) {
        is StateEntityDetailsResponseComponentDetails -> details.state?.value?.stakeUnitResourceAddress
        else -> null
    }
}

fun StateEntityDetailsResponseItemDetails.unstakeClaimTokenAddress(): String? {
    return when (val details = this) {
        is StateEntityDetailsResponseComponentDetails -> details.state?.value?.unstakeClaimTokenResourceAddress
        else -> null
    }
}

@Suppress("ComplexCondition", "TooManyFunctions", "LongMethod", "CyclomaticComplexMethod")
fun StateEntityDetailsResponseItemDetails.calculateResourceBehaviours(): List<ResourceBehaviour> {
    return if (isUsingDefaultRules()) {
        listOf(ResourceBehaviour.DEFAULT_RESOURCE)
    } else {
        val roleAssignments = toEntityRoleAssignments()
        val behaviors = mutableListOf<ResourceBehaviour>()

        if (roleAssignments?.isDefaultPerform(ResourceRole.Mint) == false && !roleAssignments.isDefaultPerform(ResourceRole.Burn)) {
            behaviors.add(ResourceBehaviour.PERFORM_MINT_BURN)
        } else if (roleAssignments?.isDefaultPerform(ResourceRole.Mint) == false) {
            behaviors.add(ResourceBehaviour.PERFORM_MINT)
        } else if (roleAssignments?.isDefaultPerform(ResourceRole.Burn) == false) {
            behaviors.add(ResourceBehaviour.PERFORM_BURN)
        }

        if (roleAssignments?.isDefaultUpdate(ResourceRole.Mint) == false && !roleAssignments.isDefaultUpdate(ResourceRole.Burn)) {
            behaviors.add(ResourceBehaviour.CHANGE_MINT_BURN)
        } else if (roleAssignments?.isDefaultUpdate(ResourceRole.Mint) == false) {
            behaviors.add(ResourceBehaviour.CHANGE_MINT)
        } else if (roleAssignments?.isDefaultUpdate(ResourceRole.Burn) == false) {
            behaviors.add(ResourceBehaviour.CHANGE_BURN)
        }

        if (roleAssignments?.isDefaultPerform(ResourceRole.Withdraw) == false && !roleAssignments.isDefaultPerform(ResourceRole.Deposit)) {
            behaviors.add(ResourceBehaviour.CANNOT_PERFORM_WITHDRAW_DEPOSIT)
        }

        // when both withdraw and deposit perform are set to defaults, but either withdraw or deposit for change
        // is set to not just a non default but specifically AllowAll (highly unusual, but it's possible)
        if (
            (roleAssignments?.isDefaultPerform(ResourceRole.Withdraw) == true && roleAssignments.isDefaultPerform(ResourceRole.Deposit)) &&
            roleAssignments.change(ResourceRole.Withdraw) == AccessRule.AllowAll ||
            roleAssignments?.change(ResourceRole.Deposit) == AccessRule.AllowAll
        ) {
            behaviors.add(ResourceBehaviour.CHANGE_WITHDRAW_DEPOSIT)
        }

        // 1. when both withdraw and deposit perform are set to defaults,
        // 2. but either withdraw or deposit change is set to non default
        // 3. (but neither set to AllowAll since that would be covered in the one above)
        val bothPerformSetToDefault = roleAssignments?.isDefaultPerform(ResourceRole.Withdraw) == true && roleAssignments.isDefaultPerform(
            ResourceRole.Deposit
        )
        val eitherChangeSetToNonDefault = roleAssignments?.isDefaultUpdate(ResourceRole.Deposit) == false ||
            roleAssignments?.isDefaultUpdate(ResourceRole.Withdraw) == false
        val neitherChangeSetToAllowAll = roleAssignments?.change(ResourceRole.Deposit) != AccessRule.AllowAll &&
            roleAssignments?.change(ResourceRole.Withdraw) != AccessRule.AllowAll
        if (bothPerformSetToDefault && eitherChangeSetToNonDefault && neitherChangeSetToAllowAll) {
            behaviors.add(ResourceBehaviour.FUTURE_MOVEMENT_WITHDRAW_DEPOSIT)
        }

        if (roleAssignments?.isDefaultPerform(ResourceRole.UpdateMetadata) == false) {
            behaviors.add(ResourceBehaviour.PERFORM_UPDATE_METADATA)
        }
        if (roleAssignments?.isDefaultUpdate(ResourceRole.UpdateMetadata) == false) {
            behaviors.add(ResourceBehaviour.CHANGE_UPDATE_METADATA)
        }

        if (roleAssignments?.isDefaultPerform(ResourceRole.Recall) == false) {
            behaviors.add(ResourceBehaviour.PERFORM_RECALL)
        }
        if (roleAssignments?.isDefaultUpdate(ResourceRole.Recall) == false) {
            behaviors.add(ResourceBehaviour.CHANGE_RECALL)
        }

        if (roleAssignments?.isDefaultPerform(ResourceRole.Freeze) == false) {
            behaviors.add(ResourceBehaviour.PERFORM_FREEZE)
        }
        if (roleAssignments?.isDefaultUpdate(ResourceRole.Freeze) == false) {
            behaviors.add(ResourceBehaviour.CHANGE_FREEZE)
        }

        if (type == StateEntityDetailsResponseItemDetailsType.nonFungibleResource) {
            if (roleAssignments?.isDefaultPerform(ResourceRole.UpdateNonFungibleData) == false) {
                behaviors.add(ResourceBehaviour.PERFORM_UPDATE_NON_FUNGIBLE_DATA)
            }
            if (roleAssignments?.isDefaultUpdate(ResourceRole.UpdateNonFungibleData) == false) {
                behaviors.add(ResourceBehaviour.CHANGE_UPDATE_NON_FUNGIBLE_DATA)
            }
        }

        behaviors
    }
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
    val defaultUpdateRule: AccessRule

    object Mint : ResourceRole {
        override val perform: String = "minter"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultUpdateRule: AccessRule = AccessRule.DenyAll
    }

    object Burn : ResourceRole {
        override val perform: String = "burner"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultUpdateRule: AccessRule = AccessRule.DenyAll
    }

    object Withdraw : ResourceRole {
        override val perform: String = "withdrawer"
        override val defaultPerformRule: AccessRule = AccessRule.AllowAll
        override val defaultUpdateRule: AccessRule = AccessRule.DenyAll
    }

    object Deposit : ResourceRole {
        override val perform: String = "depositor"
        override val defaultPerformRule: AccessRule = AccessRule.AllowAll
        override val defaultUpdateRule: AccessRule = AccessRule.DenyAll
    }

    object Recall : ResourceRole {
        override val perform: String = "recaller"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultUpdateRule: AccessRule = AccessRule.DenyAll
    }

    object Freeze : ResourceRole {
        override val perform: String = "freezer"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultUpdateRule: AccessRule = AccessRule.DenyAll
    }

    object UpdateMetadata : ResourceRole {
        override val perform: String = "metadata_setter"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultUpdateRule: AccessRule = AccessRule.DenyAll
    }

    object UpdateNonFungibleData : ResourceRole {
        override val perform: String = "non_fungible_data_updater"
        override val defaultPerformRule: AccessRule = AccessRule.DenyAll
        override val defaultUpdateRule: AccessRule = AccessRule.DenyAll
    }

    companion object {
        val rolesForFungibles = listOf(
            Burn,
            Mint,
            Deposit,
            Withdraw,
            UpdateMetadata,
            Recall,
            Freeze
        )

        val rolesForNonFungibles = listOf(
            Burn,
            Mint,
            Deposit,
            Withdraw,
            UpdateMetadata,
            Recall,
            Freeze,
            UpdateNonFungibleData
        )
    }
}

private fun StateEntityDetailsResponseItemDetails.toEntityRoleAssignments(): ComponentEntityRoleAssignments? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> details.roleAssignments
        is StateEntityDetailsResponseNonFungibleResourceDetails -> details.roleAssignments
        else -> null
    }
}

private fun ComponentEntityRoleAssignments.perform(action: ResourceRole): AccessRule? = propertyEntries.find { entry ->
    entry.roleKey.name == action.perform
}?.assignment?.explicitRule?.let { rule ->
    AccessRule.values().find { it.value == rule.type.name }
}

private fun ComponentEntityRoleAssignments.change(action: ResourceRole): AccessRule? = propertyEntries.find { entry ->
    entry.roleKey.name == action.change
}?.assignment?.explicitRule?.let { rule ->
    AccessRule.values().find { it.value == rule.type.name }
}

private fun ComponentEntityRoleAssignments.isDefaultPerform(action: ResourceRole): Boolean = perform(action) == action.defaultPerformRule

private fun ComponentEntityRoleAssignments.isDefaultUpdate(action: ResourceRole): Boolean = change(action) == action.defaultUpdateRule

private fun ComponentEntityRoleAssignments.isDefault(actions: List<ResourceRole>) = actions.all { action ->
    isDefaultPerform(action = action) && isDefaultUpdate(action = action)
}

private fun StateEntityDetailsResponseItemDetails.isUsingDefaultRules(): Boolean {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> details.roleAssignments.isDefault(ResourceRole.rolesForFungibles)
        is StateEntityDetailsResponseNonFungibleResourceDetails -> details.roleAssignments.isDefault(ResourceRole.rolesForNonFungibles)
        else -> false
    }
}

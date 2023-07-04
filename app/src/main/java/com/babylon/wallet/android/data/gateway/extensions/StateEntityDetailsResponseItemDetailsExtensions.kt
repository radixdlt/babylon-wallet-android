@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.AccessRulesChain
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.babylon.wallet.android.domain.model.behaviours.ResourceBehaviour

/**
 * The rules to determine behaviours was taken from here ->
 * https://radixdlt.atlassian.net/wiki/spaces/AT/pages/3007840284/Proposal+for+Resource+Behavior+Summarization
 */
enum class AccessRule(val value: String) {
    DenyAll("DenyAll"),
    AllowAll("AllowAll")
}

enum class ResourceRule(val value: String) {
    Mint("mint"),
    Burn("burn"),
    Withdraw("withdraw"),
    Deposit("deposit"),
    UpdateMetadata("set"),
    Recall("recall"),
    UpdateNonFungibleData("update_non_fungible_data"),
}

private fun StateEntityDetailsResponseItemDetails.toAccessRulesChain(): AccessRulesChain? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> {
            details.accessRulesChain
        }

        is StateEntityDetailsResponseNonFungibleResourceDetails -> {
            details.accessRulesChain
        }

        else -> {
            null
        }
    }
}

fun StateEntityDetailsResponseItemDetails.totalSupply(): String? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> {
            details.totalSupply
        }

        is StateEntityDetailsResponseNonFungibleResourceDetails -> {
            details.totalSupply
        }

        else -> {
            null
        }
    }
}

@Suppress("ComplexCondition", "TooManyFunctions", "LongMethod", "CyclomaticComplexMethod")
fun StateEntityDetailsResponseItemDetails.calculateResourceBehaviours(): List<ResourceBehaviour> {
    return if (isUsingDefaultRules()) {
        listOf(ResourceBehaviour.DEFAULT_RESOURCE)
    } else {
        val accessRulesChain = toAccessRulesChain()
        val mutableList = mutableListOf<ResourceBehaviour>()

        if (accessRulesChain?.performMintAccessRuleSetToNonDefault() == true && accessRulesChain.performBurnAccessRuleSetToNonDefault()
        ) {
            mutableList.add(ResourceBehaviour.PERFORM_MINT_BURN)
        } else if (accessRulesChain?.performMintAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.PERFORM_MINT)
        } else if (accessRulesChain?.performBurnAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.PERFORM_BURN)
        }

        if (accessRulesChain?.changeMintAccessRuleSetToNonDefault() == true && accessRulesChain.changeBurnAccessRuleSetToNonDefault()
        ) {
            mutableList.add(ResourceBehaviour.CHANGE_MINT_BURN)
        } else if (accessRulesChain?.changeMintAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.CHANGE_MINT)
        } else if (accessRulesChain?.changeBurnAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.CHANGE_BURN)
        }

        if (accessRulesChain?.performWithdrawAccessRuleSetToNonDefault() == true ||
            accessRulesChain?.performDepositAccessRuleSetToNonDefault() == true
        ) {
            mutableList.add(ResourceBehaviour.CANNOT_PERFORM_WITHDRAW_DEPOSIT)
        }

        // when both withdraw and deposit perform are set to defaults, but either withdraw or deposit for change
        // is set to not just a non default but specifically AllowAll (highly unusual, but it's possible)
        if ((
            accessRulesChain?.performWithdrawAccessRuleSetToDefault() == true &&
                accessRulesChain.performDepositAccessRuleSetToDefault()
            ) &&
            accessRulesChain.changeWithdrawAccessRuleSetToAllowAll() ||
            accessRulesChain?.changeDepositAccessRuleSetToAllowAll() == true
        ) {
            mutableList.add(ResourceBehaviour.CHANGE_WITHDRAW_DEPOSIT)
        }

        // 1. when both withdraw and deposit perform are set to defaults,
        // 2. but either withdraw or deposit change is set to non default
        // 3. (but neither set to AllowAll since that would be covered in the one above)
        val bothPerformSetToDefault = accessRulesChain?.performWithdrawAccessRuleSetToDefault() == true &&
            accessRulesChain.performDepositAccessRuleSetToDefault()
        val eitherChangeSetToNonDefault = accessRulesChain?.changeDepositAccessRuleSetToNonDefault() == true ||
            accessRulesChain?.changeWithdrawAccessRuleSetToNonDefault() == true
        val neitherChangeSetToAllowAll = accessRulesChain?.changeDepositAccessRuleNotSetToAllowAll() == true &&
            accessRulesChain.changeWithdrawAccessRuleNotSetToAllowAll()
        if (bothPerformSetToDefault && eitherChangeSetToNonDefault && neitherChangeSetToAllowAll) {
            mutableList.add(ResourceBehaviour.FUTURE_MOVEMENT_WITHDRAW_DEPOSIT)
        }

        if (accessRulesChain?.performUpdateMetadataAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.PERFORM_UPDATE_METADATA)
        }
        if (accessRulesChain?.changeUpdateMetadataAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.CHANGE_UPDATE_METADATA)
        }

        if (accessRulesChain?.performRecallAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.PERFORM_RECALL)
        }
        if (accessRulesChain?.changeRecallAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.CHANGE_RECALL)
        }

        if (this.type == StateEntityDetailsResponseItemDetailsType.nonFungibleResource) {
            if (accessRulesChain?.performUpdateNonFungibleMetadataAccessRuleSetToNonDefault() == true) {
                mutableList.add(ResourceBehaviour.PERFORM_UPDATE_NON_FUNGIBLE_DATA)
            }
            if (accessRulesChain?.changeUpdateNonFungibleMetadataAccessRuleSetToNonDefault() == true) {
                mutableList.add(ResourceBehaviour.CHANGE_UPDATE_NON_FUNGIBLE_DATA)
            }
        }

        mutableList
    }
}

private fun AccessRulesChain.performMintAccessRule(): AccessRule? {
    return this.rules.find { rule ->
        rule.key.name == ResourceRule.Mint.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.performMintAccessRuleSetToNonDefault(): Boolean {
    return performMintAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: run {
        false
    }
}

private fun AccessRulesChain.changeMintAccessRule(): AccessRule? {
    return this.mutability.find { rule ->
        rule.key.name == ResourceRule.Mint.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.changeMintAccessRuleSetToNonDefault(): Boolean {
    return changeMintAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.performBurnAccessRule(): AccessRule? {
    return this.rules.find { rule ->
        rule.key.name == ResourceRule.Burn.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.performBurnAccessRuleSetToNonDefault(): Boolean {
    return performBurnAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.changeBurnAccessRule(): AccessRule? {
    return this.mutability.find { rule ->
        rule.key.name == ResourceRule.Burn.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.changeBurnAccessRuleSetToNonDefault(): Boolean {
    return changeBurnAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.performWithdrawAccessRule(): AccessRule? {
    return this.rules.find { rule ->
        rule.key.name == ResourceRule.Withdraw.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.performWithdrawAccessRuleSetToNonDefault(): Boolean {
    return performWithdrawAccessRule()?.let {
        it != AccessRule.AllowAll
    } ?: false
}

private fun AccessRulesChain.performWithdrawAccessRuleSetToDefault(): Boolean {
    return performWithdrawAccessRule()?.let {
        it == AccessRule.AllowAll
    } ?: false
}

private fun AccessRulesChain.changeWithdrawAccessRule(): AccessRule? {
    return this.mutability.find { rule ->
        rule.key.name == ResourceRule.Withdraw.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.performDepositAccessRule(): AccessRule? {
    return this.rules.find { rule ->
        rule.key.name == ResourceRule.Deposit.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.performDepositAccessRuleSetToNonDefault(): Boolean {
    return performDepositAccessRule()?.let {
        it != AccessRule.AllowAll
    } ?: false
}

private fun AccessRulesChain.performDepositAccessRuleSetToDefault(): Boolean {
    return performDepositAccessRule()?.let {
        it == AccessRule.AllowAll
    } ?: false
}

private fun AccessRulesChain.changeDepositAccessRule(): AccessRule? {
    return this.mutability.find { rule ->
        rule.key.name == ResourceRule.Deposit.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.changeDepositAccessRuleSetToAllowAll(): Boolean {
    return changeDepositAccessRule()?.let {
        it == AccessRule.AllowAll
    } ?: false
}

private fun AccessRulesChain.changeWithdrawAccessRuleSetToAllowAll(): Boolean {
    return changeWithdrawAccessRule()?.let {
        it == AccessRule.AllowAll
    } ?: false
}

private fun AccessRulesChain.changeDepositAccessRuleSetToNonDefault(): Boolean {
    return this.mutability.find { rule ->
        rule.key.name == ResourceRule.Deposit.value
    }?.rule?.type?.let { type ->
        type != AccessRule.DenyAll.value
    } ?: true
}

private fun AccessRulesChain.changeWithdrawAccessRuleSetToNonDefault(): Boolean {
    return this.mutability.find { rule ->
        rule.key.name == ResourceRule.Withdraw.value
    }?.rule?.type?.let { type ->
        type != AccessRule.DenyAll.value
    } ?: true
}

private fun AccessRulesChain.changeDepositAccessRuleNotSetToAllowAll(): Boolean {
    return changeDepositAccessRule()?.let {
        it != AccessRule.AllowAll // Not AllowAll
    } ?: true
}

private fun AccessRulesChain.changeWithdrawAccessRuleNotSetToAllowAll(): Boolean {
    return changeWithdrawAccessRule()?.let {
        it != AccessRule.AllowAll // Not AllowAll
    } ?: true
}

private fun AccessRulesChain.performUpdateMetadataAccessRule(): AccessRule? {
    return this.rules.find { rule ->
        rule.key.name == ResourceRule.UpdateMetadata.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.performUpdateMetadataAccessRuleSetToNonDefault(): Boolean {
    return performUpdateMetadataAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.changeUpdateMetadataAccessRule(): AccessRule? {
    return this.mutability.find { rule ->
        rule.key.name == ResourceRule.UpdateMetadata.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.changeUpdateMetadataAccessRuleSetToNonDefault(): Boolean {
    return changeUpdateMetadataAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.performRecallAccessRule(): AccessRule? {
    return this.rules.find { rule ->
        rule.key.name == ResourceRule.Recall.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.performRecallAccessRuleSetToNonDefault(): Boolean {
    return performRecallAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.changeRecallAccessRule(): AccessRule? {
    return this.mutability.find { rule ->
        rule.key.name == ResourceRule.Recall.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.changeRecallAccessRuleSetToNonDefault(): Boolean {
    return changeRecallAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.performUpdateNonFungibleMetadataAccessRule(): AccessRule? {
    return this.rules.find { rule ->
        rule.key.name == ResourceRule.UpdateNonFungibleData.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.performUpdateNonFungibleMetadataAccessRuleSetToNonDefault(): Boolean {
    return performUpdateNonFungibleMetadataAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.changeUpdateNonFungibleMetadataAccessRule(): AccessRule? {
    return this.mutability.find { rule ->
        rule.key.name == ResourceRule.UpdateNonFungibleData.value
    }?.rule?.type?.let { type ->
        AccessRule.values().find { it.value == type }
    }
}

private fun AccessRulesChain.changeUpdateNonFungibleMetadataAccessRuleSetToNonDefault(): Boolean {
    return changeUpdateNonFungibleMetadataAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

@Suppress("CyclomaticComplexMethod")
private fun StateEntityDetailsResponseItemDetails.isUsingDefaultRules(): Boolean {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> {
            val accessRulesChain = details.accessRulesChain
            accessRulesChain.performBurnAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.changeBurnAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performMintAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.changeMintAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performDepositAccessRule()?.value == AccessRule.AllowAll.value &&
                accessRulesChain.changeDepositAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performWithdrawAccessRule()?.value == AccessRule.AllowAll.value &&
                accessRulesChain.changeWithdrawAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performUpdateMetadataAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.changeUpdateMetadataAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performRecallAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.changeRecallAccessRule()?.value == AccessRule.DenyAll.value
        }

        is StateEntityDetailsResponseNonFungibleResourceDetails -> {
            val accessRulesChain = details.accessRulesChain
            accessRulesChain.performBurnAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.changeBurnAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performMintAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.changeMintAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performDepositAccessRule()?.value == AccessRule.AllowAll.value &&
                accessRulesChain.changeDepositAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performWithdrawAccessRule()?.value == AccessRule.AllowAll.value &&
                accessRulesChain.changeWithdrawAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performUpdateMetadataAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.changeUpdateMetadataAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performRecallAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.changeRecallAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.performUpdateNonFungibleMetadataAccessRule()?.value == AccessRule.DenyAll.value &&
                accessRulesChain.changeUpdateNonFungibleMetadataAccessRule()?.value == AccessRule.DenyAll.value
        }

        else -> {
            false
        }
    }
}

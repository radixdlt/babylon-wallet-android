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
    AllowAll("AllowAll"),
    Protected("Protected")
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
    return if (toAccessRulesChain()?.isUsingDefaultRules() == true) {
        listOf(ResourceBehaviour.DEFAULT_RESOURCE)
    } else {
        val mutableList = mutableListOf<ResourceBehaviour>()

        if (toAccessRulesChain()?.performMintAccessRuleSetToNonDefault() == true &&
            toAccessRulesChain()?.performBurnAccessRuleSetToNonDefault() == true
        ) {
            mutableList.add(ResourceBehaviour.PERFORM_MINT_BURN)
        } else if (toAccessRulesChain()?.performMintAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.PERFORM_MINT)
        } else if (toAccessRulesChain()?.performBurnAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.PERFORM_BURN)
        }

        if (toAccessRulesChain()?.changeMintAccessRuleSetToNonDefault() == true &&
            toAccessRulesChain()?.changeBurnAccessRuleSetToNonDefault() == true
        ) {
            mutableList.add(ResourceBehaviour.CHANGE_MINT_BURN)
        } else if (toAccessRulesChain()?.changeMintAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.CHANGE_MINT)
        } else if (toAccessRulesChain()?.changeBurnAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.CHANGE_BURN)
        }

        if (toAccessRulesChain()?.performWithdrawAccessRuleSetToNonDefault() == true ||
            toAccessRulesChain()?.performDepositAccessRuleSetToNonDefault() == true
        ) {
            mutableList.add(ResourceBehaviour.CANNOT_PERFORM_WITHDRAW_DEPOSIT)
        }

        // when both withdraw and deposit perform are set to defaults, but either withdraw or deposit for change
        // is set to not just a non default but specifically AllowAll (highly unusual, but it's possible)
        if ((
            toAccessRulesChain()?.performWithdrawAccessRuleSetToDefault() == true &&
                toAccessRulesChain()?.performDepositAccessRuleSetToDefault() == true
            ) &&
            toAccessRulesChain()?.changeWithdrawAccessRuleSetToAllowAll() == true ||
            toAccessRulesChain()?.changeDepositAccessRuleSetToAllowAll() == true
        ) {
            mutableList.add(ResourceBehaviour.CHANGE_WITHDRAW_DEPOSIT)
        }

        // when both withdraw and deposit perform are set to defaults,
        // but either withdraw or deposit change is set to non default (but neither set to AllowAll
        // since that would be covered in the one above)
        if ((
            toAccessRulesChain()?.performWithdrawAccessRuleSetToDefault() == true &&
                toAccessRulesChain()?.performDepositAccessRuleSetToDefault() == true
            ) &&
            toAccessRulesChain()?.changeDepositAccessRuleSetToNonDefaultExceptAllowAll() == true &&
            toAccessRulesChain()?.changeWithdrawAccessRuleSetToNonDefaultExceptAllowAll() == true
        ) {
            mutableList.add(ResourceBehaviour.FUTURE_MOVEMENT_WITHDRAW_DEPOSIT)
        }

        if (toAccessRulesChain()?.performUpdateMetadataAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.PERFORM_UPDATE_METADATA)
        }
        if (toAccessRulesChain()?.changeUpdateMetadataAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.CHANGE_UPDATE_METADATA)
        }

        if (toAccessRulesChain()?.performRecallAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.PERFORM_RECALL)
        }
        if (toAccessRulesChain()?.changeRecallAccessRuleSetToNonDefault() == true) {
            mutableList.add(ResourceBehaviour.CHANGE_RECALL)
        }

        if (this.type == StateEntityDetailsResponseItemDetailsType.nonFungibleResource) {
            if (toAccessRulesChain()?.performUpdateNonFungibleMetadataAccessRuleSetToNonDefault() == true) {
                mutableList.add(ResourceBehaviour.PERFORM_UPDATE_NON_FUNGIBLE_DATA)
            }
            if (toAccessRulesChain()?.changeUpdateNonFungibleMetadataAccessRuleSetToNonDefault() == true) {
                mutableList.add(ResourceBehaviour.CHANGE_UPDATE_NON_FUNGIBLE_DATA)
            }
        }

        mutableList
    }
}

private fun AccessRulesChain.performMintAccessRule(): AccessRule? {
    return this.method_auth.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Mint.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
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
    return this.method_auth_mutability.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Mint.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.changeMintAccessRuleSetToNonDefault(): Boolean {
    return changeMintAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.performBurnAccessRule(): AccessRule? {
    return this.method_auth.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Burn.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.performBurnAccessRuleSetToNonDefault(): Boolean {
    return performBurnAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.changeBurnAccessRule(): AccessRule? {
    return this.method_auth_mutability.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Burn.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.changeBurnAccessRuleSetToNonDefault(): Boolean {
    return changeBurnAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.performWithdrawAccessRule(): AccessRule? {
    return this.method_auth.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Withdraw.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
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
    return this.method_auth_mutability.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Withdraw.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.performDepositAccessRule(): AccessRule? {
    return this.method_auth.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Deposit.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
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
    return this.method_auth_mutability.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Deposit.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
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

private fun AccessRulesChain.changeDepositAccessRuleSetToNonDefaultExceptAllowAll(): Boolean {
    return changeDepositAccessRule()?.let {
        it != AccessRule.DenyAll && // Non default
            it != AccessRule.AllowAll // Not AllowAll
    } ?: false
}

private fun AccessRulesChain.changeWithdrawAccessRuleSetToNonDefaultExceptAllowAll(): Boolean {
    return changeWithdrawAccessRule()?.let {
        it != AccessRule.DenyAll && // Non default
            it != AccessRule.AllowAll // Not AllowAll
    } ?: false
}

private fun AccessRulesChain.performUpdateMetadataAccessRule(): AccessRule? {
    return this.method_auth.find { methodAuth ->
        methodAuth.method.name == ResourceRule.UpdateMetadata.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.performUpdateMetadataAccessRuleSetToNonDefault(): Boolean {
    return performUpdateMetadataAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.changeUpdateMetadataAccessRule(): AccessRule? {
    return this.method_auth_mutability.find { methodAuth ->
        methodAuth.method.name == ResourceRule.UpdateMetadata.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.changeUpdateMetadataAccessRuleSetToNonDefault(): Boolean {
    return changeUpdateMetadataAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.performRecallAccessRule(): AccessRule? {
    return this.method_auth.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Recall.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.performRecallAccessRuleSetToNonDefault(): Boolean {
    return performRecallAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.changeRecallAccessRule(): AccessRule? {
    return this.method_auth_mutability.find { methodAuth ->
        methodAuth.method.name == ResourceRule.Recall.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.changeRecallAccessRuleSetToNonDefault(): Boolean {
    return changeRecallAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.performUpdateNonFungibleMetadataAccessRule(): AccessRule? {
    return this.method_auth.find { methodAuth ->
        methodAuth.method.name == ResourceRule.UpdateNonFungibleData.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.performUpdateNonFungibleMetadataAccessRuleSetToNonDefault(): Boolean {
    return performUpdateNonFungibleMetadataAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.changeUpdateNonFungibleMetadataAccessRule(): AccessRule? {
    return this.method_auth_mutability.find { methodAuth ->
        methodAuth.method.name == ResourceRule.UpdateNonFungibleData.value
    }?.access_rule_reference?.access_rule?.type?.let { type ->
        AccessRule.valueOf(type)
    }
}

private fun AccessRulesChain.changeUpdateNonFungibleMetadataAccessRuleSetToNonDefault(): Boolean {
    return changeUpdateNonFungibleMetadataAccessRule()?.let {
        it != AccessRule.DenyAll
    } ?: false
}

private fun AccessRulesChain.isUsingDefaultRules(): Boolean =
    performBurnAccessRule()?.value == AccessRule.DenyAll.value &&
        changeBurnAccessRule()?.value == AccessRule.DenyAll.value &&
        performMintAccessRule()?.value == AccessRule.DenyAll.value &&
        changeMintAccessRule()?.value == AccessRule.DenyAll.value &&
        performDepositAccessRule()?.value == AccessRule.AllowAll.value &&
        changeDepositAccessRule()?.value == AccessRule.DenyAll.value &&
        performWithdrawAccessRule()?.value == AccessRule.AllowAll.value &&
        changeWithdrawAccessRule()?.value == AccessRule.DenyAll.value &&
        performUpdateMetadataAccessRule()?.value == AccessRule.DenyAll.value &&
        changeUpdateMetadataAccessRule()?.value == AccessRule.DenyAll.value &&
        performRecallAccessRule()?.value == AccessRule.DenyAll.value &&
        changeRecallAccessRule()?.value == AccessRule.DenyAll.value &&
        performUpdateNonFungibleMetadataAccessRule()?.value == AccessRule.DenyAll.value &&
        changeUpdateNonFungibleMetadataAccessRule()?.value == AccessRule.DenyAll.value

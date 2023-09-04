package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.gateway.extensions.ResourceRole
import com.babylon.wallet.android.data.gateway.extensions.calculateResourceBehaviours
import com.babylon.wallet.android.data.gateway.generated.models.AccessRule
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignmentEntry
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignmentEntryAssignment
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignments
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleIdType
import com.babylon.wallet.android.data.gateway.generated.models.ObjectModuleId
import com.babylon.wallet.android.data.gateway.generated.models.RoleAssignmentResolution
import com.babylon.wallet.android.data.gateway.generated.models.RoleKey
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.babylon.wallet.android.domain.model.behaviours.ResourceBehaviour
import org.junit.Assert
import org.junit.Test

class ResourceBehaviourTest {

    @Test
    fun `given update_non_fungible_data perform and change rules set to defaults, verify no behaviours`() {
        // given
        val expectedBehaviours = listOf(ResourceBehaviour.DEFAULT_RESOURCE)
        val response = StateEntityDetailsResponseNonFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            nonFungibleIdType = NonFungibleIdType.integer,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultNonFungibleAccessRules)
        )

        // when
        val behaviours = response.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_non_fungible_data change rules set to NON defaults, verify CHANGE update_non_fungible_data behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_UPDATE_NON_FUNGIBLE_DATA
        )
        val response = StateEntityDetailsResponseNonFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            nonFungibleIdType = NonFungibleIdType.integer,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultNonFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.UpdateNonFungibleData.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = response.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_non_fungible_data perform rules set to NON defaults, verify PERFORM update_non_fungible_data behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_UPDATE_NON_FUNGIBLE_DATA
        )
        val response = StateEntityDetailsResponseNonFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            nonFungibleIdType = NonFungibleIdType.integer,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultNonFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.UpdateNonFungibleData.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = response.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_non_fungible_data perform and change rules set to NON defaults, verify PERFORM and CHANGE update_non_fungible_data behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_UPDATE_NON_FUNGIBLE_DATA
        )
        val response = StateEntityDetailsResponseNonFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            nonFungibleIdType = NonFungibleIdType.integer,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultNonFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.UpdateNonFungibleData.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.UpdateNonFungibleData.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = response.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given recall perform rules set to NON defaults, verify PERFORM recall behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_RECALL
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Recall.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given recall change rules set to NON defaults, verify CHANGE recall behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_RECALL
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Recall.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given recall perform and change rules set to NON defaults, verify PERFORM and CHANGE recall behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_RECALL
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Recall.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Recall.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given freeze perform rules set to NON defaults, verify PERFORM freeze behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_FREEZE
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Freeze.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given freeze change rules set to NON defaults, verify CHANGE freeze behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_FREEZE
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Freeze.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given freeze perform and change rules set to NON defaults, verify PERFORM and CHANGE freeze behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_FREEZE
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Freeze.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Freeze.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_metadata perform rules set to NON defaults, verify PERFORM update_metadata behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_UPDATE_METADATA
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.UpdateMetadata.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_metadata change rules set to NON defaults, verify CHANGE update_metadata behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_UPDATE_METADATA
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.UpdateMetadata.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_metadata perform and change rules set to NON defaults, verify PERFORM and CHANGE update_metadata behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_UPDATE_METADATA
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.UpdateMetadata.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.UpdateMetadata.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given mint perform rules set to NON defaults, verify PERFORM mint behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_MINT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Mint.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given mint change rules set to NON defaults, verify CHANGE mint behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_MINT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Mint.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given mint perform and change rules set to NON defaults, verify PERFORM and CHANGE mint behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_MINT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Mint.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Mint.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn perform rules set to NON defaults, verify PERFORM burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Burn.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn change rules set to NON defaults, verify CHANGE burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Burn.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn perform and change rules set to NON defaults, verify PERFORM and CHANGE burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Burn.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Burn.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn and mint perform rules set to NON defaults, verify PERFORM mint and burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_MINT_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Burn.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Burn.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.DenyAll))
                    ResourceRole.Mint.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Mint.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.DenyAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn and mint change rules set to NON defaults, verify CHANGE mint and burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_MINT_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Burn.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.DenyAll))
                    ResourceRole.Burn.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Mint.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.DenyAll))
                    ResourceRole.Mint.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given perform withdraw or deposit rules set to NON defaults, verify CANNOT_PERFORM_WITHDRAW_DEPOSIT behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CANNOT_PERFORM_WITHDRAW_DEPOSIT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Deposit.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.DenyAll))
                    ResourceRole.Deposit.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.DenyAll))
                    ResourceRole.Withdraw.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.DenyAll))
                    ResourceRole.Withdraw.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.DenyAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given perform withdraw and deposit rules set to defaults and either change withdraw or deposit is NON default exactly is AllowAll, verify CHANGE_WITHDRAW_DEPOSIT behaviour`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_WITHDRAW_DEPOSIT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Deposit.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Deposit.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.DenyAll))
                    ResourceRole.Withdraw.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Withdraw.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    else -> it
                }
            })
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given perform withdraw and deposit rules set to defaults and either change withdraw or deposit is NON default but neither is AllowAll, verify FUTURE_MOVEMENT_WITHDRAW_DEPOSIT behaviour`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.FUTURE_MOVEMENT_WITHDRAW_DEPOSIT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules.map {
                when (it.roleKey.name) {
                    ResourceRole.Deposit.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Deposit.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.Protected))
                    ResourceRole.Withdraw.perform -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.AllowAll))
                    ResourceRole.Withdraw.change -> it.updateRoleAccessRule(AccessRule(AccessRule.Type.Protected))
                    else -> it
                }
            })

        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given resource is fungible and all rules are set to defaults, verify DEFAULT_RESOURCE behaviour`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.DEFAULT_RESOURCE
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(defaultFungibleAccessRules)
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given resource is nft and all rules are set to defaults, verify DEFAULT_RESOURCE behaviour`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.DEFAULT_RESOURCE
        )
        val fungibleResource = StateEntityDetailsResponseNonFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            nonFungibleIdType = NonFungibleIdType.integer,
            totalSupply = "",
            totalBurned = "",
            totalMinted = "",
            roleAssignments = ComponentEntityRoleAssignments(
                propertyEntries = defaultNonFungibleAccessRules
            ),
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    companion object {
        private val defaultFungibleAccessRules = ResourceRole.rolesForFungibles.map {
            listOf(
                ComponentEntityRoleAssignmentEntry(
                    roleKey = RoleKey(it.perform, module = ObjectModuleId.main),
                    assignment = ComponentEntityRoleAssignmentEntryAssignment(
                        resolution = RoleAssignmentResolution.explicit,
                        explicitRule = AccessRule(AccessRule.Type.valueOf(it.defaultPerformRule.value))
                    )
                ),
                ComponentEntityRoleAssignmentEntry(
                    roleKey = RoleKey(it.change, module = ObjectModuleId.main),
                    assignment = ComponentEntityRoleAssignmentEntryAssignment(
                        resolution = RoleAssignmentResolution.explicit,
                        explicitRule = AccessRule(AccessRule.Type.valueOf(it.defaultUpdateRule.value))
                    )
                )
            )
        }.flatten()

        private val defaultNonFungibleAccessRules = ResourceRole.rolesForNonFungibles.map {
            listOf(
                ComponentEntityRoleAssignmentEntry(
                    roleKey = RoleKey(it.perform, module = ObjectModuleId.main),
                    assignment = ComponentEntityRoleAssignmentEntryAssignment(
                        resolution = RoleAssignmentResolution.explicit,
                        explicitRule = AccessRule(AccessRule.Type.valueOf(it.defaultPerformRule.value))
                    )
                ),
                ComponentEntityRoleAssignmentEntry(
                    roleKey = RoleKey(it.change, module = ObjectModuleId.main),
                    assignment = ComponentEntityRoleAssignmentEntryAssignment(
                        resolution = RoleAssignmentResolution.explicit,
                        explicitRule = AccessRule(AccessRule.Type.valueOf(it.defaultUpdateRule.value))
                    )
                )
            )
        }.flatten()
    }

    private fun ComponentEntityRoleAssignmentEntry.updateRoleAccessRule(accessRule: AccessRule): ComponentEntityRoleAssignmentEntry {
        return copy(
            assignment = ComponentEntityRoleAssignmentEntryAssignment(
                resolution = RoleAssignmentResolution.explicit,
                explicitRule = accessRule
            )
        )
    }
}

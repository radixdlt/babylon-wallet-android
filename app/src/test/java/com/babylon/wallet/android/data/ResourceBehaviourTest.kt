package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.gateway.extensions.assetBehaviours
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignmentEntry
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignmentEntryAssignment
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignmentOwner
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignments
import com.babylon.wallet.android.data.gateway.generated.models.ObjectModuleId
import com.babylon.wallet.android.data.gateway.generated.models.RoleAssignmentResolution
import com.babylon.wallet.android.data.gateway.generated.models.RoleKey
import com.babylon.wallet.android.data.gateway.model.AccessRule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import rdx.works.core.domain.assets.AssetBehaviour
import rdx.works.core.domain.assets.AssetBehaviours

@RunWith(Parameterized::class)
class ResourceBehaviourTest(
    private val testItem: TestItem
) {

    @Test
    fun `Verify vector`() {
        testItem.validate()
    }

    companion object {

        @JvmStatic
        @Parameters()
        fun parameters(): Collection<TestItem> = listOf(
            fungible(behaviours = setOf(AssetBehaviour.SIMPLE_ASSET)),
            nft(behaviours = setOf(AssetBehaviour.SIMPLE_ASSET)),
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_INCREASABLE_BY_ANYONE)) {
                replace("minter", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_INCREASABLE)) {
                replace("minter", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_INCREASABLE_BY_ANYONE)) {
                replace("minter_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_INCREASABLE)) {
                replace("minter_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_DECREASABLE_BY_ANYONE)) {
                replace("burner", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_DECREASABLE)) {
                replace("burner", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_DECREASABLE_BY_ANYONE)) {
                replace("burner_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_DECREASABLE)) {
                replace("burner_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.REMOVABLE_BY_THIRD_PARTY)) {
                replace("recaller", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.REMOVABLE_BY_ANYONE)) {
                replace("recaller", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            fungible(behaviours = setOf(AssetBehaviour.REMOVABLE_BY_THIRD_PARTY)) {
                replace("recaller_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.REMOVABLE_BY_ANYONE)) {
                replace("recaller_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            nft(behaviours = setOf(AssetBehaviour.NFT_DATA_CHANGEABLE)) {
                replace("non_fungible_data_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            nft(behaviours = setOf(AssetBehaviour.NFT_DATA_CHANGEABLE_BY_ANYONE)) {
                replace("non_fungible_data_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            nft(behaviours = setOf(AssetBehaviour.NFT_DATA_CHANGEABLE)) {
                replace("non_fungible_data_updater_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            nft(behaviours = setOf(AssetBehaviour.NFT_DATA_CHANGEABLE_BY_ANYONE)) {
                replace("non_fungible_data_updater_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            fungible(behaviours = setOf(AssetBehaviour.INFORMATION_CHANGEABLE_BY_ANYONE)) {
                replace("metadata_setter", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            fungible(behaviours = setOf(AssetBehaviour.INFORMATION_CHANGEABLE)) {
                replace("metadata_setter", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_FLEXIBLE)) {
                replace("minter", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
                    .replace("burner", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.SUPPLY_FLEXIBLE_BY_ANYONE)) {
                replace("minter", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
                    .replace("burner", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            fungible(behaviours = setOf(AssetBehaviour.MOVEMENT_RESTRICTED)) {
                replace("withdrawer", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
                    .replace("depositor", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
            },
            fungible(behaviours = setOf(AssetBehaviour.MOVEMENT_RESTRICTED)) {
                replace("withdrawer", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
                    .replace("depositor", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.MOVEMENT_RESTRICTABLE_IN_FUTURE_BY_ANYONE)) {
                replace("withdrawer_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll)
                    .replace("depositor_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(behaviours = setOf(AssetBehaviour.MOVEMENT_RESTRICTABLE_IN_FUTURE)) {
                replace("withdrawer_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
                    .replace("depositor_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
            },
            fungible(
                ownerRule = AccessRule.Type.AllowAll,
                behaviours = setOf(AssetBehaviour.SUPPLY_INCREASABLE_BY_ANYONE)
            ) {
                replace("minter", RoleAssignmentResolution.Owner, null)
            },
            fungible(
                ownerRule = AccessRule.Type.Protected,
                behaviours = setOf(AssetBehaviour.SUPPLY_INCREASABLE)
            ) {
                replace("minter", RoleAssignmentResolution.Owner, null)
            },
            fungible(
                ownerRule = AccessRule.Type.Protected,
                behaviours = setOf(
                    AssetBehaviour.REMOVABLE_BY_THIRD_PARTY,
                    AssetBehaviour.INFORMATION_CHANGEABLE,
                    AssetBehaviour.SUPPLY_FLEXIBLE
                )
            ) {
                replace("burner", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
                    .replace("minter", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
                    .replace("recaller", RoleAssignmentResolution.Explicit, AccessRule.Type.Protected)
                    .replace("burner_updater", RoleAssignmentResolution.Owner, null)
                    .replace("minter_updater", RoleAssignmentResolution.Owner, null)
                    .replace("recaller_updater", RoleAssignmentResolution.Owner, null)
                    .replace("metadata_setter", RoleAssignmentResolution.Owner, null)
                    .replace("metadata_setter_updater", RoleAssignmentResolution.Owner, null)
            }
        )

        private fun fungible(
            ownerRule: AccessRule.Type = AccessRule.Type.AllowAll,
            behaviours: AssetBehaviours,
            entries: List<ComponentEntityRoleAssignmentEntry>.() -> List<ComponentEntityRoleAssignmentEntry> = { this }
        ): TestItem = TestItem(
            assignments = ComponentEntityRoleAssignments(
                owner = ComponentEntityRoleAssignmentOwner(AccessRule(ownerRule)),
                propertyEntries = entries(defaultEntriesFungibles)
            ),
            behaviours = behaviours
        )

        private fun nft(
            ownerRule: AccessRule.Type = AccessRule.Type.AllowAll,
            behaviours: AssetBehaviours,
            entries: List<ComponentEntityRoleAssignmentEntry>.() -> List<ComponentEntityRoleAssignmentEntry> = { this }
        ): TestItem = TestItem(
            assignments = ComponentEntityRoleAssignments(
                owner = ComponentEntityRoleAssignmentOwner(AccessRule(ownerRule)),
                propertyEntries = entries(defaultEntriesNonFungibles)
            ),
            behaviours = behaviours
        )

        data class TestItem(
            val assignments: ComponentEntityRoleAssignments,
            val behaviours: AssetBehaviours
        ) {

            fun validate() = assertEquals(behaviours, assignments.assetBehaviours())

        }

        private val defaultEntriesFungibles = listOf(
            entry("minter", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("minter_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("burner", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("burner_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("withdrawer", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll),
            entry("withdrawer_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("depositor", RoleAssignmentResolution.Explicit, AccessRule.Type.AllowAll),
            entry("depositor_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("recaller", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("recaller_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("freezer", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("freezer_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("metadata_setter", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("metadata_setter_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
        )

        private val defaultEntriesNonFungibles = defaultEntriesFungibles + listOf(
            entry("non_fungible_data_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
            entry("non_fungible_data_updater_updater", RoleAssignmentResolution.Explicit, AccessRule.Type.DenyAll),
        )

        private fun entry(role: String, resolution: RoleAssignmentResolution, rule: AccessRule.Type?): ComponentEntityRoleAssignmentEntry {
            val updater = if (role == "non_fungible_data_updater") {
                "non_fungible_data_updater_updater"
            } else if (!role.endsWith("_updater")) {
                "${role}_updater"
            } else {
                role
            }

            return ComponentEntityRoleAssignmentEntry(
                roleKey = RoleKey(name = role, module = ObjectModuleId.Main),
                assignment = ComponentEntityRoleAssignmentEntryAssignment(
                    resolution = resolution,
                    explicitRule = rule?.let { AccessRule(it) }
                ),
                updaterRoles = listOf(
                    RoleKey(name = updater, module = ObjectModuleId.Main)
                )
            )
        }

        private fun List<ComponentEntityRoleAssignmentEntry>.replace(
            role: String,
            resolution: RoleAssignmentResolution,
            rule: AccessRule.Type?
        ) = map { entry ->
            if (entry.roleKey.name == role) {
                entry.copy(
                    assignment = ComponentEntityRoleAssignmentEntryAssignment(
                        resolution = resolution,
                        explicitRule = rule?.let { AccessRule(it) }
                    )
                )
            } else {
                entry
            }
        }
    }
}

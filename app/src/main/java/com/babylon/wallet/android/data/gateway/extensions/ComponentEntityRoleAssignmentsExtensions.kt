package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.AccessRule
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignmentEntry
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignmentOwner
import com.babylon.wallet.android.data.gateway.generated.models.ComponentEntityRoleAssignments
import com.babylon.wallet.android.data.gateway.generated.models.RoleAssignmentResolution
import com.babylon.wallet.android.domain.model.behaviours.AssetBehaviour
import com.babylon.wallet.android.domain.model.behaviours.AssetBehaviours

@Suppress("LongMethod")
fun ComponentEntityRoleAssignments.assetBehaviours(): AssetBehaviours {
    val behaviors = mutableSetOf<AssetBehaviour>()

    if (setOf(performer(Role.WITHDRAWER), performer(Role.DEPOSITOR)) != setOf(Assignment.ANYONE)) {
        behaviors.add(AssetBehaviour.MOVEMENT_RESTRICTED)
    } else {
        val moverUpdaters = setOf(updater(Role.WITHDRAWER), updater(Role.DEPOSITOR))
        if (moverUpdaters.contains(Assignment.ANYONE)) {
            behaviors.add(AssetBehaviour.MOVEMENT_RESTRICTABLE_IN_FUTURE_BY_ANYONE)
        } else if (moverUpdaters.contains(Assignment.SOMEONE)) {
            behaviors.add(AssetBehaviour.MOVEMENT_RESTRICTABLE_IN_FUTURE)
        }
    }

    behaviors
        .add(
            assignments = this,
            role = Role.MINTER,
            ifSomeone = AssetBehaviour.SUPPLY_INCREASABLE,
            ifAnyone = AssetBehaviour.SUPPLY_INCREASABLE_BY_ANYONE
        )
        .add(
            assignments = this,
            role = Role.BURNER,
            ifSomeone = AssetBehaviour.SUPPLY_DECREASABLE,
            ifAnyone = AssetBehaviour.SUPPLY_DECREASABLE_BY_ANYONE
        )
        .add(
            assignments = this,
            role = Role.RECALLER,
            ifSomeone = AssetBehaviour.REMOVABLE_BY_THIRD_PARTY,
            ifAnyone = AssetBehaviour.REMOVABLE_BY_ANYONE
        )
        .add(
            assignments = this,
            role = Role.FREEZER,
            ifSomeone = AssetBehaviour.FREEZABLE_BY_THIRD_PARTY,
            ifAnyone = AssetBehaviour.FREEZABLE_BY_ANYONE
        )
        .add(
            assignments = this,
            role = Role.NON_FUNGIBLE_DATA_UPDATER,
            ifSomeone = AssetBehaviour.NFT_DATA_CHANGEABLE,
            ifAnyone = AssetBehaviour.NFT_DATA_CHANGEABLE_BY_ANYONE
        )
        .add(
            assignments = this,
            role = Role.METADATA_SETTER,
            ifSomeone = AssetBehaviour.INFORMATION_CHANGEABLE,
            ifAnyone = AssetBehaviour.INFORMATION_CHANGEABLE_BY_ANYONE,
        )

    if (behaviors.isEmpty()) {
        return setOf(AssetBehaviour.SIMPLE_ASSET)
    }

    return behaviors
        .replace(
            behaviors = setOf(AssetBehaviour.SUPPLY_INCREASABLE_BY_ANYONE, AssetBehaviour.SUPPLY_DECREASABLE_BY_ANYONE),
            with = AssetBehaviour.SUPPLY_FLEXIBLE_BY_ANYONE
        )
        .replace(
            behaviors = setOf(AssetBehaviour.SUPPLY_INCREASABLE, AssetBehaviour.SUPPLY_DECREASABLE),
            with = AssetBehaviour.SUPPLY_FLEXIBLE
        )
        .sorted()
        .toSet()
}

private fun MutableSet<AssetBehaviour>.add(
    assignments: ComponentEntityRoleAssignments,
    role: Role,
    ifSomeone: AssetBehaviour,
    ifAnyone: AssetBehaviour
) = apply {
    val assigned = setOf(assignments.performer(role), assignments.updater(role))
    if (assigned.contains(Assignment.ANYONE)) {
        add(ifAnyone)
    } else if (assigned.contains(Assignment.SOMEONE)) {
        add(ifSomeone)
    }
}

private fun MutableSet<AssetBehaviour>.replace(
    behaviors: Set<AssetBehaviour>,
    with: AssetBehaviour
): MutableSet<AssetBehaviour> = apply {
    if (containsAll(behaviors)) {
        removeAll(behaviors)
        add(with)
    }
}

private fun ComponentEntityRoleAssignments.performer(role: Role): Assignment = propertyEntries.find {
    it.roleKey.name == role.value
}?.parsedAssignment(owner) ?: Assignment.UNKNOWN

private fun ComponentEntityRoleAssignments.updater(role: Role): Assignment {
    val updaters = propertyEntries.find { it.roleKey.name == role.value }?.updaterRoles
    if (updaters.isNullOrEmpty()) return Assignment.NONE

    val assignments = updaters.mapNotNull { updater ->
        propertyEntries.find { it.roleKey.name == updater.name }?.parsedAssignment(owner)
    }.toSet()

    return when {
        assignments.isEmpty() -> Assignment.UNKNOWN
        assignments == setOf(Assignment.NONE) -> Assignment.NONE
        assignments.contains(Assignment.ANYONE) -> Assignment.ANYONE
        else -> Assignment.SOMEONE
    }
}

private enum class Role(val value: String) {
    MINTER("minter"),
    BURNER("burner"),
    WITHDRAWER("withdrawer"),
    DEPOSITOR("depositor"),
    RECALLER("recaller"),
    FREEZER("freezer"),
    NON_FUNGIBLE_DATA_UPDATER("non_fungible_data_updater"),
    METADATA_SETTER("metadata_setter")
}

private fun ComponentEntityRoleAssignmentEntry.parsedAssignment(owner: ComponentEntityRoleAssignmentOwner): Assignment {
    val type = when (assignment.resolution) {
        RoleAssignmentResolution.explicit -> assignment.explicitRule?.type
        RoleAssignmentResolution.owner -> owner.rule.type
    }

    return when (type) {
        AccessRule.Type.DenyAll -> Assignment.NONE
        AccessRule.Type.AllowAll -> Assignment.ANYONE
        AccessRule.Type.Protected -> Assignment.SOMEONE
        null -> Assignment.UNKNOWN
    }
}

private enum class Assignment {
    NONE,
    ANYONE,
    SOMEONE,
    UNKNOWN
}

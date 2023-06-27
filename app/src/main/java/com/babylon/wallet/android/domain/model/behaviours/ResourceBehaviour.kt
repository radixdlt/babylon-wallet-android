package com.babylon.wallet.android.domain.model.behaviours

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R

@Composable
fun ResourceBehaviour.name(): String = stringResource(id = title)

@Composable
fun ResourceBehaviour.icon(): Painter = painterResource(id = icon)

enum class ResourceBehaviour(
    @StringRes val title: Int,
    @DrawableRes val icon: Int
) {
    DEFAULT_RESOURCE(
        title = R.string.accountSettings_behaviors_simpleAsset,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_simple_resource_behaviour
    ),

    PERFORM_MINT(
        title = R.string.accountSettings_behaviors_supplyIncreasable,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_supply_increase
    ),
    PERFORM_BURN(
        title = R.string.accountSettings_behaviors_supplyDecreasable,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_supply_decrease
    ),
    PERFORM_MINT_BURN(
        title = R.string.accountSettings_behaviors_supplyFlexible,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_supply_increase_decrease
    ),

    CHANGE_MINT(
        title = R.string.accountSettings_behaviors_supplyIncreasableByAnyone,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_supply_increase_anyone
    ),
    CHANGE_BURN(
        title = R.string.accountSettings_behaviors_supplyDecreasableByAnyone,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_supply_decrease_anyone
    ),
    CHANGE_MINT_BURN(
        title = R.string.accountSettings_behaviors_supplyFlexibleByAnyone,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_supply_increase_decrease_anyone
    ),

    CANNOT_PERFORM_WITHDRAW_DEPOSIT(
        title = R.string.accountSettings_behaviors_movementRestricted,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_movement_restricted
    ),
    CHANGE_WITHDRAW_DEPOSIT(
        title = R.string.accountSettings_behaviors_movementRestrictableInFutureByAnyone,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_movement_restricted_future_anyone
    ),
    FUTURE_MOVEMENT_WITHDRAW_DEPOSIT(
        title = R.string.accountSettings_behaviors_movementRestrictableInFuture,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_movement_restricted_future
    ),

    PERFORM_UPDATE_METADATA(
        title = R.string.accountSettings_behaviors_informationChangeable,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_perform_update_metadata
    ),
    CHANGE_UPDATE_METADATA(
        title = R.string.accountSettings_behaviors_informationChangeableByAnyone,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_change_update_metadata
    ),

    PERFORM_RECALL(
        title = R.string.accountSettings_behaviors_removableByThirdParty,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_perform_recall
    ),
    CHANGE_RECALL(
        title = R.string.accountSettings_behaviors_removableByAnyone,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_change_recall
    ),

    PERFORM_UPDATE_NON_FUNGIBLE_DATA(
        title = R.string.accountSettings_behaviors_nftDataChangeable,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_perform_update_non_fungible_data
    ),
    CHANGE_UPDATE_NON_FUNGIBLE_DATA(
        title = R.string.accountSettings_behaviors_nftDataChangeableByAnyone,
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_change_update_non_fungible_data
    )
}

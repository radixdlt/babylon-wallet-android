package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.assets.AssetBehaviour
import com.babylon.wallet.android.domain.model.resources.Tag

typealias DSR = com.babylon.wallet.android.designsystem.R.drawable

@Composable
fun Tag.name(): String {
    return when (this) {
        is Tag.Official -> stringResource(id = R.string.assetDetails_tags_officialRadix)
        is Tag.Dynamic -> name
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
fun AssetBehaviour.name(isXrd: Boolean = false): String = when (this) {
    AssetBehaviour.SIMPLE_ASSET -> stringResource(id = R.string.assetDetails_behaviors_simpleAsset)
    AssetBehaviour.SUPPLY_INCREASABLE -> stringResource(id = R.string.assetDetails_behaviors_supplyIncreasable)
    AssetBehaviour.SUPPLY_DECREASABLE -> stringResource(id = R.string.assetDetails_behaviors_supplyDecreasable)
    AssetBehaviour.SUPPLY_INCREASABLE_BY_ANYONE -> stringResource(id = R.string.assetDetails_behaviors_supplyIncreasableByAnyone)
    AssetBehaviour.SUPPLY_DECREASABLE_BY_ANYONE -> stringResource(id = R.string.assetDetails_behaviors_supplyDecreasableByAnyone)
    AssetBehaviour.SUPPLY_FLEXIBLE -> if (isXrd) {
        stringResource(id = R.string.assetDetails_behaviors_supplyFlexibleXrd)
    } else {
        stringResource(id = R.string.assetDetails_behaviors_supplyFlexible)
    }
    AssetBehaviour.SUPPLY_FLEXIBLE_BY_ANYONE -> stringResource(id = R.string.assetDetails_behaviors_supplyFlexibleByAnyone)
    AssetBehaviour.MOVEMENT_RESTRICTED -> stringResource(id = R.string.assetDetails_behaviors_movementRestricted)
    AssetBehaviour.MOVEMENT_RESTRICTABLE_IN_FUTURE -> stringResource(id = R.string.assetDetails_behaviors_movementRestrictableInFuture)
    AssetBehaviour.MOVEMENT_RESTRICTABLE_IN_FUTURE_BY_ANYONE -> stringResource(
        id = R.string.assetDetails_behaviors_movementRestrictableInFutureByAnyone
    )
    AssetBehaviour.REMOVABLE_BY_THIRD_PARTY -> stringResource(id = R.string.assetDetails_behaviors_removableByThirdParty)
    AssetBehaviour.REMOVABLE_BY_ANYONE -> stringResource(id = R.string.assetDetails_behaviors_removableByAnyone)
    AssetBehaviour.FREEZABLE_BY_THIRD_PARTY -> stringResource(id = R.string.assetDetails_behaviors_freezableByThirdParty)
    AssetBehaviour.FREEZABLE_BY_ANYONE -> stringResource(id = R.string.assetDetails_behaviors_freezableByAnyone)
    AssetBehaviour.NFT_DATA_CHANGEABLE -> stringResource(id = R.string.assetDetails_behaviors_nftDataChangeable)
    AssetBehaviour.NFT_DATA_CHANGEABLE_BY_ANYONE -> stringResource(id = R.string.assetDetails_behaviors_nftDataChangeableByAnyone)
    AssetBehaviour.INFORMATION_CHANGEABLE -> stringResource(id = R.string.assetDetails_behaviors_informationChangeable)
    AssetBehaviour.INFORMATION_CHANGEABLE_BY_ANYONE -> stringResource(id = R.string.assetDetails_behaviors_informationChangeableByAnyone)
}

@Suppress("CyclomaticComplexMethod")
@Composable
fun AssetBehaviour.icon(): Painter = when (this) {
    AssetBehaviour.SIMPLE_ASSET -> painterResource(id = DSR.ic_simple_resource_behaviour)
    AssetBehaviour.SUPPLY_INCREASABLE -> painterResource(id = DSR.ic_supply_increase)
    AssetBehaviour.SUPPLY_DECREASABLE -> painterResource(id = DSR.ic_supply_decrease)
    AssetBehaviour.SUPPLY_INCREASABLE_BY_ANYONE -> painterResource(id = DSR.ic_supply_increase_anyone)
    AssetBehaviour.SUPPLY_DECREASABLE_BY_ANYONE -> painterResource(id = DSR.ic_supply_decrease_anyone)
    AssetBehaviour.SUPPLY_FLEXIBLE -> painterResource(id = DSR.ic_supply_increase_decrease)
    AssetBehaviour.SUPPLY_FLEXIBLE_BY_ANYONE -> painterResource(id = DSR.ic_supply_increase_decrease_anyone)
    AssetBehaviour.MOVEMENT_RESTRICTED -> painterResource(id = DSR.ic_movement_restricted)
    AssetBehaviour.MOVEMENT_RESTRICTABLE_IN_FUTURE -> painterResource(id = DSR.ic_movement_restricted_future)
    AssetBehaviour.MOVEMENT_RESTRICTABLE_IN_FUTURE_BY_ANYONE -> painterResource(id = DSR.ic_movement_restricted_future_anyone)
    AssetBehaviour.REMOVABLE_BY_THIRD_PARTY -> painterResource(id = DSR.ic_perform_recall)
    AssetBehaviour.REMOVABLE_BY_ANYONE -> painterResource(id = DSR.ic_change_recall)
    AssetBehaviour.FREEZABLE_BY_THIRD_PARTY -> painterResource(id = DSR.ic_freeze_asset_thirdparty)
    AssetBehaviour.FREEZABLE_BY_ANYONE -> painterResource(id = DSR.ic_freeze_asset_anyone)
    AssetBehaviour.NFT_DATA_CHANGEABLE -> painterResource(id = DSR.ic_perform_update_non_fungible_data)
    AssetBehaviour.NFT_DATA_CHANGEABLE_BY_ANYONE -> painterResource(id = DSR.ic_change_update_non_fungible_data)
    AssetBehaviour.INFORMATION_CHANGEABLE -> painterResource(id = DSR.ic_perform_update_metadata)
    AssetBehaviour.INFORMATION_CHANGEABLE_BY_ANYONE -> painterResource(id = DSR.ic_change_update_metadata)
}

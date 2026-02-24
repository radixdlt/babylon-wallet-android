package com.babylon.wallet.android.presentation.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.presentation.dialogs.assets.fungibleAssetDialog
import com.babylon.wallet.android.presentation.dialogs.assets.nonFungibleAssetDialog
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.dAppDetailScreen
import com.babylon.wallet.android.presentation.settings.debug.debugSettings
import com.babylon.wallet.android.presentation.settings.linkedconnectors.linkedConnectorsScreen
import com.babylon.wallet.android.presentation.settings.personas.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.settings.preferences.preferencesNavGraph
import com.babylon.wallet.android.presentation.settings.securitycenter.securityCenterNavGraph
import com.babylon.wallet.android.presentation.settings.troubleshooting.troubleshootingNavGraph

fun NavGraphBuilder.settingsNavGraph(
    navController: NavController,
) {
    linkedConnectorsScreen(
        onInfoClick = { glossaryItem ->
            navController.infoDialog(glossaryItem)
        },
        onBackClick = {
            navController.popBackStack()
        }
    )
    dAppDetailScreen(
        onBackClick = {
            navController.popBackStack()
        },
        onEditPersona = { personaAddress, requiredFields ->
            navController.personaEditScreen(personaAddress, requiredFields)
        },
        onFungibleClick = { resource ->
            navController.fungibleAssetDialog(resourceAddress = resource.address)
        },
        onNonFungibleClick = { resource ->
            navController.nonFungibleAssetDialog(resourceAddress = resource.address)
        }
    )
    preferencesNavGraph(navController)
    securityCenterNavGraph(navController)
    troubleshootingNavGraph(navController)
    if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
        debugSettings(navController)
    }
}

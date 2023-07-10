<<<<<<<< HEAD:app/src/main/java/com/babylon/wallet/android/presentation/account/accountpreference/AccountPreferencesNav.kt
package com.babylon.wallet.android.presentation.account.accountpreference
========
package com.babylon.wallet.android.presentation.settings.account.specificassets
>>>>>>>> 6c886c7c9 (third party deposits UI):app/src/main/java/com/babylon/wallet/android/presentation/settings/account/specificassets/SpecificAssetsDepositsNav.kt

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_ADDRESS = "arg_address"

internal class SpecificAssetsArgs(val address: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_ADDRESS]) as String)
}

fun NavController.specificAssets(address: String) {
    navigate("account_specific_assets_route/$address")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.specificAssets(onBackClick: () -> Unit) {
    composable(
        route = "account_specific_assets_route/{$ARG_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ADDRESS) { type = NavType.StringType }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        SpecificAssetsDepositsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}

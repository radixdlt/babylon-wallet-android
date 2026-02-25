package com.babylon.wallet.android.presentation.settings.troubleshooting.manifest

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavController.sendTransactionManifest() {
    navigate("send_transaction_manifest")
}

fun NavGraphBuilder.sendTransactionManifest(
    onBackClick: () -> Unit
) {
    composable(
        route = "send_transaction_manifest",
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SendTransactionManifestScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}

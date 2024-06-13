package com.babylon.wallet.android.presentation.mobileconnect

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.radixdlt.sargon.RadixConnectMobileLinkRequest
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson

private const val ARG_REQUEST = "request"
private const val ROUTE_ARGS = "$ARG_REQUEST={${ARG_REQUEST}}"
private const val ROUTE = "mobileConnect?$ROUTE_ARGS"

fun NavController.mobileConnect(
    request: RadixConnectMobileLinkRequest
) {
    navigate(route = "mobileConnect/$ARG_REQUEST=${request.toJson()}")
}

internal class MobileConnectArgs(
    val request: RadixConnectMobileLinkRequest
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        request = checkNotNull(savedStateHandle.get<String>(ARG_REQUEST)).let {
            RadixConnectMobileLinkRequest.fromJson(it)
        }
    )
}

fun NavGraphBuilder.mobileConnect(onBackClick: () -> Unit) {
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(ARG_REQUEST) {
                type = NavType.StringType
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        MobileConnectLinkScreen(onBackClick = onBackClick)
    }
}

package com.babylon.wallet.android.presentation.m2m

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ARG_DAPP_ORIGIN = "origin"
private const val ARG_PUBLIC_KEY = "publicKey"
private const val ARG_SESSION_ID = "sessionId"
private const val ARG_INTERACTION_ID = "interactionId"
private const val ROUTE_ARGS =
    "$ARG_PUBLIC_KEY={$ARG_PUBLIC_KEY}&$ARG_SESSION_ID={$ARG_SESSION_ID}&$ARG_DAPP_ORIGIN={$ARG_DAPP_ORIGIN}&$ARG_INTERACTION_ID={$ARG_INTERACTION_ID}"
private const val ROUTE = "m2m?$ROUTE_ARGS"

fun NavController.m2mScreen(publicKeyHex: String = "", sessionId: String = "", origin: String = "", interactionId: String = "") {
    navigate(route = "m2m?$$ARG_PUBLIC_KEY={$publicKeyHex}&$ARG_SESSION_ID={$sessionId}&$ARG_DAPP_ORIGIN={$origin}&$ARG_INTERACTION_ID={$interactionId}")
}

internal class M2MArgs(val publicKey: String?, val sessionId: String?, val origin: String?, val interactionId: String? = null) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String>(ARG_PUBLIC_KEY),
        savedStateHandle.get<String>(ARG_SESSION_ID),
        savedStateHandle.get<String>(ARG_DAPP_ORIGIN),
        savedStateHandle.get<String>(ARG_INTERACTION_ID)
    )
}

fun NavGraphBuilder.m2mScreen(
    onBackClick: () -> Unit
) {
    markAsHighPriority(route = ROUTE)
    composable(
        route = ROUTE,
        deepLinks = listOf(
            navDeepLink {
                uriPattern =
                    "https://d1rxdfxrfmemlj.cloudfront.net/?$ROUTE_ARGS"
            }
        ),
        arguments = listOf(
            navArgument(ARG_PUBLIC_KEY) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_SESSION_ID) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_DAPP_ORIGIN) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_INTERACTION_ID) {
                type = NavType.StringType
                nullable = true
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        M2MScreen(
            onBackClick = onBackClick
        )
    }
}


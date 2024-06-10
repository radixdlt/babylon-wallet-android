package com.babylon.wallet.android.presentation.mobileconnect

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

private const val ARG_DAPP_ORIGIN = "origin"
private const val ARG_PUBLIC_KEY = "publicKey"
private const val ARG_SESSION_ID = "sessionId"
private const val ARG_INTERACTION_ID = "interactionId"
private const val ARG_BROWSER = "browser"

private const val ROUTE_ARGS = "$ARG_PUBLIC_KEY={$ARG_PUBLIC_KEY}" +
    "&$ARG_SESSION_ID={$ARG_SESSION_ID}" +
    "&$ARG_DAPP_ORIGIN={$ARG_DAPP_ORIGIN}" +
    "&$ARG_INTERACTION_ID={$ARG_INTERACTION_ID}" +
    "&$ARG_BROWSER={$ARG_BROWSER}"

private const val ROUTE = "mobileConnect?$ROUTE_ARGS"

fun NavController.mobileConnect(
    publicKeyHex: String = "",
    sessionId: String = "",
    origin: String = "",
    interactionId: String = "",
    browser: String = ""
) {
    navigate(
        route = "mobileConnect?$$ARG_PUBLIC_KEY={$publicKeyHex}" +
            "&$ARG_SESSION_ID={$sessionId}" +
            "&$ARG_DAPP_ORIGIN={$origin}" +
            "&$ARG_INTERACTION_ID={$interactionId}" +
            "&$ARG_BROWSER={$browser}"
    )
}

internal class MobileConnectArgs(
    val publicKey: String?,
    val sessionId: String?,
    val origin: String?,
    val interactionId: String? = null,
    val browser: String? = null
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String>(ARG_PUBLIC_KEY),
        savedStateHandle.get<String>(ARG_SESSION_ID),
        savedStateHandle.get<String>(ARG_DAPP_ORIGIN),
        savedStateHandle.get<String>(ARG_INTERACTION_ID),
        savedStateHandle.get<String>(ARG_BROWSER)
    )

    fun isValidRequest(): Boolean {
        return sessionId != null && interactionId != null
    }

    fun isValidConnect(): Boolean {
        return origin != null && publicKey != null && sessionId != null
    }
}

fun NavGraphBuilder.mobileConnect(onBackClick: () -> Unit) {
    composable(
        route = ROUTE,
        deepLinks = listOf(
            navDeepLink {
                uriPattern =
                    "https://dr6vsuukf8610.cloudfront.net/?$ROUTE_ARGS"
            },
            navDeepLink {
                uriPattern = "radixwallet://?${ROUTE_ARGS}"
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
            },
            navArgument(ARG_BROWSER) {
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
        MobileConnectScreen(onBackClick = onBackClick)
    }
}

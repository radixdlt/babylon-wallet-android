package com.babylon.wallet.android.presentation.mobileconnect

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder

private const val ARG_DAPP_ORIGIN = "origin"
private const val ARG_PUBLIC_KEY = "publicKey"
private const val ARG_SESSION_ID = "sessionId"
private const val ARG_BROWSER = "browser"

private const val ROUTE_ARGS = "{$ARG_PUBLIC_KEY}/{$ARG_SESSION_ID}/{$ARG_DAPP_ORIGIN}/{$ARG_BROWSER}"

private const val ROUTE = "mobileConnect/$ROUTE_ARGS"

fun NavController.mobileConnect(
    publicKeyHex: String = "",
    sessionId: String = "",
    origin: String = "",
    browser: String = ""
) {
    val originEncoded = URLEncoder.encode(origin, "UTF-8")
    navigate(
        route = "mobileConnect/$publicKeyHex/$sessionId/$originEncoded/$browser"
    )
}

internal class MobileConnectArgs(
    val publicKey: String,
    val sessionId: String,
    val origin: String,
    val browser: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<String>(ARG_PUBLIC_KEY)),
        checkNotNull(savedStateHandle.get<String>(ARG_SESSION_ID)),
        checkNotNull((savedStateHandle.get<String>(ARG_DAPP_ORIGIN))).let { URLDecoder.decode(it, "UTF-8") },
        checkNotNull(savedStateHandle.get<String>(ARG_BROWSER)).let { URLDecoder.decode(it, "UTF-8") }
    )
}

fun NavGraphBuilder.mobileConnect(onBackClick: () -> Unit) {
    composable(
        route = ROUTE,
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
        MobileConnectLinkScreen(onBackClick = onBackClick)
    }
}

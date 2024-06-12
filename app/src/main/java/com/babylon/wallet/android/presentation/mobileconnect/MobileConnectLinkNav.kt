package com.babylon.wallet.android.presentation.mobileconnect

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.radixdlt.sargon.RadixConnectMobileLinkRequest
import com.radixdlt.sargon.SessionId
import com.radixdlt.sargon.keyAgreementPublicKeyToHex
import com.radixdlt.sargon.newKeyAgreementPublicKeyFromHex
import rdx.works.core.sargon.toUrl
import java.net.URLDecoder
import java.net.URLEncoder

private const val ARG_DAPP_ORIGIN = "origin"
private const val ARG_PUBLIC_KEY = "publicKey"
private const val ARG_SESSION_ID = "sessionId"
private const val ARG_BROWSER = "browser"

private const val ROUTE_ARGS = "{$ARG_PUBLIC_KEY}/{$ARG_SESSION_ID}/{$ARG_DAPP_ORIGIN}/{$ARG_BROWSER}"

private const val ROUTE = "mobileConnect/$ROUTE_ARGS"

fun NavController.mobileConnect(
    request: RadixConnectMobileLinkRequest
) {
    // TODO Mobile connect (toJson)
    navigate(
        route = "mobileConnect" +
                "/${keyAgreementPublicKeyToHex(publicKey = request.publicKey)}" +
                "/${request.sessionId}" +
                "/${URLEncoder.encode(request.origin.toString(), "UTF-8")}" +
                "/${request.browser}"
    )
}

internal class MobileConnectArgs(
    val request: RadixConnectMobileLinkRequest
) {
    // TODO Mobile connect (fromJson)
    constructor(savedStateHandle: SavedStateHandle) : this(
        request = RadixConnectMobileLinkRequest(
            origin = checkNotNull((savedStateHandle.get<String>(ARG_DAPP_ORIGIN))).let { URLDecoder.decode(it, "UTF-8") }.toUrl(),
            sessionId = SessionId.fromString(checkNotNull(savedStateHandle.get<String>(ARG_SESSION_ID))),
            publicKey = newKeyAgreementPublicKeyFromHex(checkNotNull(savedStateHandle.get<String>(ARG_PUBLIC_KEY))),
            browser = checkNotNull(savedStateHandle.get<String>(ARG_BROWSER))
        )
    )
}

fun NavGraphBuilder.mobileConnect(onBackClick: () -> Unit) {
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(ARG_PUBLIC_KEY) {
                type = NavType.StringType
            },
            navArgument(ARG_SESSION_ID) {
                type = NavType.StringType
            },
            navArgument(ARG_DAPP_ORIGIN) {
                type = NavType.StringType
            },
            navArgument(ARG_BROWSER) {
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

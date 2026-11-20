package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServer

private const val ROUTE_RADIX_CONNECT_SETTINGS = "radix_connect_settings_route"
private const val ROUTE_TRANSPORT_SERVERS = "radix_connect_transport_servers_route"
private const val ROUTE_IROH_RELAY_SERVERS = "iroh_relay_servers_route"
private const val ARG_DIAGNOSED_TRANSPORT = "arg_diagnosed_transport"
private const val DESTINATION_DIAGNOSTICS = "radix_connect_diagnostics_route"
private const val ROUTE_DIAGNOSTICS = "$DESTINATION_DIAGNOSTICS/{$ARG_DIAGNOSED_TRANSPORT}"

internal const val ARG_IROH_RELAY_URL = "arg_iroh_relay_url"
internal const val ARG_IROH_RELAY_IS_PUBLIC = "arg_iroh_relay_is_public"
private const val DESTINATION_IROH_RELAY_SERVER = "iroh_relay_server_details_route"
private const val ROUTE_IROH_RELAY_SERVER =
    "$DESTINATION_IROH_RELAY_SERVER?$ARG_IROH_RELAY_URL={$ARG_IROH_RELAY_URL}&$ARG_IROH_RELAY_IS_PUBLIC={$ARG_IROH_RELAY_IS_PUBLIC}"

/**
 * The relay server a details screen shows: null for a new one.
 */
class IrohRelayServerDetailsArgs(
    val relayServer: IrohRelayServer?
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        relayServer = when {
            savedStateHandle.get<Boolean>(ARG_IROH_RELAY_IS_PUBLIC) == true -> IrohRelayServer.Public
            else -> savedStateHandle.get<String>(ARG_IROH_RELAY_URL)
                ?.takeIf { it.isNotEmpty() }
                ?.let { IrohRelayServer.Custom(it) }
        }
    )
}

fun NavController.radixConnectSettings() {
    navigate(ROUTE_RADIX_CONNECT_SETTINGS) {
        launchSingleTop = true
    }
}

fun NavController.transportServers() {
    navigate(ROUTE_TRANSPORT_SERVERS) {
        launchSingleTop = true
    }
}

fun NavController.irohRelayServers() {
    navigate(ROUTE_IROH_RELAY_SERVERS) {
        launchSingleTop = true
    }
}

fun NavController.radixConnectDiagnostics(transport: DiagnosedTransport) {
    navigate("$DESTINATION_DIAGNOSTICS/${transport.name}")
}

fun NavController.irohRelayServerDetails(relayServer: IrohRelayServer?) {
    val url = (relayServer as? IrohRelayServer.Custom)?.url?.let { Uri.encode(it) }.orEmpty()
    val isPublic = relayServer == IrohRelayServer.Public
    navigate("$DESTINATION_IROH_RELAY_SERVER?$ARG_IROH_RELAY_URL=$url&$ARG_IROH_RELAY_IS_PUBLIC=$isPublic")
}

fun NavGraphBuilder.radixConnectSettings(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_RADIX_CONNECT_SETTINGS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        RadixConnectSettingsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}

/** Signaling Servers: the transports' servers, iroh then WebRTC, as the connection method uses them. */
fun NavGraphBuilder.transportServers(
    onBackClick: () -> Unit,
    onIrohClick: () -> Unit,
    onWebRtcClick: () -> Unit
) {
    composable(
        route = ROUTE_TRANSPORT_SERVERS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        TransportServersScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onIrohClick = onIrohClick,
            onWebRtcClick = onWebRtcClick
        )
    }
}

fun NavGraphBuilder.irohRelayServers(
    onBackClick: () -> Unit,
    onRelayServerClick: (IrohRelayServer) -> Unit,
    onAddRelayServerClick: () -> Unit,
    onDiagnosticsClick: () -> Unit
) {
    composable(
        route = ROUTE_IROH_RELAY_SERVERS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        IrohRelayServersScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onRelayServerClick = onRelayServerClick,
            onAddRelayServerClick = onAddRelayServerClick,
            onDiagnosticsClick = onDiagnosticsClick
        )
    }
}

fun NavGraphBuilder.radixConnectDiagnostics(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_DIAGNOSTICS,
        arguments = listOf(navArgument(ARG_DIAGNOSED_TRANSPORT) { type = NavType.StringType }),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) { entry ->
        val transport = entry.arguments?.getString(ARG_DIAGNOSED_TRANSPORT)
            ?.let { name -> DiagnosedTransport.entries.firstOrNull { it.name == name } }
            ?: DiagnosedTransport.IROH
        RadixConnectDiagnosticsScreen(
            viewModel = hiltViewModel(),
            transport = transport,
            onBackClick = onBackClick
        )
    }
}

fun NavGraphBuilder.irohRelayServerDetails(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_IROH_RELAY_SERVER,
        arguments = listOf(
            navArgument(ARG_IROH_RELAY_URL) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ARG_IROH_RELAY_IS_PUBLIC) {
                type = NavType.BoolType
                defaultValue = false
            }
        ),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        IrohRelayServerDetailsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}

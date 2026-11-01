package com.babylon.wallet.android.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

@Suppress("SwallowedException")
fun NavController.routeExist(route: String): Boolean {
    return try {
        getBackStackEntry(route)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * The back stack entry of the graph [graphRoute] that [entry]'s screen belongs to, for the screen to share the graph's
 * view model, or `null` when the screen must draw nothing.
 *
 * A screen of a flow that is closing (a dApp request rejected, say) is still composed during its exit transition, and
 * even for the first time when it was opened a moment before the flow closed. Then:
 * - its graph may be gone already, or destroyed: asking it for its view model would make a new one, without the graph's
 *   arguments, and the app crashes;
 * - the topmost graph with that route may be the next request's: the screen would act on that request.
 *
 * So the graph is looked up only while the screen is entering (then its graph is the topmost: nothing was opened after
 * it), and kept; a screen that is already leaving when first composed draws nothing, and so does one whose graph was
 * popped.
 */
@Composable
fun NavController.graphEntryWhileAlive(entry: NavBackStackEntry, graphRoute: String): NavBackStackEntry? {
    // Observed only to recompose when they change; decisions read the state now, as navigation may be ahead of them.
    entry.lifecycle.currentStateAsState()
    val found = remember(entry) { arrayOfNulls<NavBackStackEntry>(1) }
    if (found[0] == null && entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        found[0] = runCatching { getBackStackEntry(graphRoute) }.getOrNull()
    }
    val graphEntry = found[0] ?: return null
    graphEntry.lifecycle.currentStateAsState()
    // A graph that was popped is CREATED until its screens' transitions end, and its view models are cleared already.
    return graphEntry.takeIf { it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
}

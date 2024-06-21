package com.babylon.wallet.android.presentation.navigation

import androidx.navigation.NavBackStackEntry

object PriorityRoutes {

    private val highPriorityRoutes: MutableSet<String> = mutableSetOf()

    fun add(route: String) = highPriorityRoutes.add(route)

    fun isHighPriority(entry: NavBackStackEntry): Boolean = highPriorityRoutes.any {
        it.startsWith(entry.destination.route.orEmpty())
    }
}

fun markAsHighPriority(route: String) {
    PriorityRoutes.add(route = route)
}

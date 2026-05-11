package com.babylon.wallet.android.presentation.settings.preferences.addressbook

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "address_book_route"

fun NavController.addressBook() {
    navigate(ROUTE)
}

fun NavGraphBuilder.addressBook(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        AddressBookScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}

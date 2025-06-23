package com.babylon.wallet.android.presentation.discover.blogposts

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_BLOG_POSTS = "route_blog_posts"

fun NavController.blogPostsScreen() {
    navigate(route = ROUTE_BLOG_POSTS)
}

fun NavGraphBuilder.blogPostsScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_BLOG_POSTS,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        BlogPostsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}

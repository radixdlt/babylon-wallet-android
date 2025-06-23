package com.babylon.wallet.android.presentation.alerts

import androidx.compose.runtime.Composable

data class AlertUI(
    val title: String,
    val message: String
) {

    companion object {

        @Composable
        fun from(state: AlertHandler.State): AlertUI? = when (state) {
            is AlertHandler.State.NewBlogPost -> AlertUI(
                title = "New Blog Post",
                message = state.post.name
            )

            AlertHandler.State.Idle -> null
        }
    }
}

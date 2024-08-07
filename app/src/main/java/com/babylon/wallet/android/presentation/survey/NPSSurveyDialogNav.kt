package com.babylon.wallet.android.presentation.survey

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ROUTE = "nps_survey_dialog"

fun NavController.npsSurveyDialog() {
    navigate(
        route = ROUTE
    )
}

fun NavGraphBuilder.npsSurveyDialog(
    onDismiss: () -> Unit
) {
    markAsHighPriority(route = ROUTE)
    dialog(
        route = ROUTE,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        NPSSurveyDialog(
            onDismiss = onDismiss
        )
    }
}

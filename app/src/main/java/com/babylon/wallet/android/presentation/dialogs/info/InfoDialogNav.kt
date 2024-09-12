package com.babylon.wallet.android.presentation.dialogs.info

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

private const val ROUTE = "info_dialog_route"
internal const val ARG_GLOSSARY_ITEM = "arg_glossary_item"

fun NavController.infoDialog(glossaryItem: GlossaryItem) {
    navigate(route = "$ROUTE/$glossaryItem")
}

fun NavGraphBuilder.infoDialog(
    onDismiss: () -> Unit
) {
    dialog(
        route = "$ROUTE/{$ARG_GLOSSARY_ITEM}",
        arguments = listOf(
            navArgument(ARG_GLOSSARY_ITEM) {
                type = NavType.EnumType(GlossaryItem::class.java)
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        InfoDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}

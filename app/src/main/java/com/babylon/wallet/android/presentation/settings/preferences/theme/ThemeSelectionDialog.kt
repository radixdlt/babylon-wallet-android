@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.settings.preferences.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.applyThemeSelection
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.none
import kotlinx.coroutines.launch
import rdx.works.core.domain.ThemeSelection

@Composable
fun ThemeSelectionDialog(
    modifier: Modifier = Modifier,
    viewModel: ThemeSelectionViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val handleOnDismiss: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }
    LaunchedEffect(Unit) {
        scope.launch {
            sheetState.show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                ThemeSelectionViewModel.Event.Dismiss -> handleOnDismiss()
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    ThemeSelectionContent(
        modifier = modifier,
        sheetState = sheetState,
        state = state,
        onSelected = viewModel::onThemeSelected,
        onDismiss = handleOnDismiss
    )
}

@Composable
private fun ThemeSelectionContent(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: ThemeSelectionViewModel.State,
    onSelected: (ThemeSelection) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    DefaultModalSheetLayout(
        modifier = modifier.fillMaxSize(),
        onDismissRequest = onDismiss,
        wrapContent = true,
        sheetState = sheetState,
        containerColor = RadixTheme.colors.background,
        sheetContent = {
            Column {
                RadixCenteredTopAppBar(
                    windowInsets = WindowInsets.none,
                    title = "",
                    onBackClick = onDismiss,
                    backIconType = BackIconType.Close
                )

                ThemeSelectionItem(
                    modifier = Modifier
                        .fillMaxWidth(),
                    selection = ThemeSelection.LIGHT,
                    isSelected = state.selection == ThemeSelection.LIGHT,
                    onSelected = {
                        onSelected(ThemeSelection.LIGHT)
                        context.applyThemeSelection(ThemeSelection.LIGHT)
                    }
                )

                ThemeSelectionItem(
                    modifier = Modifier
                        .fillMaxWidth(),
                    selection = ThemeSelection.DARK,
                    isSelected = state.selection == ThemeSelection.DARK,
                    onSelected = {
                        onSelected(ThemeSelection.DARK)
                        context.applyThemeSelection(ThemeSelection.DARK)
                    }
                )

                ThemeSelectionItem(
                    modifier = Modifier
                        .fillMaxWidth(),
                    selection = ThemeSelection.SYSTEM,
                    isSelected = state.selection == ThemeSelection.SYSTEM,
                    onSelected = {
                        onSelected(ThemeSelection.SYSTEM)
                        context.applyThemeSelection(ThemeSelection.SYSTEM)
                    }
                )
            }
        }
    )
}

@Composable
private fun ThemeSelectionItem(
    modifier: Modifier = Modifier,
    selection: ThemeSelection,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    DefaultSettingsItem(
        modifier = modifier,
        title = when (selection) {
            ThemeSelection.LIGHT -> stringResource(R.string.preferences_themeSelection_light)
            ThemeSelection.DARK -> stringResource(R.string.preferences_themeSelection_dark)
            ThemeSelection.SYSTEM -> stringResource(R.string.preferences_themeSelection_system)
        },
        onClick = onSelected,
        leadingIconRes = when (selection) {
            ThemeSelection.LIGHT -> com.babylon.wallet.android.designsystem.R.drawable.ic_light
            ThemeSelection.DARK -> com.babylon.wallet.android.designsystem.R.drawable.ic_dark
            ThemeSelection.SYSTEM -> com.babylon.wallet.android.designsystem.R.drawable.ic_system
        },
        trailingIcon = {
            Box(
                modifier = Modifier.size(24.dp)
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_check),
                        contentDescription = null,
                        tint = RadixTheme.colors.icon
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun ThemeSelectionPreview() {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            sheetState.show()
        }
    }

    var selection: ThemeSelection by remember {
        mutableStateOf(ThemeSelection.LIGHT)
    }

    RadixWalletPreviewTheme {
        ThemeSelectionContent(
            sheetState = sheetState,
            state = ThemeSelectionViewModel.State(
                selection = selection
            ),
            onSelected = {
                selection = it
            },
            onDismiss = {}
        )
    }
}

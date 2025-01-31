@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.accessfactorsources.applyshield

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixRadioButton
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.babylon.wallet.android.presentation.ui.none
import com.radixdlt.sargon.SecurityStructureId
import kotlinx.coroutines.launch

@Composable
fun ApplyShieldDialog(
    modifier: Modifier = Modifier,
    viewModel: ApplyShieldViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                ApplyShieldViewModel.Event.Dismiss -> onDismiss()
            }
        }
    }

    ApplyShieldContent(
        modifier = modifier,
        state = state,
        onSelected = viewModel::onSelected,
        onApply = viewModel::onApply,
        onDismiss = onDismiss
    )
}

@Composable
private fun ApplyShieldContent(
    modifier: Modifier = Modifier,
    state: ApplyShieldViewModel.State,
    onSelected: (SecurityStructureId) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            sheetState.show()
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier.fillMaxSize(),
        onDismissRequest = onDismiss,
        heightFraction = 0.8f,
        sheetState = sheetState,
        sheetContent = {
            Column {
                Scaffold(
                    topBar = {
                        RadixCenteredTopAppBar(
                            windowInsets = WindowInsets.none,
                            title = "Apply Shield",
                            onBackClick = onDismiss,
                            backIconType = BackIconType.Close
                        )
                    },
                    bottomBar = {
                       RadixBottomBar(
                           enabled = state.isApplyEnabled,
                           isLoading = state.isApplying,
                           text = "Apply",
                           onClick = onApply
                       )
                    },
                    containerColor = RadixTheme.colors.defaultBackground,
                    content = { padding ->
                        Column(modifier = Modifier.padding(padding)) {

                            state.shields.forEach { shield ->
                                Column(
                                    modifier = modifier
                                        .padding(
                                            horizontal = RadixTheme.dimensions.paddingDefault,
                                            vertical = RadixTheme.dimensions.paddingSmall
                                        )
                                        .defaultCardShadow(elevation = 6.dp)
                                        .clip(RadixTheme.shapes.roundedRectMedium)
                                        .fillMaxWidth()
                                        .background(
                                            color = RadixTheme.colors.white,
                                            shape = RadixTheme.shapes.roundedRectDefault
                                        )
                                ) {
                                    val isSelected = remember(state, shield) {
                                        state.selected == shield.id
                                    }

                                    Row(
                                        modifier = modifier
                                            .clickable {
                                                if (!isSelected) {
                                                    onSelected(shield.id)
                                                }
                                            }
                                            .padding(RadixTheme.dimensions.paddingDefault),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = shield.displayName.value,
                                            style = RadixTheme.typography.body1Header,
                                            color = RadixTheme.colors.gray1
                                        )

                                        RadixRadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                if (!isSelected) {
                                                    onSelected(shield.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    )
}
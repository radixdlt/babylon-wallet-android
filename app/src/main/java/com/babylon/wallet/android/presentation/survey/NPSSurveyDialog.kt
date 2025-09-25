package com.babylon.wallet.android.presentation.survey

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.none
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NPSSurveyDialog(
    modifier: Modifier = Modifier,
    viewModel: NPSSurveyViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                NPSSurveyEvent.Close -> onDismiss()
            }
        }
    }
    BackHandler {
        viewModel.onBackPress()
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            sheetState.show()
        }
    }
    val onDismissRequest: () -> Unit = {
        scope.launch {
            sheetState.hide()
            viewModel.onBackPress()
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        showDragHandle = true,
        wrapContent = true,
        onDismissRequest = onDismissRequest,
        sheetContent = {
            Column {
                RadixCenteredTopAppBar(
                    windowInsets = WindowInsets.none,
                    title = "",
                    onBackClick = onDismissRequest,
                    backIconType = BackIconType.Close
                )

                NPSSurveySheet(
                    reason = state.reason,
                    onReasonChanged = viewModel::onReasonChanged,
                    onScoreClick = viewModel::onScoreClick,
                    isLoading = state.isLoading,
                    isSubmitButtonEnabled = state.isSubmitButtonEnabled,
                    scores = state.scores,
                    onSubmitClick = viewModel::onSubmitClick
                )
            }
        }
    )
}

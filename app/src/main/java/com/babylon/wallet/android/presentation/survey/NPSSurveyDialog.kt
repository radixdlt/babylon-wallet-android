package com.babylon.wallet.android.presentation.survey

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper

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

    BottomSheetDialogWrapper(
        modifier = modifier,
        title = "",
        onDismiss = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(fraction = 0.9f)
        ) {
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
}

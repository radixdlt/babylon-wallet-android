package com.babylon.wallet.android.presentation.nfc

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.none
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcDialog(
    viewModel: NfcViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        viewModel.onDismiss()
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                NfcViewModel.Event.Completed -> onDismiss()
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch { sheetState.show() }
    }

    DefaultModalSheetLayout(
        modifier = Modifier.fillMaxSize(),
        sheetState = sheetState,
        onDismissRequest = viewModel::onDismiss,
        sheetContent = {
            Scaffold(
                topBar = {
                    RadixCenteredTopAppBar(
                        windowInsets = WindowInsets.none,
                        title = state.title,
                        onBackClick = viewModel::onDismiss,
                        backIconType = BackIconType.Close
                    )
                },
                content = { _ ->
                    // For scaffold phase we intentionally do not render real NFC content
                    // Later we can show message/progress, tag state, etc.
                }
            )
        }
    )
}



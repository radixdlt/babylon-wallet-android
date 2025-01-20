package com.babylon.wallet.android.presentation.ui.composables.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun isKeyboardVisible(): Boolean {
    return WindowInsets.isImeVisible
}

@OptIn(FlowPreview::class)
@Composable
fun HideKeyboardOnFullScroll(scrollState: ScrollState) {
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(scrollState) {
        snapshotFlow { !scrollState.canScrollForward }
            .distinctUntilChanged()
            .debounce(200)
            .collect { isFullyScrolled ->
                if (isFullyScrolled) {
                    keyboardController?.hide()
                }
            }
    }
}

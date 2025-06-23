package com.babylon.wallet.android.presentation.ui.composables.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun clearFocusNestedScrollConnection(): NestedScrollConnection {
    val focusManager = LocalFocusManager.current

    return remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                focusManager.clearFocus()
                return super.onPreScroll(available, source)
            }
        }
    }
}

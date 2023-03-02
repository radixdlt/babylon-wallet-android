package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.ui.Modifier

fun Modifier.applyIf(
    condition: Boolean,
    modifier: Modifier
): Modifier {
    return if (condition) {
        return then(modifier)
    } else {
        modifier
    }
}

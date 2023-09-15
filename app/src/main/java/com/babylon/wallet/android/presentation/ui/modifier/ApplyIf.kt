package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.ui.Modifier

fun Modifier.applyIf(
    condition: Boolean,
    modifier: Modifier
): Modifier {
    return if (condition) {
        return then(modifier)
    } else {
        this
    }
}

fun <T> Modifier.applyWhen(
    condition: T?,
    modifier: (T) -> Modifier
): Modifier {
    return if (condition != null) {
        then(modifier(condition))
    } else {
        this
    }
}

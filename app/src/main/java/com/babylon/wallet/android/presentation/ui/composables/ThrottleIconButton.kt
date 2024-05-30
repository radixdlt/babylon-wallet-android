package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.utils.ClickListenerUtils

@Composable
fun ThrottleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thresholdMs: Long = 500L,
    content: @Composable () -> Unit
) {
    // Dimensions and shapes were ported from IconButton source code
    val lastClickMs = remember { mutableStateOf(0L) }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                onClick = {
                    ClickListenerUtils.throttleOnClick(
                        lastClickMs = lastClickMs,
                        onClick = onClick,
                        throttleMs = thresholdMs
                    )
                },
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = false,
                    radius = 40.dp / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

package com.babylon.wallet.android.presentation.alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.DSR
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import kotlinx.coroutines.delay

private const val AUTO_DISMISS_IN_MS = 5000L
private val animationSpec: FiniteAnimationSpec<IntOffset> by lazy {
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

@Composable
fun AlertView(
    ui: AlertUI,
    modifier: Modifier = Modifier,
    autoDismiss: Boolean = true,
    onDismiss: (() -> Unit)? = null
) {
    val show = remember { mutableStateOf(true) }

    LaunchedEffect(show.value) {
        if (show.value) {
            if (autoDismiss) {
                delay(AUTO_DISMISS_IN_MS)
                show.value = false
            }
        } else {
            onDismiss?.invoke()
        }
    }

    AnimatedVisibility(
        visible = show.value,
        enter = slideInVertically(animationSpec) { -it },
        exit = slideOutVertically(animationSpec) { -it }
    ) {
        AlertContent(
            modifier = modifier,
            ui = ui,
            color = RadixTheme.colors.backgroundTertiary,
            onDismiss = { show.value = false }
        )
    }
}

@Composable
private fun AlertContent(
    ui: AlertUI,
    color: Color,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBarsAndBanner)
            .fillMaxWidth()
            .heightIn(max = 148.dp)
            .padding(
                top = RadixTheme.dimensions.paddingSemiLarge,
                start = RadixTheme.dimensions.paddingLarge,
                end = RadixTheme.dimensions.paddingLarge
            )
            .background(
                color = color,
                shape = RadixTheme.shapes.roundedRectDefault
            )
    ) {
        Row(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                painter = painterResource(DSR.ic_notifications),
                contentDescription = null,
                tint = RadixTheme.colors.icon
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

            Column {
                if (ui.title.isNotBlank()) {
                    Text(
                        text = ui.title,
                        color = RadixTheme.colors.text,
                        style = RadixTheme.typography.body1HighImportance,
                        maxLines = 2
                    )
                }

                if (ui.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))

                    Text(
                        text = ui.message,
                        color = RadixTheme.colors.text,
                        style = RadixTheme.typography.body2Regular,
                        maxLines = 3
                    )
                }
            }
        }

        IconButton(
            modifier = Modifier.align(Alignment.TopEnd),
            onClick = { onDismiss() }
        ) {
            Icon(
                painter = painterResource(DSR.ic_close),
                contentDescription = null,
                tint = RadixTheme.colors.icon
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun ErrorAlertPreview() {
    RadixWalletPreviewTheme {
        AlertView(
            ui = AlertUI(
                title = "Alert Title",
                message = "This is a test message"
            )
        )
    }
}

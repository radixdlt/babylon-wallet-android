package com.babylon.wallet.android.designsystem.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.Gray1
import com.babylon.wallet.android.designsystem.theme.Gray3
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixThemeConfig
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.designsystem.theme.indexToGradient

private val RADIO_BUTTON_SIZE = 20.dp
private val RADIO_BUTTON_PADDING = 2.dp
private val RADIO_BUTTON_DOT_SIZE = 8.dp
private val RADIO_STROKE_WIDTH = 1.dp

private const val RADIO_ANIMATION_DURATION = 100

@Composable
fun RadixRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadixRadioButtonColors = RadixRadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    size: Dp = RADIO_BUTTON_SIZE
) {
    val dotRadius = animateDpAsState(
        targetValue = RADIO_BUTTON_DOT_SIZE / 2,
        animationSpec = tween(durationMillis = RADIO_ANIMATION_DURATION)
    )
    val dotColor = colors.dotColors.radioColor(enabled, selected)
    val backgroundColor = colors.backgroundColors.radioColor(enabled, selected)
    val borderColor = colors.borderColors.radioColor(enabled, selected)
    val selectableModifier =
        if (onClick != null) {
            @Suppress("DEPRECATION_ERROR")
            Modifier.selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = size
                )
            )
        } else {
            Modifier
        }
    Canvas(
        modifier
            .then(
                if (onClick != null) {
                    Modifier.size(32.dp)
                } else {
                    Modifier
                }
            )
            .then(selectableModifier)
            .wrapContentSize(Alignment.Center)
            .padding(RADIO_BUTTON_PADDING)
            .requiredSize(size)
    ) {
        // Draw the border
        val strokeWidth = RADIO_STROKE_WIDTH.toPx()
        val outerCircleSize = (size / 2).toPx() - strokeWidth / 2
        drawCircle(
            color = borderColor.value,
            radius = outerCircleSize,
            style = Stroke(strokeWidth)
        )
        // Draw the background
        drawCircle(
            color = backgroundColor.value,
            radius = outerCircleSize,
            style = Fill
        )
        // Draw the dot
        drawCircle(
            color = dotColor.value,
            radius = dotRadius.value.toPx(),
            style = Fill
        )
    }
}

object RadixRadioButtonDefaults {

    @Composable
    fun onLightBackgroundColors(
        dotColors: RadixRadioButtonColors.ComponentColors = RadixRadioButtonColors.ComponentColors(
            selectedColor = Gray1,
            unselectedColor = Color.Transparent,
            disabledSelectedColor = Gray3,
            disabledUnselectedColor = Color.Transparent
        ),
        backgroundColors: RadixRadioButtonColors.ComponentColors = RadixRadioButtonColors.ComponentColors(
            selectedColor = White,
            unselectedColor = White.copy(alpha = 0.5f),
            disabledSelectedColor = White.copy(alpha = 0.5f),
            disabledUnselectedColor = White.copy(alpha = 0.5f)
        ),
        borderColors: RadixRadioButtonColors.ComponentColors = RadixRadioButtonColors.ComponentColors(
            selectedColor = White,
            unselectedColor = White,
            disabledSelectedColor = Gray3,
            disabledUnselectedColor = Gray3
        )
    ): RadixRadioButtonColors = RadixRadioButtonColors(
        dotColors = dotColors,
        backgroundColors = backgroundColors,
        borderColors = borderColors
    )

    @Composable
    fun colors(
        dotColors: RadixRadioButtonColors.ComponentColors = RadixRadioButtonColors.ComponentColors(
            selectedColor = RadixTheme.colors.background,
            unselectedColor = Color.Transparent,
            disabledSelectedColor = RadixTheme.colors.iconTertiary,
            disabledUnselectedColor = Color.Transparent
        ),
        backgroundColors: RadixRadioButtonColors.ComponentColors = RadixRadioButtonColors.ComponentColors(
            selectedColor = RadixTheme.colors.icon,
            unselectedColor = Color.Transparent,
            disabledSelectedColor = Color.Transparent,
            disabledUnselectedColor = Color.Transparent
        ),
        borderColors: RadixRadioButtonColors.ComponentColors = RadixRadioButtonColors.ComponentColors(
            selectedColor = RadixTheme.colors.icon,
            unselectedColor = RadixTheme.colors.iconSecondary,
            disabledSelectedColor = RadixTheme.colors.iconTertiary,
            disabledUnselectedColor = RadixTheme.colors.iconTertiary
        )
    ): RadixRadioButtonColors = RadixRadioButtonColors(
        dotColors = dotColors,
        backgroundColors = backgroundColors,
        borderColors = borderColors
    )
}

data class RadixRadioButtonColors(
    val dotColors: ComponentColors,
    val backgroundColors: ComponentColors,
    val borderColors: ComponentColors
) {

    @Immutable
    data class ComponentColors(
        val selectedColor: Color,
        val unselectedColor: Color,
        val disabledSelectedColor: Color,
        val disabledUnselectedColor: Color
    ) {

        @Composable
        fun radioColor(enabled: Boolean, selected: Boolean): State<Color> {
            val target = when {
                enabled && selected -> selectedColor
                enabled && !selected -> unselectedColor
                !enabled && selected -> disabledSelectedColor
                else -> disabledUnselectedColor
            }

            // If not enabled 'snap' to the disabled state, as there should be no animations between
            // enabled / disabled.
            return if (enabled) {
                animateColorAsState(target, tween(durationMillis = RADIO_ANIMATION_DURATION))
            } else {
                rememberUpdatedState(target)
            }
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonSelectedOnLightBackgroundColorsPreview() {
    RadixWalletTheme {
        Box(
            modifier = Modifier
                .background(0.indexToGradient())
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            RadixRadioButton(
                selected = true,
                colors = RadixRadioButtonDefaults.onLightBackgroundColors(),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonNotSelectedOnLightBackgroundColorsPreview() {
    RadixWalletTheme {
        Box(
            modifier = Modifier
                .background(0.indexToGradient())
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            RadixRadioButton(
                selected = false,
                colors = RadixRadioButtonDefaults.onLightBackgroundColors(),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonSelectedColorsPreviewLight() {
    RadixWalletTheme {
        Box(
            modifier =
            Modifier.background(RadixTheme.colors.background)
        ) {
            RadixRadioButton(
                selected = true,
                colors = RadixRadioButtonDefaults.colors(),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonNotSelectedColorsPreviewLight() {
    RadixWalletTheme {
        Box(
            modifier =
            Modifier.background(RadixTheme.colors.background)
        ) {
            RadixRadioButton(
                selected = false,
                colors = RadixRadioButtonDefaults.colors(),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonSelectedDisabledColorsPreviewLight() {
    RadixWalletTheme {
        Box(
            modifier =
            Modifier.background(RadixTheme.colors.background)
        ) {
            RadixRadioButton(
                selected = true,
                enabled = false,
                colors = RadixRadioButtonDefaults.colors(),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonNotSelectedDisabledColorsPreviewLight() {
    RadixWalletTheme {
        Box(
            modifier =
            Modifier.background(RadixTheme.colors.background)
        ) {
            RadixRadioButton(
                selected = false,
                enabled = false,
                colors = RadixRadioButtonDefaults.colors(),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonSelectedColorsPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(
            modifier =
            Modifier.background(RadixTheme.colors.background)
        ) {
            RadixRadioButton(
                selected = true,
                colors = RadixRadioButtonDefaults.colors(),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonNotSelectedColorsPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(
            modifier =
            Modifier.background(RadixTheme.colors.background)
        ) {
            RadixRadioButton(
                selected = false,
                colors = RadixRadioButtonDefaults.colors(),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonSelectedDisabledColorsPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(
            modifier =
            Modifier.background(RadixTheme.colors.background)
        ) {
            RadixRadioButton(
                selected = true,
                enabled = false,
                colors = RadixRadioButtonDefaults.colors(),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun RadixRadioButtonNotSelectedDisabledColorsPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(
            modifier =
            Modifier.background(RadixTheme.colors.background)
        ) {
            RadixRadioButton(
                selected = false,
                enabled = false,
                colors = RadixRadioButtonDefaults.colors(),
                onClick = {}
            )
        }
    }
}

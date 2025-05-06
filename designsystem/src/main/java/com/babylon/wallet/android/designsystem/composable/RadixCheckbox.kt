package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.Gray1
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixThemeConfig
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.designsystem.theme.indexToGradient

object RadixCheckboxDefaults {
    @Composable
    fun onLightBackgroundColors() = CheckboxDefaults.colors().copy(
        checkedCheckmarkColor = Gray1,
        uncheckedCheckmarkColor = Color.Transparent,
        checkedBoxColor = White,
        uncheckedBoxColor = White.copy(alpha = 0.4f),
        disabledCheckedBoxColor = White.copy(alpha = 0.4f),
        disabledUncheckedBoxColor = White.copy(alpha = 0.2f),
        disabledIndeterminateBoxColor = White.copy(alpha = 0.2f),
        checkedBorderColor = White,
        uncheckedBorderColor = White,
        disabledBorderColor = White.copy(alpha = 0.4f),
        disabledUncheckedBorderColor = White.copy(alpha = 0.4f),
        disabledIndeterminateBorderColor = White.copy(alpha = 0.4f),
    )

    @Composable
    fun colors() = CheckboxDefaults.colors().copy(
        checkedCheckmarkColor = RadixTheme.colors.background,
        uncheckedCheckmarkColor = Color.Transparent,
        checkedBoxColor = RadixTheme.colors.icon,
        uncheckedBoxColor = RadixTheme.colors.backgroundSecondary,
        disabledCheckedBoxColor = RadixTheme.colors.icon.copy(alpha = 0.4f),
        disabledUncheckedBoxColor = RadixTheme.colors.backgroundSecondary.copy(alpha = 0.4f),
        disabledIndeterminateBoxColor = RadixTheme.colors.backgroundSecondary.copy(alpha = 0.4f),
        checkedBorderColor = RadixTheme.colors.icon,
        uncheckedBorderColor = RadixTheme.colors.icon,
        disabledBorderColor = RadixTheme.colors.icon.copy(alpha = 0.4f),
        disabledUncheckedBorderColor = RadixTheme.colors.icon.copy(alpha = 0.4f),
        disabledIndeterminateBorderColor = RadixTheme.colors.icon.copy(alpha = 0.4f),
    )
}

@Composable
@Preview
fun CheckBoxColorsPreviewLight() {
    RadixWalletTheme(
        config = RadixThemeConfig(
            isSystemDarkTheme = false
        )
    ) {
        Column(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Unchecked",
                    color = RadixTheme.colors.text
                )

                Checkbox(
                    checked = false,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.colors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Checked",
                    color = RadixTheme.colors.text
                )

                Checkbox(
                    checked = true,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.colors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Unchecked disabled", color = RadixTheme.colors.text)

                Checkbox(
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.colors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Checked disabled", color = RadixTheme.colors.text)

                Checkbox(
                    checked = true,
                    enabled = false,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.colors()
                )
            }
        }

    }
}

@Composable
@Preview
fun CheckBoxColorsPreviewDark() {
    RadixWalletTheme(
        config = RadixThemeConfig(
            isSystemDarkTheme = true
        )
    ) {
        Column(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Unchecked", color = RadixTheme.colors.text)

                Checkbox(
                    checked = false,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.colors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Checked", color = RadixTheme.colors.text)

                Checkbox(
                    checked = true,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.colors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Unchecked disabled", color = RadixTheme.colors.text)

                Checkbox(
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.colors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Checked disabled", color = RadixTheme.colors.text)

                Checkbox(
                    checked = true,
                    enabled = false,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.colors()
                )
            }
        }

    }
}

@Composable
@Preview
fun CheckBoxColorsOnAccountPreview() {
    RadixWalletTheme(
        config = RadixThemeConfig(
            isSystemDarkTheme = false
        )
    ) {
        Column(
            modifier = Modifier
                .background(0.indexToGradient())
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Unchecked", color = White)

                Checkbox(
                    checked = false,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.onLightBackgroundColors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Checked", color = White)

                Checkbox(
                    checked = true,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.onLightBackgroundColors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Unchecked disabled", color = White)

                Checkbox(
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.onLightBackgroundColors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Checked disabled", color = White)

                Checkbox(
                    checked = true,
                    enabled = false,
                    onCheckedChange = {},
                    colors = RadixCheckboxDefaults.onLightBackgroundColors()
                )
            }
        }

    }
}


package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun RadixBottomBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    throttleClicks: Boolean = true,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    text: String,
    color: Color = RadixTheme.colors.background,
    buttonPadding: PaddingValues = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
    insets: WindowInsets = WindowInsets.navigationBars,
    additionalTopContent: (@Composable ColumnScope.() -> Unit)? = null,
    additionalBottomContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    RadixBottomBar(
        modifier = modifier,
        color = color,
        insets = insets,
        additionalTopContent = additionalTopContent,
        additionalBottomContent = additionalBottomContent,
        button = {
            RadixPrimaryButton(
                text = text,
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(buttonPadding),
                enabled = enabled,
                isLoading = isLoading,
                throttleClicks = throttleClicks
            )
        }
    )
}

@Composable
fun RadixBottomBar(
    button: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    color: Color = RadixTheme.colors.background,
    dividerColor: Color = RadixTheme.colors.divider,
    insets: WindowInsets = WindowInsets.navigationBars,
    additionalTopContent: (@Composable ColumnScope.() -> Unit)? = null,
    additionalBottomContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .background(color)
            .padding(insets.asPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = dividerColor)

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        additionalTopContent?.invoke(this)

        button()

        additionalBottomContent?.invoke(this)

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}

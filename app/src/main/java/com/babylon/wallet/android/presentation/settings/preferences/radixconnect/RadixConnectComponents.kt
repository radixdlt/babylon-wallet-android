package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

/** The top bar of the Radix Connect screens: the title, back, and the divider under it. */
@Composable
internal fun RadixConnectTopBar(title: String, onBackClick: () -> Unit) {
    Column {
        RadixCenteredTopAppBar(
            title = title,
            onBackClick = onBackClick,
            windowInsets = WindowInsets.statusBarsAndBanner
        )
        HorizontalDivider(color = RadixTheme.colors.divider)
    }
}

/** A section's title, as the other settings screens draw it. */
@Composable
internal fun SectionTitle(text: String) {
    Text(
        modifier = Modifier.padding(
            start = RadixTheme.dimensions.paddingDefault,
            end = RadixTheme.dimensions.paddingDefault,
            top = RadixTheme.dimensions.paddingLarge,
            bottom = RadixTheme.dimensions.paddingSmall
        ),
        text = text,
        style = RadixTheme.typography.body1Link,
        color = RadixTheme.colors.textSecondary
    )
}

/** The label above a list of servers: «Current», «Others». */
@Composable
internal fun ListLabel(text: String) {
    Text(
        modifier = Modifier.padding(
            horizontal = RadixTheme.dimensions.paddingDefault,
            vertical = RadixTheme.dimensions.paddingSmall
        ),
        text = text,
        style = RadixTheme.typography.body1HighImportance,
        color = RadixTheme.colors.text
    )
}

/** The text that opens a screen: what it is for. */
@Composable
internal fun ScreenLead(text: String) {
    Text(
        modifier = Modifier.padding(
            horizontal = RadixTheme.dimensions.paddingDefault,
            vertical = RadixTheme.dimensions.paddingMedium
        ),
        text = text,
        style = RadixTheme.typography.body1HighImportance,
        color = RadixTheme.colors.textSecondary
    )
}

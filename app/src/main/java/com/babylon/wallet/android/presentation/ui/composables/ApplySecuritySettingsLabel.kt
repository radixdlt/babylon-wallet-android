package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Badge
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.Red1
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable

@Composable
fun ApplySecuritySettingsLabel(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    text: String,
    labelColor: Color = RadixTheme.colors.white.copy(alpha = 0.3f),
    contentColor: Color = RadixTheme.colors.white
) {
    Row(
        modifier = modifier
            .background(labelColor, RadixTheme.shapes.roundedRectSmall)
            .clip(RadixTheme.shapes.roundedRectSmall)
            .applyIf(
                onClick != null,
                modifier = Modifier.throttleClickable {
                    onClick?.invoke()
                }
            )
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(id = R.drawable.ic_security),
            contentDescription = null,
            tint = contentColor
        )
        Text(
            text = text,
            style = RadixTheme.typography.body2HighImportance,
            modifier = Modifier.weight(1f),
            color = contentColor
        )
        Badge(backgroundColor = Red1)
    }
}

@Composable
fun SecurityPromptType.toText() = when (this) {
    SecurityPromptType.NEEDS_BACKUP -> stringResource(id = com.babylon.wallet.android.R.string.securityProblems_no3_accountCard)
    SecurityPromptType.NEEDS_RECOVER -> stringResource(id = com.babylon.wallet.android.R.string.securityProblems_no9_accountCard)
}

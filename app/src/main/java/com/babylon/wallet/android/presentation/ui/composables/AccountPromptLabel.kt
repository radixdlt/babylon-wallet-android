package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.domain.usecases.securityproblems.SecurityPromptType
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable

@Composable
fun AccountPromptLabel(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    text: String,
    labelColor: Color = White,
    contentColor: Color = White,
    @DrawableRes iconRes: Int = R.drawable.ic_warning_error,
) {
    PromptLabel(
        modifier = modifier
            .background(RadixTheme.colors.backgroundTransparent, RadixTheme.shapes.roundedRectSmall)
            .clip(RadixTheme.shapes.roundedRectSmall)
            .applyIf(
                onClick != null,
                modifier = Modifier.throttleClickable {
                    onClick?.invoke()
                }
            )
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingSmall),
        text = text,
        textColor = labelColor,
        iconRes = iconRes,
        iconTint = contentColor,
        iconSize = 14.dp
    )
}

@Composable
fun SecurityPromptType.toText() = when (this) {
    SecurityPromptType.WRITE_DOWN_SEED_PHRASE -> stringResource(id = com.babylon.wallet.android.R.string.securityProblems_no3_accountCard)
    SecurityPromptType.RECOVERY_REQUIRED -> stringResource(id = com.babylon.wallet.android.R.string.securityProblems_no9_accountCard)
    SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM -> stringResource(
        id = com.babylon.wallet.android.R.string.securityProblems_no5_accountCard
    )
    SecurityPromptType.WALLET_NOT_RECOVERABLE -> stringResource(id = com.babylon.wallet.android.R.string.securityProblems_no6_accountCard)
    SecurityPromptType.CONFIGURATION_BACKUP_NOT_UPDATED -> stringResource(
        id = com.babylon.wallet.android.R.string.securityProblems_no7_accountCard
    )
}

@Composable
@Preview(showBackground = false)
private fun AccountPromptLabelPreview() {
    RadixWalletPreviewTheme {
        AccountPromptLabel(
            modifier = Modifier.fillMaxWidth().padding(bottom = RadixTheme.dimensions.paddingMedium),
            onClick = {},
            text = "Recovery required"
        )
    }
}

package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.modifier.enabledOpacity
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
fun DefaultSettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    isErrorText: Boolean = false,
    isEnabled: Boolean = true,
    subtitleView: @Composable (ColumnScope.() -> Unit)? = null,
    infoView: @Composable (ColumnScope.() -> Unit)? = null,
    leadingIcon: @Composable (BoxScope.() -> Unit)? = null,
    warningView: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = {
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.icon
        )
    }
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.background)
            .throttleClickable(onClick = onClick, enabled = isEnabled)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingLarge),
    ) {
        val (leadingIconRef, contentRef, trailingIconRef, warningRef) = createRefs()
        val paddingMedium = RadixTheme.dimensions.paddingMedium
        leadingIcon?.let { icon ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .enabledOpacity(isEnabled)
                    .constrainAs(leadingIconRef) {
                        start.linkTo(parent.start)
                        end.linkTo(contentRef.start, paddingMedium)
                        top.linkTo(contentRef.top)
                        bottom.linkTo(contentRef.bottom)
                    },
                contentAlignment = Center
            ) {
                icon()
            }
        }
        Column(
            modifier = Modifier
                .enabledOpacity(isEnabled)
                .constrainAs(contentRef) {
                    start.linkTo(if (leadingIcon == null) parent.start else leadingIconRef.end)
                    end.linkTo(if (trailingIcon == null) parent.end else trailingIconRef.start)
                    top.linkTo(parent.top)
                    if (warningView == null) {
                        bottom.linkTo(parent.bottom)
                    }
                    width = Dimension.fillToConstraints
                },
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = RadixTheme.typography.body1Header,
                color = if (isErrorText) {
                    RadixTheme.colors.error
                } else {
                    RadixTheme.colors.text
                }
            )
            subtitleView?.let { subtitle ->
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXSmall))
                subtitle()
            }
            infoView?.let { info ->
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
                info()
            }
        }
        trailingIcon?.let { trailing ->
            Box(
                modifier = Modifier
                    .enabledOpacity(isEnabled, 0.1f)
                    .constrainAs(trailingIconRef) {
                        start.linkTo(contentRef.end, paddingMedium)
                        end.linkTo(parent.end)
                        top.linkTo(contentRef.top)
                        bottom.linkTo(contentRef.bottom)
                    },
                contentAlignment = Center
            ) {
                trailing()
            }
        }
        warningView?.let { warning ->
            val padding = RadixTheme.dimensions.paddingMedium
            Box(
                modifier = Modifier.constrainAs(warningRef) {
                    centerHorizontallyTo(contentRef, bias = 0f)
                    top.linkTo(contentRef.bottom, padding)
                    width = Dimension.fillToConstraints
                }
            ) {
                warning()
            }
        }
    }
}

@Composable
fun DefaultSettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    info: String? = null,
    isErrorText: Boolean = false,
    isEnabled: Boolean = true,
    warningView: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (BoxScope.() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = {
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.icon
        )
    }
) {
    DefaultSettingsItem(
        modifier = modifier,
        title = title,
        onClick = onClick,
        isErrorText = isErrorText,
        isEnabled = isEnabled,
        subtitleView = subtitle?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body1Regular,
                    color = if (isErrorText) {
                        RadixTheme.colors.error
                    } else {
                        RadixTheme.colors.icon
                    }
                )
            }
        },
        infoView = info?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.textSecondary
                )
            }
        },
        leadingIcon = leadingIcon,
        warningView = warningView,
        trailingIcon = trailingIcon
    )
}

@Composable
fun DefaultSettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    info: String? = null,
    isErrorText: Boolean = false,
    warnings: ImmutableList<String>? = null,
    @DrawableRes leadingIconRes: Int? = null,
    trailingIcon: @Composable (() -> Unit)? = {
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = if (isErrorText) RadixTheme.colors.error else RadixTheme.colors.icon
        )
    }
) {
    DefaultSettingsItem(
        modifier = modifier,
        title = title,
        onClick = onClick,
        subtitle = subtitle,
        info = info,
        isErrorText = isErrorText,
        warningView = if (warnings.isNullOrEmpty()) {
            null
        } else {
            {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                ) {
                    Spacer(modifier = Modifier)
                    warnings.forEach { warning ->
                        PromptLabel(
                            text = warning
                        )
                    }
                }
            }
        },
        leadingIcon = leadingIconRes?.let {
            {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = it),
                    contentDescription = null,
                    tint = if (isErrorText) {
                        RadixTheme.colors.error
                    } else {
                        RadixTheme.colors.icon
                    }
                )
            }
        },
        trailingIcon = trailingIcon
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        Column {
            DefaultSettingsItem(
                title = "Title",
                onClick = {},
                subtitle = "Subtitle",
                info = "Info",
                warnings = persistentListOf("Warning"),
                leadingIconRes = R.drawable.ic_gateways
            )

            DefaultSettingsItem(
                title = "Title",
                onClick = {},
                subtitle = "Subtitle",
                info = "Info",
                isErrorText = true,
                warnings = persistentListOf("Warning"),
                leadingIconRes = R.drawable.ic_gateways
            )
        }
    }
}

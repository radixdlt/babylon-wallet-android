package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
fun DefaultSettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    subtitleView: @Composable (ColumnScope.() -> Unit)? = null,
    infoView: @Composable (ColumnScope.() -> Unit)? = null,
    leadingIcon: @Composable (BoxScope.() -> Unit)? = null,
    warningView: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = {
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.gray1
        )
    }
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.defaultBackground)
            .throttleClickable(onClick = onClick)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingLarge),
    ) {
        val (leadingIconRef, contentRef, trailingIconRef, warningRef) = createRefs()
        val paddingMedium = RadixTheme.dimensions.paddingMedium
        leadingIcon?.let { icon ->
            Box(
                modifier = Modifier
                    .size(32.dp)
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
            modifier = Modifier.constrainAs(contentRef) {
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
                color = RadixTheme.colors.gray1
            )
            subtitleView?.let { subtitle ->
                subtitle()
            }
            infoView?.let { info ->
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
                info()
            }
        }
        trailingIcon?.let { trailing ->
            Box(
                modifier = Modifier
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
            val paddingLarge = RadixTheme.dimensions.paddingLarge
            Box(
                modifier = Modifier.constrainAs(warningRef) {
                    centerHorizontallyTo(contentRef, bias = 0f)
                    top.linkTo(contentRef.bottom, paddingLarge)
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
    warnings: ImmutableList<String>? = null,
    @DrawableRes leadingIcon: Int? = null,
    trailingIcon: @Composable (() -> Unit)? = {
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.gray1
        )
    }
) {
    DefaultSettingsItem(
        modifier = modifier,
        title = title,
        onClick = onClick,
        subtitleView = subtitle?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }
        },
        infoView = info?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
            }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = it),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
            }
        },
        warningView = warnings?.let {
            {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                ) {
                    it.forEach { warning ->
                        Row(
                            verticalAlignment = CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_warning_error),
                                contentDescription = null,
                                tint = RadixTheme.colors.orange1
                            )
                            Text(
                                text = warning,
                                style = RadixTheme.typography.body2HighImportance,
                                color = RadixTheme.colors.orange1
                            )
                        }
                    }
                }
            }
        },
        trailingIcon = trailingIcon
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        DefaultSettingsItem(
            title = "Title",
            onClick = {},
            subtitle = "Subtitle",
            info = "Info",
            warnings = persistentListOf("Warning"),
            leadingIcon = R.drawable.ic_gateways
        )
    }
}

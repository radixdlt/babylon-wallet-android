package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.ExpandableText
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.utils.openUrl
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun MetadataView(
    modifier: Modifier = Modifier,
    key: String,
    isLocked: Boolean = false,
    isRenderedInNewLine: Boolean = false,
    valueView: @Composable () -> Unit
) {
    if (isRenderedInNewLine) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            MetadataKeyView(key = key, isLocked = isLocked)
            valueView()
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            MetadataKeyView(key = key, isLocked = isLocked)
            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
            valueView()
        }
    }
}

@Composable
fun MetadataView(
    modifier: Modifier = Modifier,
    metadata: Metadata
) {
    MetadataView(
        modifier = modifier,
        key = metadata.key,
        isLocked = metadata.isLocked,
        isRenderedInNewLine = metadata.isRenderedInNewLine,
    ) {
        MetadataValueView(
            metadata = metadata,
            isRenderedInNewLine = metadata.isRenderedInNewLine
        )
    }
}

@Composable
fun MetadataKeyView(
    modifier: Modifier = Modifier,
    metadata: Metadata,
    style: TextStyle = RadixTheme.typography.body1Regular,
    color: Color = RadixTheme.colors.gray2,
) {
    MetadataKeyView(
        modifier = modifier,
        key = metadata.key,
        isLocked = metadata.isLocked,
        style = style,
        color = color
    )
}

@Composable
fun MetadataKeyView(
    modifier: Modifier = Modifier,
    key: String,
    isLocked: Boolean,
    style: TextStyle = RadixTheme.typography.body1Regular,
    color: Color = RadixTheme.colors.gray2,
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            append(key)
            append(" ")
            if (isLocked) {
                appendInlineContent(id = "lock_icon")
            }
        },
        style = style,
        color = color,
        textAlign = TextAlign.Start,
        inlineContent = mapOf(
            "lock_icon" to InlineTextContent(
                Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_lock),
                    contentDescription = null
                )
            }
        )
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
fun MetadataValueView(
    modifier: Modifier = Modifier,
    metadata: Metadata,
    isRenderedInNewLine: Boolean,
    style: TextStyle = RadixTheme.typography.body1HighImportance,
    color: Color = RadixTheme.colors.gray1
) {
    val context = LocalContext.current
    when (metadata) {
        is Metadata.Collection, is Metadata.Map -> Text(
            modifier = modifier,
            text = stringResource(id = R.string.assetDetails_NFTDetails_complexData),
            style = style,
            color = color,
            textAlign = if (isRenderedInNewLine) TextAlign.Start else TextAlign.End,
            maxLines = 2
        )

        is Metadata.Primitive -> when (metadata.valueType) {
            MetadataType.Bool,
            is MetadataType.Integer,
            MetadataType.Bytes,
            MetadataType.Enum,
            MetadataType.PublicKeyEcdsaSecp256k1,
            MetadataType.PublicKeyEddsaEd25519,
            MetadataType.PublicKeyHashEcdsaSecp256k1,
            MetadataType.PublicKeyHashEddsaEd25519 -> Text(
                modifier = modifier,
                text = metadata.value,
                style = style,
                color = color,
                textAlign = if (isRenderedInNewLine) TextAlign.Start else TextAlign.End,
                maxLines = 2
            )

            MetadataType.String -> ExpandableText(
                modifier = modifier,
                text = metadata.value,
                style = style.copy(
                    color = color,
                    textAlign = if (isRenderedInNewLine) TextAlign.Start else TextAlign.End,
                ),
                toggleStyle = style.copy(
                    color = RadixTheme.colors.gray2
                ),
            )

            MetadataType.Instant -> {
                val displayable = remember(metadata.value) {
                    val epochSeconds = metadata.value.toLongOrNull() ?: return@remember metadata.value
                    val dateTime = Instant.ofEpochSecond(epochSeconds)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                    dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                }
                Text(
                    modifier = modifier,
                    text = displayable,
                    style = style,
                    color = color,
                    textAlign = if (isRenderedInNewLine) TextAlign.Start else TextAlign.End,
                    maxLines = 2
                )
            }

            MetadataType.Address -> ActionableAddressView(
                modifier = modifier,
                address = remember(metadata.value) {
                    Address.init(metadata.value)
                },
                textStyle = style,
                textColor = color,
                iconColor = color
            )

            MetadataType.NonFungibleGlobalId -> ActionableAddressView(
                modifier = modifier,
                globalId = remember(metadata.value) {
                    NonFungibleGlobalId.init(metadata.value)
                }.copy(),
                textStyle = style,
                textColor = color,
                iconColor = color
            )

            MetadataType.NonFungibleLocalId -> ActionableAddressView(
                modifier = modifier,
                localId = remember(metadata.value) {
                    NonFungibleLocalId.init(metadata.value)
                },
                textStyle = style,
                textColor = color,
                iconColor = color
            )

            MetadataType.Decimal -> Text(
                modifier = modifier,
                // If value is unable to transform to big decimal we just display raw value
                text = metadata.value.toDecimal192OrNull()?.formatted() ?: metadata.value,
                style = style,
                color = color,
                textAlign = if (isRenderedInNewLine) TextAlign.Start else TextAlign.End,
                maxLines = 2
            )

            MetadataType.Url -> Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { context.openUrl(metadata.value) },
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metadata.value,
                    style = RadixTheme.typography.body1StandaloneLink,
                    color = RadixTheme.colors.blue1
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_external_link),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray3
                )
            }
        }
    }
}

private const val ASSET_METADATA_SHORT_STRING_THRESHOLD = 40
private val Metadata.isRenderedInNewLine: Boolean
    get() = this is Metadata.Primitive && (
        valueType is MetadataType.Url ||
            (valueType is MetadataType.String && value.length > ASSET_METADATA_SHORT_STRING_THRESHOLD)
        )

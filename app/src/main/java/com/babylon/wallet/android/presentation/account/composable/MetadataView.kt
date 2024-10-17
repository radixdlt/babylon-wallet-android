package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.ExpandableText
import com.babylon.wallet.android.presentation.ui.composables.LinkText
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.copyToClipboard
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    valueContent: @Composable () -> Unit
) {
    if (isRenderedInNewLine) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            MetadataKeyView(key = key, isLocked = isLocked)
            valueContent()
        }
    } else {
        ConstraintLayout(modifier = modifier) {
            val (keyView, valueView) = createRefs()

            MetadataKeyView(
                modifier = Modifier.constrainAs(keyView) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    end.linkTo(valueView.start, margin = 12.dp)
                    width = if (key.length > SHORT_KEY_THRESHOLD) {
                        Dimension.fillToConstraints
                    } else {
                        Dimension.preferredWrapContent
                    }
                    height = Dimension.wrapContent
                },
                key = key,
                isLocked = isLocked
            )
            Box(
                modifier = Modifier.constrainAs(valueView) {
                    start.linkTo(keyView.end)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    width = Dimension.fillToConstraints
                    height = Dimension.wrapContent
                },
                contentAlignment = Alignment.TopEnd
            ) {
                valueContent()
            }
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
            modifier = Modifier.wrapContentSize(
                align = if (metadata.isRenderedInNewLine) Alignment.TopStart else Alignment.TopEnd
            ),
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
                Placeholder(style.fontSize, style.fontSize, PlaceholderVerticalAlign.TextCenter)
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_lock),
                    contentDescription = null,
                    tint = color
                )
            }
        )
    )
}

@Suppress("CyclomaticComplexMethod", "InjectDispatcher")
@Composable
fun MetadataValueView(
    modifier: Modifier = Modifier,
    metadata: Metadata,
    isRenderedInNewLine: Boolean,
    style: TextStyle = RadixTheme.typography.body1HighImportance,
    color: Color = RadixTheme.colors.gray1,
    iconColor: Color = RadixTheme.colors.gray2
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
            MetadataType.Enum -> Text(
                modifier = modifier,
                text = metadata.value,
                style = style,
                color = color,
                textAlign = if (isRenderedInNewLine) TextAlign.Start else TextAlign.End,
                maxLines = 2
            )

            MetadataType.PublicKeyEcdsaSecp256k1,
            MetadataType.PublicKeyEddsaEd25519,
            MetadataType.PublicKeyHashEcdsaSecp256k1,
            MetadataType.PublicKeyHashEddsaEd25519 -> Row(
                modifier = Modifier.throttleClickable {
                    context.copyToClipboard(
                        label = metadata.key,
                        value = metadata.value,
                        successMessage = context.getString(R.string.addressAction_copiedToClipboard)
                    )
                },
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = modifier.weight(1f),
                    text = metadata.value,
                    style = style,
                    color = color,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val iconSize = with(LocalDensity.current) {
                    style.fontSize.toPx().toDp()
                }
                Icon(
                    modifier = Modifier
                        .padding(start = RadixTheme.dimensions.paddingXSmall)
                        .size(iconSize),
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_copy),
                    contentDescription = null,
                    tint = iconColor
                )
            }

            MetadataType.String -> ExpandableText(
                modifier = modifier,
                text = metadata.value,
                style = style.copy(
                    color = color,
                    textAlign = if (isRenderedInNewLine) TextAlign.Start else TextAlign.End,
                ),
                toggleStyle = style.copy(color = iconColor)
            )

            MetadataType.Instant -> {
                var displayable by remember {
                    mutableStateOf("")
                }

                LaunchedEffect(metadata.value) {
                    withContext(Dispatchers.Default) {
                        val formatted = metadata.value.toLongOrNull()?.let { epochSeconds ->
                            val dateTime = Instant.ofEpochSecond(epochSeconds)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()

                            dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                        } ?: metadata.value

                        displayable = formatted
                    }
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
                iconColor = iconColor
            )

            MetadataType.NonFungibleGlobalId -> ActionableAddressView(
                modifier = modifier,
                globalId = remember(metadata.value) {
                    NonFungibleGlobalId.init(metadata.value)
                }.copy(),
                showOnlyLocalId = false,
                textStyle = style,
                textColor = color,
                iconColor = iconColor
            )

            MetadataType.NonFungibleLocalId -> {
                var displayable by remember {
                    mutableStateOf("")
                }

                LaunchedEffect(metadata.value) {
                    withContext(Dispatchers.Default) {
                        displayable = NonFungibleLocalId.init(metadata.value).formatted(AddressFormat.DEFAULT)
                    }
                }

                Text(
                    modifier = modifier,
                    text = displayable,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = if (isRenderedInNewLine) TextAlign.Start else TextAlign.End,
                    maxLines = 2
                )
            }

            MetadataType.Decimal -> {
                var displayable by remember {
                    mutableStateOf("")
                }

                LaunchedEffect(metadata.value) {
                    withContext(Dispatchers.Default) {
                        // If value is unable to transform to Decimal192 we just display raw value
                        displayable = metadata.value.toDecimal192OrNull()?.formatted() ?: metadata.value
                    }
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

            MetadataType.Url, MetadataType.Origin -> LinkText(
                modifier = modifier.fillMaxWidth(),
                url = metadata.value
            )
        }
    }
}

private const val SHORT_KEY_THRESHOLD = 30
private const val SHORT_VALUE_THRESHOLD = 40
private val Metadata.isRenderedInNewLine: Boolean
    get() = this is Metadata.Primitive && (
        valueType is MetadataType.Url || valueType is MetadataType.Origin ||
            (valueType is MetadataType.String && value.length > SHORT_VALUE_THRESHOLD) ||
            (valueType is MetadataType.NonFungibleGlobalId)
        )

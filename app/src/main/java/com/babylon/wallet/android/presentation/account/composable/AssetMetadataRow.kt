package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.utils.openUrl
import rdx.works.core.displayableQuantity
import java.util.Locale

@Composable
fun AssetMetadataRow(
    modifier: Modifier,
    key: String,
    valueView: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(end = RadixTheme.dimensions.paddingMedium),
            text = key.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )

        valueView()
    }
}

@Composable
fun Metadata.View(modifier: Modifier) {
    if (this is Metadata.Primitive && this.valueType == MetadataType.Url) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            KeyView()
            ValueView()
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KeyView()
            ValueView()
        }
    }
}

@Composable
fun Metadata.KeyView(
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier.padding(end = RadixTheme.dimensions.paddingMedium),
        text = key.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        },
        style = RadixTheme.typography.body1Regular,
        color = RadixTheme.colors.gray2,
        textAlign = TextAlign.Start
    )
}

@Composable
fun Metadata.ValueView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    when (this) {
        is Metadata.Collection, is Metadata.Map -> Text(
            modifier = modifier,
            text = stringResource(id = R.string.assetDetails_NFTDetails_complexData),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End,
            maxLines = 2
        )

        is Metadata.Primitive -> when (valueType) {
            MetadataType.Bool,
            is MetadataType.Integer,
            MetadataType.Bytes,
            MetadataType.Instant,
            MetadataType.String,
            MetadataType.Enum,
            MetadataType.PublicKeyEcdsaSecp256k1,
            MetadataType.PublicKeyEddsaEd25519,
            MetadataType.PublicKeyHashEcdsaSecp256k1,
            MetadataType.PublicKeyHashEddsaEd25519 ->
                Text(
                    modifier = modifier,
                    text = value,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )

            MetadataType.Address, MetadataType.NonFungibleGlobalId, MetadataType.NonFungibleLocalId ->
                ActionableAddressView(
                    modifier = modifier,
                    address = value
                )

            MetadataType.Decimal ->
                Text(
                    modifier = modifier,
                    text = value.toBigDecimalOrNull()?.displayableQuantity().orEmpty(),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )

            MetadataType.Url ->
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable { context.openUrl(value) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
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

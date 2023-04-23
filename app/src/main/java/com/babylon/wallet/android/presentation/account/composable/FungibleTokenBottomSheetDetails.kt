package com.babylon.wallet.android.presentation.account.composable

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.repository.buildLargeImageRequest
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.model.toTokenUiModel
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.util.Locale

@Composable
fun FungibleTokenBottomSheetDetails(
    token: TokenUiModel,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = token.name.orEmpty(),
            onBackClick = onCloseClick,
            modifier = Modifier.fillMaxWidth(),
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Close
        )
        Spacer(modifier = Modifier.height(22.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val placeholder = if (token.isXrd()) {
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token)
            } else {
                rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))
            }
            AsyncImage(
                model = LocalContext.current.buildLargeImageRequest(token.iconUrl),
                placeholder = placeholder,
                fallback = placeholder,
                error = placeholder,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .background(RadixTheme.colors.gray3, RadixTheme.shapes.circle)
                    .clip(RadixTheme.shapes.circle)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            TokenBalance(token)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            token.description?.let { desc ->
                Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray5)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = desc,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray5)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            if (token.metadata.isNotEmpty()) {
                token.metadata.forEach { mapEntry ->
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    AssetMetadataRow(Modifier.fillMaxWidth(), mapEntry.key, mapEntry.value)
                }
            }
            ResourceAddressRow(
                modifier = Modifier.fillMaxWidth(),
                address = token.address
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ResourceAddressRow(
    modifier: Modifier,
    address: String
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.resource_address).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )

        ActionableAddressView(
            address = address,
            textStyle = RadixTheme.typography.body1Regular,
            textColor = RadixTheme.colors.gray1,
            iconColor = RadixTheme.colors.gray2
        )
    }
}

@Composable
private fun TokenBalance(token: TokenUiModel, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier.alignByBaseline(),
            text = token.tokenQuantityToDisplay,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Text(
            modifier = Modifier.alignByBaseline(),
            text = stringResource(id = R.string.space) + token.symbol.orEmpty(),
            style = RadixTheme.typography.header,
            color = RadixTheme.colors.gray1
        )
    }
}

@Preview
@Composable
fun FungibleTokenBottomSheetDetailsPreview() {
    RadixWalletTheme {
        FungibleTokenBottomSheetDetails(
            token = SampleDataProvider().sampleFungibleTokens().first().toTokenUiModel(),
            onCloseClick = {}
        )
    }
}

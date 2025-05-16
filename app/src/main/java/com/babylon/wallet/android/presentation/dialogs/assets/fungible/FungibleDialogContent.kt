package com.babylon.wallet.android.presentation.dialogs.assets.fungible

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.MetadataView
import com.babylon.wallet.android.presentation.dialogs.assets.AssetDialogArgs
import com.babylon.wallet.android.presentation.dialogs.assets.BehavioursSection
import com.babylon.wallet.android.presentation.dialogs.assets.DescriptionSection
import com.babylon.wallet.android.presentation.dialogs.assets.NonStandardMetadataSection
import com.babylon.wallet.android.presentation.dialogs.assets.TagsSection
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.transaction.composables.LargeBoundedAmountSection
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.ShimmeringView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.FiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.xrd
import com.radixdlt.sargon.samples.sample
import rdx.works.core.domain.assets.AssetBehaviour
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.Metadata

@Composable
fun FungibleDialogContent(
    modifier: Modifier = Modifier,
    token: Token?,
    tokenPrice: AssetPrice.TokenPrice?,
    args: AssetDialogArgs.Fungible,
    isLoadingBalance: Boolean,
    canBeHidden: Boolean,
    onInfoClick: (GlossaryItem) -> Unit,
    onHideClick: (() -> Unit)? = null
) {
    val resourceAddress = args.resourceAddress
    val isNewlyCreated = args.isNewlyCreated
    val amount = remember(args) { args.fungibleAmountOf(resourceAddress) }
        ?: remember(token) { token?.resource?.ownedAmount?.let { BoundedAmount.Exact(it) } }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FungibleIconSection(token = token)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            if (amount != null) {
                LargeBoundedAmountSection(
                    modifier = Modifier
                        .widthIn(min = if (token == null) RadixTheme.dimensions.amountShimmeringWidth else 0.dp)
                        .radixPlaceholder(visible = token == null),
                    boundedAmount = amount,
                    symbol = token?.resource?.symbol
                )

                val fiatPrice = tokenPrice?.price
                if (isLoadingBalance) {
                    ShimmeringView(
                        modifier = Modifier
                            .padding(top = RadixTheme.dimensions.paddingXXXSmall)
                            .height(12.dp)
                            .fillMaxWidth(0.2f),
                        isVisible = true
                    )
                } else if (fiatPrice != null) {
                    FiatBalanceView(
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
                        fiatPrice = fiatPrice,
                        textStyle = RadixTheme.typography.body2HighImportance
                    )
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            HorizontalDivider(Modifier.fillMaxWidth(), color = RadixTheme.colors.divider)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            DescriptionSection(
                modifier = Modifier.fillMaxWidth(),
                description = token?.resource?.description,
                infoUrl = token?.resource?.infoUrl
            )

            AddressRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                address = Address.Resource(resourceAddress),
                isNewlyCreatedEntity = isNewlyCreated
            )
            if (!token?.resource?.name.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    key = stringResource(id = R.string.assetDetails_name)
                ) {
                    Text(
                        text = token?.resource?.name.orEmpty(),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.text
                    )
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            if (!isNewlyCreated) {
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    key = stringResource(id = R.string.assetDetails_currentSupply)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = RadixTheme.dimensions.paddingDefault)
                            .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                            .radixPlaceholder(visible = token?.resource?.currentSupply == null),
                        text = when (val supply = token?.resource?.currentSupply) {
                            null -> ""
                            0.toDecimal192() -> stringResource(id = R.string.assetDetails_supplyUnkown)
                            else -> supply.formatted()
                        },
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.text,
                        textAlign = TextAlign.End
                    )
                }

                token?.resource?.divisibility?.let {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    MetadataView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                        key = stringResource(id = R.string.assetDetails_divisibility)
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(start = RadixTheme.dimensions.paddingDefault)
                                .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2),
                            text = it.value.toString(),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.text,
                            textAlign = TextAlign.End
                        )
                    }
                }

                BehavioursSection(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    isXRD = token?.resource?.isXrd ?: false,
                    behaviours = token?.resource?.behaviours,
                    onInfoClick = onInfoClick
                )

                TagsSection(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    tags = token?.resource?.tags
                )

                token?.resource?.let { resource ->
                    NonStandardMetadataSection(resource = resource)
                }
            }
        }

        if (canBeHidden) {
            Spacer(modifier = Modifier.weight(1f))

            RadixBottomBar(
                modifier = Modifier.padding(top = RadixTheme.dimensions.paddingLarge),
                button = {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.assetDetails_hideAsset),
                        onClick = { onHideClick?.invoke() }
                    )
                }
            )
        } else {
            Spacer(
                modifier = Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}

@Composable
private fun FungibleIconSection(
    modifier: Modifier = Modifier,
    token: Token?
) {
    if (token?.resource != null) {
        Thumbnail.Fungible(
            modifier = modifier.size(104.dp),
            token = token.resource
        )
    } else {
        Box(
            modifier = modifier
                .size(104.dp)
                .radixPlaceholder(
                    visible = true,
                    shape = CircleShape
                )
        )
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
private fun FungibleDialogContentPreview() {
    RadixWalletPreviewTheme {
        FungibleDialogContent(
            token = Token(
                resource = Resource.FungibleResource(
                    address = ResourceAddress.xrd(NetworkId.MAINNET),
                    ownedAmount = 666.toDecimal192(),
                    currentSupply = Decimal192.sample.invoke(),
                    assetBehaviours = setOf(
                        AssetBehaviour.SUPPLY_INCREASABLE,
                        AssetBehaviour.SUPPLY_FLEXIBLE
                    ),
                    metadata = listOf(
                        Metadata.Collection(
                            key = ExplicitMetadataKey.TAGS.key,
                            values = listOf(),
                        )
                    )
                )
            ),
            tokenPrice = AssetPrice.TokenPrice(
                asset = Token(
                    resource = Resource.FungibleResource(
                        address = ResourceAddress.xrd(NetworkId.MAINNET),
                        ownedAmount = 666.toDecimal192(),
                    ),
                ),
                price = FiatPrice(price = 999.toDecimal192(), currency = SupportedCurrency.USD)
            ),
            args = AssetDialogArgs.Fungible(
                resourceAddress = ResourceAddress.xrd(NetworkId.MAINNET),
                isNewlyCreated = false,
                underAccountAddress = null,
                amounts = mapOf(
                    ResourceAddress.xrd(NetworkId.MAINNET).string to BoundedAmount.Exact(
                        Decimal192.sample()
                    )
                )
            ),
            isLoadingBalance = false,
            canBeHidden = true,
            onInfoClick = {}
        )
    }
}

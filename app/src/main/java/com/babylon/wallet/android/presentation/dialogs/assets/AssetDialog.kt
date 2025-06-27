@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.dialogs.assets

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.MetadataKeyView
import com.babylon.wallet.android.presentation.account.composable.MetadataView
import com.babylon.wallet.android.presentation.dialogs.assets.fungible.FungibleDialogContent
import com.babylon.wallet.android.presentation.dialogs.assets.lsu.LSUDialogContent
import com.babylon.wallet.android.presentation.dialogs.assets.nonfungible.NonFungibleAssetDialogContent
import com.babylon.wallet.android.presentation.dialogs.assets.pool.PoolUnitDialogContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.HideResourceSheetContent
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.LinkText
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.assets.Behaviour
import com.babylon.wallet.android.presentation.ui.composables.assets.Tag
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.none
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.AssetBehaviours
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Tag

@Composable
fun AssetDialog(
    modifier: Modifier = Modifier,
    viewModel: AssetDialogViewModel = hiltViewModel(),
    onInfoClick: (GlossaryItem) -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch { sheetState.show() }
    }
    val onDismissRequest: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = viewModel::onMessageShown
    )

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        wrapContent = true,
        windowInsets = {
            WindowInsets.none // Bottom insets are handled by child views
        },
        sheetContent = {
            Scaffold(
                topBar = {
                    RadixCenteredTopAppBar(
                        title = state.asset?.displayTitle().orEmpty(),
                        onBackClick = onDismissRequest,
                        windowInsets = WindowInsets.none
                    )
                },
                snackbarHost = {
                    RadixSnackbarHost(
                        hostState = snackBarHostState,
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
                    )
                },
                containerColor = RadixTheme.colors.background,
                contentWindowInsets = WindowInsets.none
            ) { padding ->
                val isLoadingBalance = if (state.isFiatBalancesEnabled) {
                    state.isLoadingBalance
                } else {
                    false
                }

                when (val asset = state.asset) {
                    is Token -> FungibleDialogContent(
                        modifier = Modifier.padding(padding),
                        args = state.args as AssetDialogArgs.Fungible,
                        token = asset,
                        tokenPrice = state.assetPrice as? AssetPrice.TokenPrice,
                        isLoadingBalance = isLoadingBalance,
                        canBeHidden = state.canBeHidden,
                        onInfoClick = onInfoClick,
                        onHideClick = { viewModel.onHideClick(asset) }
                    )

                    is LiquidStakeUnit -> LSUDialogContent(
                        modifier = Modifier.padding(padding),
                        args = state.args as AssetDialogArgs.Fungible,
                        lsu = asset,
                        price = state.assetPrice as? AssetPrice.LSUPrice,
                        isLoadingBalance = isLoadingBalance,
                        onInfoClick = onInfoClick
                    )

                    is PoolUnit -> PoolUnitDialogContent(
                        modifier = Modifier.padding(padding),
                        args = state.args as AssetDialogArgs.Fungible,
                        poolUnit = asset,
                        poolUnitPrice = state.assetPrice as? AssetPrice.PoolUnitPrice,
                        isLoadingBalance = isLoadingBalance,
                        canBeHidden = state.canBeHidden,
                        onInfoClick = onInfoClick,
                        onHideClick = { viewModel.onHideClick(asset) }
                    )
                    // Includes NFTs and stake claims
                    is Asset.NonFungible -> {
                        val args = state.args as? AssetDialogArgs.NonFungible
                        NonFungibleAssetDialogContent(
                            modifier = Modifier.padding(padding),
                            resourceAddress = state.args.resourceAddress,
                            localId = args?.localId,
                            asset = asset,
                            isNewlyCreated = state.args.isNewlyCreated,
                            claimState = state.claimState,
                            accountContext = state.accountContext,
                            price = state.assetPrice as? AssetPrice.StakeClaimPrice,
                            isLoadingBalance = isLoadingBalance,
                            boundedAmount = args?.amount,
                            canBeHidden = state.canBeHidden,
                            onInfoClick = onInfoClick,
                            onClaimClick = viewModel::onClaimClick,
                            onHideClick = { viewModel.onHideClick(asset) }
                        )
                    }

                    // When asset is not retrieved yet, we show the placeholders for tokens, or NFTs
                    null -> when (val args = state.args) {
                        is AssetDialogArgs.Fungible -> FungibleDialogContent(
                            modifier = Modifier.padding(padding),
                            args = state.args as AssetDialogArgs.Fungible,
                            token = null,
                            tokenPrice = null,
                            isLoadingBalance = state.isLoadingBalance,
                            canBeHidden = false,
                            onInfoClick = onInfoClick
                        )

                        is AssetDialogArgs.NonFungible -> NonFungibleAssetDialogContent(
                            modifier = Modifier.padding(padding),
                            resourceAddress = state.args.resourceAddress,
                            localId = args.localId,
                            asset = null,
                            price = null,
                            isNewlyCreated = args.isNewlyCreated,
                            isLoadingBalance = false, // we do not need to pass value here because it's for NFTs
                            canBeHidden = false
                        )
                    }
                }
            }
        }
    )

    HideAssetSheet(
        type = state.showHideConfirmation,
        onHideClick = viewModel::hideAsset,
        onDismiss = viewModel::onDismissHideConfirmation
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is AssetDialogViewModel.Event.Close -> onDismiss()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HideAssetSheet(
    type: AssetDialogViewModel.State.HideConfirmationType?,
    onHideClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    SyncSheetState(
        sheetState = sheetState,
        isSheetVisible = type != null,
        onSheetClosed = onDismiss
    )

    if (type != null) {
        DefaultModalSheetLayout(
            wrapContent = true,
            enableImePadding = true,
            sheetState = sheetState,
            sheetContent = {
                val content = when (type) {
                    is AssetDialogViewModel.State.HideConfirmationType.Asset -> Triple(
                        stringResource(id = R.string.confirmation_hideAsset_title),
                        stringResource(id = R.string.confirmation_hideAsset_message),
                        stringResource(id = R.string.confirmation_hideAsset_button)
                    )

                    is AssetDialogViewModel.State.HideConfirmationType.Collection -> Triple(
                        stringResource(id = R.string.confirmation_hideCollection_title),
                        stringResource(id = R.string.confirmation_hideCollection_message, type.name),
                        stringResource(id = R.string.confirmation_hideCollection_button)
                    )
                }

                HideResourceSheetContent(
                    title = content.first,
                    description = content.second,
                    positiveButton = content.third,
                    onPositiveButtonClick = onHideClick,
                    onClose = onDismiss
                )
            },
            showDragHandle = true,
            onDismissRequest = onDismiss
        )
    }
}

@Composable
fun Asset.displayTitle() = when (this) {
    is Token -> resource.name
    is LiquidStakeUnit -> name
    is PoolUnit -> displayTitle
    is Asset.NonFungible -> {
        val item = resource.items.firstOrNull()
        if (item != null) {
            item.nameTruncated ?: resource.name
        } else {
            resource.name
        }
    }
}

@Composable
fun DescriptionSection(
    modifier: Modifier = Modifier,
    description: String?,
    infoUrl: Uri?
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (!description.isNullOrBlank()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall)
                    .padding(bottom = if (infoUrl != null) RadixTheme.dimensions.paddingSemiLarge else 0.dp),
                text = description,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Start
            )
        }

        if (infoUrl != null) {
            Text(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                text = stringResource(id = R.string.assetDetails_moreInfo),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.textSecondary
            )

            LinkText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall)
                    .padding(top = RadixTheme.dimensions.paddingXXSmall),
                url = infoUrl
            )
        }

        if (!description.isNullOrBlank() || infoUrl != null) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            HorizontalDivider(Modifier.fillMaxWidth(), color = RadixTheme.colors.divider)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
    }
}

@Composable
fun NonStandardMetadataSection(
    modifier: Modifier = Modifier,
    resource: Resource
) {
    val metadata = remember(resource.metadata) {
        resource.nonStandardMetadata
    }

    if (metadata.isNotEmpty()) {
        Column(
            modifier = modifier
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            HorizontalDivider(Modifier.fillMaxWidth(), color = RadixTheme.colors.divider)

            metadata.forEach { metadata ->
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    metadata = metadata
                )
            }
        }
    }
}

@Composable
fun BehavioursSection(
    modifier: Modifier = Modifier,
    behaviours: AssetBehaviours?,
    isXRD: Boolean = false,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(modifier = modifier) {
        if (behaviours == null || behaviours.isNotEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = RadixTheme.dimensions.paddingDefault,
                        bottom = RadixTheme.dimensions.paddingSmall
                    ),
                text = stringResource(id = R.string.assetDetails_behavior),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.textSecondary
            )
        }

        if (behaviours == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(RadixTheme.dimensions.paddingLarge)
                    .radixPlaceholder(visible = true)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(RadixTheme.dimensions.paddingLarge)
                    .radixPlaceholder(visible = true)
            )
        } else {
            behaviours.forEach { behaviour ->
                Behaviour(
                    icon = behaviour.icon(),
                    name = behaviour.name(isXRD)
                )
            }
            InfoButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = RadixTheme.dimensions.paddingDefault,
                        bottom = RadixTheme.dimensions.paddingSmall
                    ),
                text = stringResource(id = R.string.infoLink_title_behaviors),
                onClick = {
                    onInfoClick(GlossaryItem.behaviors)
                }
            )
        }
    }
}

@Composable
fun TagsSection(
    modifier: Modifier = Modifier,
    tags: ImmutableList<Tag>?
) {
    if (!tags.isNullOrEmpty()) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            MetadataKeyView(
                modifier = Modifier.fillMaxWidth(),
                key = stringResource(id = R.string.assetDetails_tags),
                isLocked = false
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            TagsView(
                modifier = Modifier.fillMaxWidth(),
                tags = tags
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsView(
    tags: ImmutableList<Tag>,
    modifier: Modifier = Modifier,
    borderColor: Color = RadixTheme.colors.divider,
    iconColor: Color = RadixTheme.colors.iconSecondary,
    maxLines: Int = Int.MAX_VALUE
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall),
        maxLines = maxLines,
        content = {
            tags.forEach { tag ->
                Tag(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RadixTheme.shapes.roundedTag
                        )
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingSmall,
                            vertical = RadixTheme.dimensions.paddingXXSmall
                        ),
                    tag = tag,
                    iconColor = iconColor
                )
            }
        }
    )
}

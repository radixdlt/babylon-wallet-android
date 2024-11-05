package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.PreviewType.Transaction.InvolvedComponents.DApps
import com.babylon.wallet.android.presentation.transaction.PreviewType.Transaction.InvolvedComponents.None
import com.babylon.wallet.android.presentation.transaction.PreviewType.Transaction.InvolvedComponents.Pools
import com.babylon.wallet.android.presentation.transaction.PreviewType.Transaction.InvolvedComponents.Validators
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircleBorder
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ManifestEncounteredComponentAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Validator

@Composable
fun InvolvedComponentsContent(
    modifier: Modifier = Modifier,
    involvedComponents: PreviewType.Transaction.InvolvedComponents,
    onDAppClick: (DApp) -> Unit,
    onUnknownComponentsClick: (List<Address>) -> Unit
) {
    val isEmpty = remember(involvedComponents) { involvedComponents.isEmpty }

    if (!isEmpty) {
        var isExpanded by rememberSaveable { mutableStateOf(true) }

        Column(modifier = modifier) {
            Title(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(bottom = RadixTheme.dimensions.paddingSmall),
                text = involvedComponents.title(),
                icon = involvedComponents.icon(),
                isExpanded = isExpanded
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                when (involvedComponents) {
                    None -> {}
                    is DApps -> DAppsContent(
                        modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingSmall),
                        involvedDApps = involvedComponents,
                        onDAppClick = onDAppClick,
                        onUnknownComponentsClick = onUnknownComponentsClick
                    )
                    is Pools -> PoolsContent(
                        modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingSmall),
                        pools = involvedComponents,
                        onDAppClick = onDAppClick,
                        onUnknownComponentsClick = onUnknownComponentsClick
                    )
                    is Validators -> ValidatorsContent(
                        modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingSmall),
                        validators = involvedComponents
                    )
                }
            }
        }
    }
}

@Composable
private fun Title(
    modifier: Modifier,
    text: String,
    icon: Painter,
    isExpanded: Boolean
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .size(24.dp)
                .dashedCircleBorder(RadixTheme.colors.gray3),
            painter = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(RadixTheme.colors.gray2),
            contentScale = ContentScale.Inside
        )
        Text(
            modifier = Modifier.padding(start = RadixTheme.dimensions.paddingMedium),
            text = text,
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.gray2,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(
                id = if (isExpanded) {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_up
                } else {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_down
                }
            ),
            tint = RadixTheme.colors.gray2,
            contentDescription = "arrow"
        )
    }
}

@Composable
private fun PreviewType.Transaction.InvolvedComponents.title(): String = when (this) {
    is None -> ""
    is DApps -> stringResource(id = R.string.interactionReview_usingDappsHeading).uppercase()
    is Pools -> when (actionType) {
        Pools.ActionType.Contribution -> stringResource(id = R.string.interactionReview_poolContributionHeading).uppercase()
        Pools.ActionType.Redemption -> stringResource(id = R.string.interactionReview_poolRedemptionHeading).uppercase()
    }
    is Validators -> when (actionType) {
        Validators.ActionType.Stake -> stringResource(id = R.string.interactionReview_stakingToValidatorsHeading).uppercase()
        Validators.ActionType.Unstake -> stringResource(id = R.string.interactionReview_unstakingFromValidatorsHeading).uppercase()
        Validators.ActionType.ClaimStake -> stringResource(id = R.string.interactionReview_claimFromValidatorsHeading).uppercase()
    }
}

@Composable
private fun PreviewType.Transaction.InvolvedComponents.icon(): Painter = when (this) {
    is None -> rememberDrawablePainter(null)
    is DApps -> painterResource(id = DSR.ic_using_dapps)
    is Pools -> painterResource(id = DSR.ic_pools_contribution)
    is Validators -> painterResource(id = DSR.ic_validator)
}

@Composable
private fun DAppsContent(
    modifier: Modifier = Modifier,
    involvedDApps: DApps,
    onDAppClick: (DApp) -> Unit,
    onUnknownComponentsClick: (List<Address>) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        val verifiedDApps = remember(involvedDApps) { involvedDApps.verifiedDapps }

        verifiedDApps.forEach { dApp ->
            InvolvedComponent(
                modifier = Modifier.fillMaxWidth().clickable { onDAppClick(dApp) },
                icon = {
                    Thumbnail.DApp(
                        modifier = Modifier.size(44.dp),
                        dapp = dApp
                    )
                },
                text = dApp.displayName()
            )
        }

        val unknownComponents = remember(involvedDApps) { involvedDApps.unknownComponents }
        if (unknownComponents.isNotEmpty()) {
            InvolvedComponent(
                modifier = Modifier.fillMaxWidth().clickable {
                    onUnknownComponentsClick(unknownComponents.map { it.asGeneral() })
                },
                icon = {
                    Thumbnail.DApp(
                        modifier = Modifier.size(44.dp),
                        dapp = null
                    )
                },
                text = stringResource(id = R.string.interactionReview_unknownComponents, unknownComponents.size)
            )
        }

        if (involvedDApps.morePossibleDAppsPresent) {
            InvolvedComponent(
                modifier = Modifier.fillMaxWidth().clickable {
                    // TODO sergiu
                },
                contentPadding = PaddingValues(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingSmall
                ),
                contentSpacing = RadixTheme.dimensions.paddingMedium,
                icon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(com.babylon.wallet.android.designsystem.R.drawable.ic_using_dapps),
                        tint = RadixTheme.colors.gray3,
                        contentDescription = ""
                    )
                },
                text = stringResource(id = R.string.interactionReview_possibleDappCalls),
                textColor = RadixTheme.colors.gray2,
                textStyle = RadixTheme.typography.body2Link,
                infoIconVisible = true
            )
        }
    }
}

@Composable
private fun PoolsContent(
    modifier: Modifier = Modifier,
    pools: Pools,
    onDAppClick: (DApp) -> Unit,
    onUnknownComponentsClick: (List<Address>) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        val associatedDApps = remember(pools) { pools.associatedDApps }

        associatedDApps.forEach { dApp ->
            InvolvedComponent(
                modifier = Modifier.fillMaxWidth().clickable { onDAppClick(dApp) },
                icon = {
                    Thumbnail.DApp(
                        modifier = Modifier.size(44.dp),
                        dapp = dApp
                    )
                },
                text = dApp.displayName()
            )
        }

        val unknownPools = remember(pools) { pools.unknownPools }
        if (unknownPools.isNotEmpty()) {
            InvolvedComponent(
                modifier = Modifier.fillMaxWidth().clickable { onUnknownComponentsClick(unknownPools.map { it.address.asGeneral() }) },
                icon = {
                    Thumbnail.DApp(
                        modifier = Modifier.size(44.dp),
                        dapp = null
                    )
                },
                text = stringResource(id = R.string.interactionReview_unknownPools, unknownPools.size)
            )
        }
    }
}

@Composable
private fun ValidatorsContent(
    modifier: Modifier = Modifier,
    validators: Validators
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        validators.validators.forEach { validator ->
            InvolvedComponent(
                modifier = Modifier.fillMaxWidth().clickable {  },
                icon = {
                    Thumbnail.Validator(
                        modifier = Modifier.size(44.dp),
                        validator = validator
                    )
                },
                text = validator.name,
                subtitle = {
                    ActionableAddressView(
                        address = validator.address.asGeneral(),
                        textStyle = RadixTheme.typography.body2HighImportance,
                        textColor = RadixTheme.colors.gray2
                    )
                }
            )
        }
    }
}

@Composable
private fun InvolvedComponent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(RadixTheme.dimensions.paddingDefault),
    contentSpacing: Dp = RadixTheme.dimensions.paddingDefault,
    icon: @Composable () -> Unit,
    text: String,
    textColor: Color = RadixTheme.colors.gray1,
    textStyle: TextStyle = RadixTheme.typography.body1Header,
    subtitle: @Composable (() -> Unit)? = null,
    infoIconVisible: Boolean = false
) {
    Row(
        modifier = modifier
            .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
            .clip(RadixTheme.shapes.roundedRectMedium)
            .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(contentSpacing)
    ) {
        icon()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = textStyle,
                color = textColor,
                maxLines = 1
            )

            if (subtitle != null) {
                subtitle()
            }
        }


        if (infoIconVisible) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline),
                tint = RadixTheme.colors.gray3,
                contentDescription = "info"
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun UsingDAppsPreview() {
    val components = remember(Unit) {
        listOf(
            ManifestEncounteredComponentAddress.sampleMainnet() to DApp.sampleMainnet(),
            ManifestEncounteredComponentAddress.sampleMainnet.other() to null
        )
    }
    RadixWalletPreviewTheme {
        InvolvedComponentsContent(
            involvedComponents = DApps(
                components = components
            ),
            onDAppClick = {},
            onUnknownComponentsClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun UsingDAppsWithPossibleOtherDAppsPreview() {
    val components = remember(Unit) {
        listOf(
            ManifestEncounteredComponentAddress.sampleMainnet() to DApp.sampleMainnet(),
            ManifestEncounteredComponentAddress.sampleMainnet.other() to null
        )
    }
    RadixWalletPreviewTheme {
        InvolvedComponentsContent(
            involvedComponents = DApps(
                components = components,
                morePossibleDAppsPresent = true
            ),
            onDAppClick = {},
            onUnknownComponentsClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PoolContributionPreview() {
    val pools = remember(Unit) {
        setOf(
            Pool.sampleMainnet(),
            Pool.sampleMainnet.other()
        )
    }
    RadixWalletPreviewTheme {
        InvolvedComponentsContent(
            involvedComponents = Pools(
                pools = pools,
                actionType = Pools.ActionType.Contribution
            ),
            onDAppClick = {},
            onUnknownComponentsClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PoolRedemptionPreview() {
    val pools = remember(Unit) {
        setOf(
            Pool.sampleMainnet(),
            Pool.sampleMainnet.other()
        )
    }
    RadixWalletPreviewTheme {
        InvolvedComponentsContent(
            involvedComponents = Pools(
                pools = pools,
                actionType = Pools.ActionType.Redemption
            ),
            onDAppClick = {},
            onUnknownComponentsClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun StakePreview() {
    val validators = remember(Unit) {
        setOf(
            Validator.sampleMainnet(),
            Validator.sampleMainnet.other()
        )
    }
    RadixWalletPreviewTheme {
        InvolvedComponentsContent(
            involvedComponents = Validators(
                validators = validators,
                actionType = Validators.ActionType.Stake
            ),
            onDAppClick = {},
            onUnknownComponentsClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun UnstakePreview() {
    val validators = remember(Unit) {
        setOf(
            Validator.sampleMainnet(),
            Validator.sampleMainnet.other()
        )
    }
    RadixWalletPreviewTheme {
        InvolvedComponentsContent(
            involvedComponents = Validators(
                validators = validators,
                actionType = Validators.ActionType.Unstake
            ),
            onDAppClick = {},
            onUnknownComponentsClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun ClaimPreview() {
    val validators = remember(Unit) {
        setOf(
            Validator.sampleMainnet(),
            Validator.sampleMainnet.other()
        )
    }
    RadixWalletPreviewTheme {
        InvolvedComponentsContent(
            involvedComponents = Validators(
                validators = validators,
                actionType = Validators.ActionType.ClaimStake
            ),
            onDAppClick = {},
            onUnknownComponentsClick = {}
        )
    }
}
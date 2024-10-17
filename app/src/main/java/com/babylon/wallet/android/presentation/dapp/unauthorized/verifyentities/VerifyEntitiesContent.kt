package com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.card.SimplePersonaCard
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import rdx.works.core.domain.DApp

@Composable
fun VerifyEntitiesContent(
    modifier: Modifier = Modifier,
    dapp: DApp?,
    entityType: VerifyEntitiesViewModel.State.EntityType,
    profileEntities: ImmutableList<ProfileEntity>,
    canNavigateBack: Boolean,
    isSigningInProgress: Boolean,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onCloseClick,
                backIconType = if (canNavigateBack) BackIconType.Back else BackIconType.Close,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(id = R.string.dAppRequest_personalDataPermission_continue),
                isLoading = isSigningInProgress
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues + PaddingValues(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Thumbnail.DApp(
                    modifier = Modifier.size(64.dp),
                    dapp = dapp
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    text = when (entityType) {
                        VerifyEntitiesViewModel.State.EntityType.Persona -> stringResource(
                            id = R.string.dAppRequest_personaProofOfOwnership_title
                        )
                        VerifyEntitiesViewModel.State.EntityType.Account -> stringResource(
                            id = R.string.dAppRequest_accountsProofOfOwnership_title
                        )
                    },
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = when (entityType) {
                        VerifyEntitiesViewModel.State.EntityType.Persona -> stringResource(
                            id = R.string.dAppRequest_personaProofOfOwnership_subtitle,
                            dapp.displayName()
                        )
                        VerifyEntitiesViewModel.State.EntityType.Account -> stringResource(
                            id = R.string.dAppRequest_accountsProofOfOwnership_subtitle,
                            dapp.displayName()
                        )
                    }.formattedSpans(boldStyle = SpanStyle(color = RadixTheme.colors.gray1, fontWeight = FontWeight.SemiBold)),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
            items(profileEntities) { entity ->
                when (entity) {
                    is ProfileEntity.PersonaEntity -> {
                        SimplePersonaCard(
                            modifier = Modifier.defaultCardShadow(elevation = 6.dp)
                                .background(
                                    brush = SolidColor(RadixTheme.colors.gray5),
                                    shape = RadixTheme.shapes.roundedRectMedium
                                )
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                .clip(RadixTheme.shapes.roundedRectMedium),
                            persona = entity.persona
                        )
                    }
                    is ProfileEntity.AccountEntity -> {
                        SimpleAccountCard(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    entity.account.appearanceId.gradient(),
                                    shape = RadixTheme.shapes.roundedRectSmall
                                ),
                            account = entity.account
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    }
                }
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun VerifyPersonaScreenPreview() {
    RadixWalletPreviewTheme {
        VerifyEntitiesContent(
            dapp = DApp.sampleMainnet(),
            entityType = VerifyEntitiesViewModel.State.EntityType.Persona,
            profileEntities = listOf(Persona.sampleMainnet.ripley.asProfileEntity()).toImmutableList(),
            canNavigateBack = false,
            isSigningInProgress = false,
            onContinueClick = {},
            onCloseClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun VerifyAccountsScreenPreview() {
    RadixWalletPreviewTheme {
        VerifyEntitiesContent(
            dapp = DApp.sampleMainnet(),
            entityType = VerifyEntitiesViewModel.State.EntityType.Account,
            profileEntities = Account.sampleMainnet.all.map {
                it.asProfileEntity()
            }.toImmutableList(),
            canNavigateBack = true,
            isSigningInProgress = true,
            onContinueClick = {},
            onCloseClick = {}
        )
    }
}

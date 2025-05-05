package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.SecurityShieldCardView
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceSecurityShieldCard
import com.babylon.wallet.android.presentation.ui.composables.card.mainShieldForDisplaySample
import com.babylon.wallet.android.presentation.ui.composables.card.otherShieldsForDisplaySample
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.SecurityStructureMetadata
import com.radixdlt.sargon.ShieldForDisplay
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityShieldsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityShieldsViewModel,
    onNavigateToSecurityShieldDetails: (securityShieldId: SecurityStructureId, securityShieldName: String) -> Unit,
    onCreateNewSecurityShieldClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SecurityShieldsContent(
        modifier = modifier,
        state = state,
        onChangeMainSecurityShieldClick = viewModel::onChangeMainSecurityShieldClick,
        onSecurityShieldClick = onNavigateToSecurityShieldDetails,
        onCreateNewSecurityShieldClick = onCreateNewSecurityShieldClick,
        onBackClick = onBackClick,
        onInfoClick = onInfoClick
    )

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SyncSheetState(
        sheetState = bottomSheetState,
        isSheetVisible = state.isMainSecurityShieldBottomSheetVisible,
        onSheetClosed = viewModel::onDismissMainSecurityShieldBottomSheet
    )

    if (state.isMainSecurityShieldBottomSheetVisible) {
        DefaultModalSheetLayout(
            enableImePadding = false,
            sheetState = bottomSheetState,
            sheetContent = {
                ChangeMainSecurityShieldContent(
                    otherSecurityShields = state.selectableOtherSecurityShieldIds,
                    isContinueButtonEnabled = state.isContinueButtonEnabled,
                    isChangingSecurityShieldInProgress = state.isChangingMainSecurityShieldInProgress,
                    onSecurityShieldSelect = viewModel::onSecurityShieldSelect,
                    onConfirmClick = viewModel::onConfirmChangeMainSecurityShield,
                    onDismissClick = viewModel::onDismissMainSecurityShieldBottomSheet
                )
            },
            onDismissRequest = viewModel::onDismissMainSecurityShieldBottomSheet
        )
    }
}

@Composable
fun SecurityShieldsContent(
    modifier: Modifier = Modifier,
    state: SecurityShieldsViewModel.State,
    onBackClick: () -> Unit,
    onChangeMainSecurityShieldClick: () -> Unit,
    onSecurityShieldClick: (SecurityStructureId, securityShieldName: String) -> Unit,
    onCreateNewSecurityShieldClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.securityShields_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner,
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        if (state.isLoading) {
            FullscreenCircularProgressContent()
        } else {
            Column(
                modifier = Modifier.padding(padding),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                SecurityShieldsList(
                    mainSecurityShield = state.mainSecurityShield,
                    otherSecurityShields = state.otherSecurityShields,
                    onChangeMainSecurityShieldClick = onChangeMainSecurityShieldClick,
                    onSecurityShieldClick = onSecurityShieldClick,
                    onCreateNewSecurityShieldClick = onCreateNewSecurityShieldClick,
                    onInfoClick = onInfoClick
                )
            }
        }
    }
}

@Composable
private fun SecurityShieldsList(
    modifier: Modifier = Modifier,
    mainSecurityShield: SecurityShieldCard?,
    otherSecurityShields: PersistentList<SecurityShieldCard>,
    onChangeMainSecurityShieldClick: () -> Unit,
    onSecurityShieldClick: (SecurityStructureId, securityShieldName: String) -> Unit,
    onCreateNewSecurityShieldClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        mainSecurityShield?.let { securityShieldCard ->
            item {
                if (otherSecurityShields.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.securityShields_default),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.textSecondary
                        )
                        Spacer(Modifier.weight(1f).fillMaxHeight())
                        RadixTextButton(
                            text = stringResource(R.string.securityShields_change),
                            onClick = onChangeMainSecurityShieldClick
                        )
                    }
                } else {
                    Text(
                        modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.securityShields_default),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.textSecondary
                    )
                }

                SecurityShieldCardView(
                    modifier = Modifier.clickable { onSecurityShieldClick(securityShieldCard.id, securityShieldCard.name) },
                    item = mainSecurityShield
                )
                if (otherSecurityShields.isEmpty()) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
        }

        if (otherSecurityShields.isNotEmpty()) {
            item {
                if (mainSecurityShield != null) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                    Text(
                        text = stringResource(id = R.string.securityShields_others),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.securityShields_others),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.textSecondary
                        )
                        Spacer(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        RadixTextButton(
                            text = stringResource(R.string.securityShields_change),
                            onClick = onChangeMainSecurityShieldClick
                        )
                    }
                }
            }
        }

        items(otherSecurityShields) { securityShieldCard ->
            SecurityShieldCardView(
                modifier = Modifier.clickable { onSecurityShieldClick(securityShieldCard.id, securityShieldCard.name) },
                item = securityShieldCard
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }

        item {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = stringResource(R.string.securityShields_createShieldButton),
                onClick = onCreateNewSecurityShieldClick,
                throttleClicks = true
            )

            InfoButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(align = Alignment.CenterHorizontally)
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingLarge
                    ),
                text = stringResource(R.string.infoLink_title_securityshields),
                onClick = {
                    onInfoClick(GlossaryItem.securityshields)
                }
            )
        }
    }
}

@Composable
private fun ChangeMainSecurityShieldContent(
    modifier: Modifier = Modifier,
    otherSecurityShields: ImmutableList<Selectable<SecurityShieldCard>>,
    isContinueButtonEnabled: Boolean,
    isChangingSecurityShieldInProgress: Boolean,
    onSecurityShieldSelect: (SecurityShieldCard) -> Unit,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.empty),
                windowInsets = WindowInsets(0.dp),
                backIconType = BackIconType.Close,
                containerColor = RadixTheme.colors.backgroundSecondary,
                onBackClick = onDismissClick,
            )
        },
        bottomBar = {
            RadixBottomBar(
                enabled = isContinueButtonEnabled,
                isLoading = isChangingSecurityShieldInProgress,
                onClick = onConfirmClick,
                text = stringResource(R.string.common_confirm),
                insets = WindowInsets(0.dp)
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            item {
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(R.string.securityShields_changeMain_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.securityShields_changeMain_subtitle),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }

            items(otherSecurityShields) {
                SelectableSingleChoiceSecurityShieldCard(
                    item = it,
                    onSelect = onSecurityShieldSelect
                )
            }

            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
        }
    }
}

@UsesSampleValues
@Composable
@Preview
private fun SecurityShieldsWithMainAndOthersPreviewLight() {
    RadixWalletPreviewTheme {
        SecurityShieldsContent(
            state = SecurityShieldsViewModel.State(
                isLoading = false,
                mainSecurityShield = mainShieldForDisplaySample,
                otherSecurityShields = otherShieldsForDisplaySample,
                isChangingMainSecurityShieldInProgress = false
            ),
            onBackClick = {},
            onChangeMainSecurityShieldClick = {},
            onSecurityShieldClick = { _, _ -> },
            onCreateNewSecurityShieldClick = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun SecurityShieldsWithMainAndOthersPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        SecurityShieldsContent(
            state = SecurityShieldsViewModel.State(
                isLoading = false,
                mainSecurityShield = mainShieldForDisplaySample,
                otherSecurityShields = otherShieldsForDisplaySample,
                isChangingMainSecurityShieldInProgress = false
            ),
            onBackClick = {},
            onChangeMainSecurityShieldClick = {},
            onSecurityShieldClick = { _, _ -> },
            onCreateNewSecurityShieldClick = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun SecurityShieldsWithMainPreviewLight() {
    RadixWalletPreviewTheme {
        SecurityShieldsContent(
            state = SecurityShieldsViewModel.State(
                isLoading = false,
                mainSecurityShield = SecurityShieldCard(
                    ShieldForDisplay(
                        metadata = SecurityStructureMetadata(
                            id = SecurityStructureId.randomUUID(),
                            displayName = DisplayName("XXX"),
                            createdOn = Timestamp.now(),
                            lastUpdatedOn = Timestamp.now(),
                            flags = emptyList()
                        ),
                        numberOfLinkedAccounts = 2.toUInt(),
                        numberOfLinkedHiddenAccounts = 3.toUInt(),
                        numberOfLinkedPersonas = 1.toUInt(),
                        numberOfLinkedHiddenPersonas = 0.toUInt()
                    ),
                    messages = persistentListOf()
                ),
                otherSecurityShields = persistentListOf(),
                isChangingMainSecurityShieldInProgress = false
            ),
            onBackClick = {},
            onChangeMainSecurityShieldClick = {},
            onSecurityShieldClick = { _, _ -> },
            onCreateNewSecurityShieldClick = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun SecurityShieldsWithMainPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        SecurityShieldsContent(
            state = SecurityShieldsViewModel.State(
                isLoading = false,
                mainSecurityShield = SecurityShieldCard(
                    ShieldForDisplay(
                        metadata = SecurityStructureMetadata(
                            id = SecurityStructureId.randomUUID(),
                            displayName = DisplayName("XXX"),
                            createdOn = Timestamp.now(),
                            lastUpdatedOn = Timestamp.now(),
                            flags = emptyList()
                        ),
                        numberOfLinkedAccounts = 2.toUInt(),
                        numberOfLinkedHiddenAccounts = 3.toUInt(),
                        numberOfLinkedPersonas = 1.toUInt(),
                        numberOfLinkedHiddenPersonas = 0.toUInt()
                    ),
                    messages = persistentListOf()
                ),
                otherSecurityShields = persistentListOf(),
                isChangingMainSecurityShieldInProgress = false
            ),
            onBackClick = {},
            onChangeMainSecurityShieldClick = {},
            onSecurityShieldClick = { _, _ -> },
            onCreateNewSecurityShieldClick = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun SecurityShieldsWithOthersPreview() {
    RadixWalletPreviewTheme {
        SecurityShieldsContent(
            state = SecurityShieldsViewModel.State(
                isLoading = false,
                mainSecurityShield = null,
                otherSecurityShields = otherShieldsForDisplaySample.subList(0, 3).toPersistentList(),
                isChangingMainSecurityShieldInProgress = false
            ),
            onBackClick = {},
            onChangeMainSecurityShieldClick = {},
            onSecurityShieldClick = { _, _ -> },
            onCreateNewSecurityShieldClick = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun SecurityShieldsWithOthersPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        SecurityShieldsContent(
            state = SecurityShieldsViewModel.State(
                isLoading = false,
                mainSecurityShield = null,
                otherSecurityShields = otherShieldsForDisplaySample.subList(0, 3).toPersistentList(),
                isChangingMainSecurityShieldInProgress = false
            ),
            onBackClick = {},
            onChangeMainSecurityShieldClick = {},
            onSecurityShieldClick = { _, _ -> },
            onCreateNewSecurityShieldClick = {},
            onInfoClick = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun ChangeMainSecurityShieldBottomSheetPreview() {
    RadixWalletPreviewTheme {
        ChangeMainSecurityShieldContent(
            otherSecurityShields = otherShieldsForDisplaySample.map { Selectable(it) }.toImmutableList(),
            isChangingSecurityShieldInProgress = false,
            isContinueButtonEnabled = false,
            onSecurityShieldSelect = {},
            onConfirmClick = {},
            onDismissClick = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun ChangeMainSecurityShieldBottomSheetPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        ChangeMainSecurityShieldContent(
            otherSecurityShields = otherShieldsForDisplaySample.map { Selectable(it) }.toImmutableList(),
            isChangingSecurityShieldInProgress = false,
            isContinueButtonEnabled = false,
            onSecurityShieldSelect = {},
            onConfirmClick = {},
            onDismissClick = {}
        )
    }
}

package com.babylon.wallet.android.presentation.settings.preferences.tokenpriceservices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow

@Composable
fun TokenPriceServicesScreen(
    viewModel: TokenPriceServicesViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TokenPriceServicesContent(
        state = state,
        onBackClick = onBackClick,
        onDismissErrorMessage = viewModel::onDismissErrorMessage,
        onAddClick = { viewModel.setAddSheetVisible(true) },
        onDeleteItemClick = viewModel::onDeleteItemClick,
        onDeleteConfirmationDismissed = viewModel::onDeleteConfirmationDismissed
    )

    state.addInput?.let { input ->
        AddTokenPriceServiceSheet(
            input = input,
            onAddClick = viewModel::onAddConfirmed,
            onUrlChanged = viewModel::onNewUrlChanged,
            onDismiss = { viewModel.setAddSheetVisible(false) }
        )
    }
}

@Composable
private fun TokenPriceServicesContent(
    state: TokenPriceServicesViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onDismissErrorMessage: () -> Unit,
    onDeleteItemClick: (TokenPriceServicesViewModel.State.UiItem) -> Unit,
    onDeleteConfirmationDismissed: (Boolean) -> Unit
) {
    state.errorMessage?.let {
        ErrorAlertDialog(
            errorMessage = it,
            cancel = onDismissErrorMessage
        )
    }

    state.itemToDelete?.let {
        BasicPromptAlertDialog(
            finish = onDeleteConfirmationDismissed,
            titleText = stringResource(id = R.string.tokenPriceServices_deleteAlertTitle),
            messageText = stringResource(id = R.string.tokenPriceServices_deleteAlertMessage),
            confirmText = stringResource(id = R.string.common_remove),
            confirmTextColor = RadixTheme.colors.error
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.tokenPriceServices_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner,
                    actions = {
                        IconButton(onClick = onAddClick) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(id = R.string.tokenPriceServices_add_title),
                                tint = RadixTheme.colors.icon
                            )
                        }
                    }
                )
                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.tokenPriceServices_subtitle),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.textSecondary
                )
            }

            if (state.items.isEmpty()) {
                item {
                    TokenPriceServicesEmptyState()
                }
            } else {
                items(state.items) { item ->
                    TokenPriceServiceView(
                        item = item,
                        showDelete = state.items.size > 1,
                        onDeleteClick = { onDeleteItemClick(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingsEntrySurface()
                            .padding(
                                start = RadixTheme.dimensions.paddingDefault,
                                end = RadixTheme.dimensions.paddingSmall,
                                top = RadixTheme.dimensions.paddingDefault,
                                bottom = RadixTheme.dimensions.paddingDefault
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenPriceServicesEmptyState() {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .settingsEntrySurface()
            .padding(RadixTheme.dimensions.paddingDefault),
        text = stringResource(id = R.string.tokenPriceServices_emptyState),
        style = RadixTheme.typography.body1HighImportance,
        color = RadixTheme.colors.textSecondary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun TokenPriceServiceView(
    item: TokenPriceServicesViewModel.State.UiItem,
    showDelete: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = item.tokenPriceService.baseUrl.toString(),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (showDelete) {
            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline),
                    tint = RadixTheme.colors.icon,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun Modifier.settingsEntrySurface(): Modifier {
    val shape = RadixTheme.shapes.roundedRectMedium

    return this
        .defaultCardShadow(shape = shape)
        .clip(shape)
        .background(color = RadixTheme.colors.card, shape = shape)
}

@Composable
private fun AddTokenPriceServiceSheet(
    input: TokenPriceServicesViewModel.State.AddInput,
    onAddClick: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    BottomSheetDialogWrapper(
        addScrim = true,
        showDragHandle = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.tokenPriceServices_add_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.tokenPriceServices_add_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                    .focusRequester(inputFocusRequester),
                onValueChanged = onUrlChanged,
                value = input.url,
                hint = stringResource(id = R.string.tokenPriceServices_add_placeholder),
                singleLine = true,
                error = when (input.failure) {
                    TokenPriceServicesViewModel.State.AddInput.Failure.AlreadyExist ->
                        stringResource(id = R.string.tokenPriceServices_add_errorDuplicateUrl)

                    TokenPriceServicesViewModel.State.AddInput.Failure.ErrorWhileAdding ->
                        stringResource(id = R.string.tokenPriceServices_add_errorAddFailed)

                    null -> null
                }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
        }

        RadixBottomBar(
            onClick = onAddClick,
            text = stringResource(id = R.string.tokenPriceServices_add_confirmButton),
            enabled = input.isUrlValid,
            isLoading = input.isLoading,
            insets = WindowInsets.navigationBars.union(WindowInsets.ime)
        )
    }
}

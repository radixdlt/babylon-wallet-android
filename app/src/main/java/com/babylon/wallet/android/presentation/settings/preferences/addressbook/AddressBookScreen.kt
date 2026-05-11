package com.babylon.wallet.android.presentation.settings.preferences.addressbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressBookEntry

@Composable
fun AddressBookScreen(
    viewModel: AddressBookViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAppear()
    }

    AddressBookContent(
        state = state,
        onBackClick = onBackClick,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirmationDismissed = viewModel::onDeleteConfirmationDismissed,
        onDismissErrorMessage = viewModel::onDismissErrorMessage,
        onDismissOwnAccountAddressAlert = viewModel::onDismissOwnAccountAddressAlert,
        onFormDismissed = viewModel::onFormDismissed,
        onFormAddressChanged = viewModel::onFormAddressChanged,
        onFormNameChanged = viewModel::onFormNameChanged,
        onFormNoteChanged = viewModel::onFormNoteChanged,
        onFormSaveClick = viewModel::onFormSaveClick
    )
}

@Composable
private fun AddressBookContent(
    state: AddressBookViewModel.State,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (AddressBookEntry) -> Unit,
    onDeleteClick: (AddressBookEntry) -> Unit,
    onDeleteConfirmationDismissed: (Boolean) -> Unit,
    onDismissErrorMessage: () -> Unit,
    onDismissOwnAccountAddressAlert: () -> Unit,
    onFormDismissed: () -> Unit,
    onFormAddressChanged: (String) -> Unit,
    onFormNameChanged: (String) -> Unit,
    onFormNoteChanged: (String) -> Unit,
    onFormSaveClick: () -> Unit
) {
    state.errorMessage?.let {
        ErrorAlertDialog(
            errorMessage = it,
            cancel = onDismissErrorMessage
        )
    }

    if (state.showOwnAccountAddressAlert) {
        BasicPromptAlertDialog(
            finish = { onDismissOwnAccountAddressAlert() },
            titleText = stringResource(id = R.string.addressBook_entryForm_ownAccountAlertTitle),
            messageText = stringResource(id = R.string.addressBook_entryForm_ownAccountAlertMessage),
            confirmText = stringResource(id = R.string.common_ok),
            dismissText = null
        )
    }

    state.entryToDelete?.let {
        BasicPromptAlertDialog(
            finish = onDeleteConfirmationDismissed,
            titleText = stringResource(id = R.string.addressBook_deleteAlertTitle),
            messageText = stringResource(id = R.string.addressBook_deleteAlertMessage),
            confirmText = stringResource(id = R.string.addressBook_delete),
            dismissText = stringResource(id = R.string.common_cancel),
            confirmTextColor = RadixTheme.colors.error
        )
    }

    state.formInput?.let {
        AddressBookEntryFormSheet(
            state = it.toAddressBookEntryFormUiState(),
            onDismiss = onFormDismissed,
            onAddressChanged = onFormAddressChanged,
            onNameChanged = onFormNameChanged,
            onNoteChanged = onFormNoteChanged,
            onSaveClick = onFormSaveClick
        )
    }

    Scaffold(
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.addressBook_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner,
                    actions = {
                        IconButton(onClick = onAddClick) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(id = R.string.addressBook_entryForm_addTitle),
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
                    text = stringResource(id = R.string.addressBook_subtitle),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.textSecondary
                )
            }

            if (state.entries.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(state.entries) { entry ->
                    AddressBookEntryRow(
                        entry = entry,
                        onEditClick = { onEditClick(entry) },
                        onDeleteClick = { onDeleteClick(entry) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .addressBookEntrySurface()
                            .padding(RadixTheme.dimensions.paddingDefault)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .addressBookEntrySurface()
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.addressBook_emptyState),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun AddressBookEntryRow(
    entry: AddressBookEntry,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    text = entry.name.value,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ActionableAddressView(
                    address = entry.address,
                    textStyle = RadixTheme.typography.body2Regular,
                    textColor = RadixTheme.colors.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(id = R.string.addressBook_edit),
                        tint = RadixTheme.colors.icon
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(id = R.string.addressBook_delete),
                        tint = RadixTheme.colors.icon
                    )
                }
            }
        }

        entry.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Modifier.addressBookEntrySurface(): Modifier {
    val shape = RadixTheme.shapes.roundedRectMedium

    return this
        .defaultCardShadow(shape = shape)
        .clip(shape)
        .background(color = RadixTheme.colors.card, shape = shape)
}

private fun AddressBookViewModel.State.FormInput.toAddressBookEntryFormUiState() = AddressBookEntryFormUiState(
    titleMode = if (isEditing) {
        AddressBookEntryFormUiState.TitleMode.Edit
    } else {
        AddressBookEntryFormUiState.TitleMode.Add
    },
    address = address,
    addressToShow = addressToSave,
    isAddressEditable = isAddressEditable,
    isAddressScannerEnabled = true,
    hasAddressError = hasAddressError,
    name = name,
    note = note,
    isValid = isValid,
    isSaving = isSaving
)

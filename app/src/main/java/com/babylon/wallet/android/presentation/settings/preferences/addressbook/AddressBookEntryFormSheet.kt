package com.babylon.wallet.android.presentation.settings.preferences.addressbook

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transfer.accounts.ScanQRContent
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.radixdlt.sargon.Address
import kotlinx.coroutines.launch

data class AddressBookEntryFormUiState(
    val titleMode: TitleMode,
    val address: String,
    val addressToShow: Address? = null,
    val isAddressEditable: Boolean,
    val isAddressScannerEnabled: Boolean = true,
    val hasAddressError: Boolean = false,
    val name: String,
    val note: String,
    val isValid: Boolean,
    val isSaving: Boolean
) {
    enum class TitleMode {
        Add,
        Edit
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddressBookEntryFormSheet(
    state: AddressBookEntryFormUiState,
    onDismiss: () -> Unit,
    onAddressChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var mode by remember { mutableStateOf(FormMode.Form) }
    val dismissSheet: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            sheetState.show()
        }
    }

    DefaultModalSheetLayout(
        sheetState = sheetState,
        heightFraction = 0.9f,
        enableImePadding = true,
        showDragHandle = true,
        onDismissRequest = dismissSheet,
        sheetContent = {
            Scaffold(
                containerColor = RadixTheme.colors.background,
                topBar = {
                    RadixCenteredTopAppBar(
                        title = stringResource(
                            id = if (mode == FormMode.Scanner) {
                                R.string.assetTransfer_chooseReceivingAccount_scanQRNavigationTitle
                            } else {
                                when (state.titleMode) {
                                    AddressBookEntryFormUiState.TitleMode.Edit -> R.string.addressBook_entryForm_editTitle
                                    AddressBookEntryFormUiState.TitleMode.Add -> R.string.addressBook_entryForm_addTitle
                                }
                            }
                        ),
                        onBackClick = {
                            if (mode == FormMode.Scanner) {
                                mode = FormMode.Form
                            } else {
                                dismissSheet()
                            }
                        },
                        backIconType = if (mode == FormMode.Scanner) {
                            BackIconType.Back
                        } else {
                            BackIconType.Close
                        },
                        windowInsets = WindowInsets(0.dp)
                    )
                },
                bottomBar = {
                    if (mode == FormMode.Form) {
                        RadixBottomBar(
                            onClick = onSaveClick,
                            text = stringResource(id = R.string.common_save),
                            enabled = state.isValid,
                            isLoading = state.isSaving,
                            insets = WindowInsets.navigationBars
                        )
                    }
                }
            ) { padding ->
                if (mode == FormMode.Scanner) {
                    if (cameraPermissionState.status.isGranted) {
                        ScanQRContent(
                            modifier = Modifier
                                .background(color = RadixTheme.colors.background)
                                .padding(padding),
                            onQRDecoded = { scanned ->
                                onAddressChanged(scanned.replaceFirst(RNS_HRP, ""))
                                mode = FormMode.Form
                            }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(padding)
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                    ) {
                        if (state.isAddressEditable) {
                            RadixTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = state.address,
                                onValueChanged = onAddressChanged,
                                hint = stringResource(id = R.string.addressBook_entryForm_addressPlaceholder),
                                singleLine = true,
                                error = if (state.hasAddressError) {
                                    stringResource(id = R.string.addressBook_entryForm_invalidAddress)
                                } else {
                                    null
                                },
                                trailingIcon = if (state.isAddressScannerEnabled) {
                                    {
                                        IconButton(
                                            onClick = {
                                                cameraPermissionState.launchPermissionRequest()
                                                mode = FormMode.Scanner
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
                                                ),
                                                contentDescription = null,
                                                tint = RadixTheme.colors.icon
                                            )
                                        }
                                    }
                                } else {
                                    null
                                }
                            )
                        } else {
                            state.addressToShow?.let { address ->
                                ActionableAddressView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                    address = address,
                                    textStyle = RadixTheme.typography.body2Regular,
                                    textColor = RadixTheme.colors.textSecondary
                                )
                            }
                        }

                        RadixTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.name,
                            onValueChanged = onNameChanged,
                            hint = stringResource(id = R.string.addressBook_entryForm_namePlaceholder),
                            singleLine = true
                        )

                        RadixTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.note,
                            onValueChanged = onNoteChanged,
                            hint = stringResource(id = R.string.addressBook_entryForm_notePlaceholder),
                            singleLine = false
                        )
                    }
                }
            }
        }
    )
}

private enum class FormMode {
    Form,
    Scanner
}

private const val RNS_HRP = "rns:"

package com.babylon.wallet.android.presentation.status.address

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.status.address.AddressDetailsDialogViewModel.State.Section
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.flow.Flow
import rdx.works.core.qr.QRCodeGenerator

@Composable
fun AddressDetailsDialog(
    viewModel: AddressDetailsDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HandleEvents(events = viewModel.oneOffEvent)

    AddressDetailsDialogContent(
        state = state,
        onCopy = viewModel::onCopyClick,
        onEnlarge = viewModel::onEnlargeClick,
        onShare = viewModel::onShareClick,
        onVisitDashboard = viewModel::onVisitDashboardClick,
        onVerifyAddressOnLedger = viewModel::onVerifyOnLedgerDeviceClick,
        onDismiss = onDismiss
    )
}

@Composable
private fun AddressDetailsDialogContent(
    modifier: Modifier = Modifier,
    state: AddressDetailsDialogViewModel.State,
    onCopy: () -> Unit,
    onEnlarge: () -> Unit,
    onShare: () -> Unit,
    onVisitDashboard: () -> Unit,
    onVerifyAddressOnLedger: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    BottomSheetDialogWrapper(
        modifier = modifier.navigationBarsPadding(),
        onDismiss = onDismiss,
        showDragHandle = true,
        content = {
            Column {
                if (state.title != null) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                        text = state.title,
                        textAlign = TextAlign.Center,
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.gray1
                    )
                }

                state.sections.forEach { section ->
                    when (section) {
                        is Section.AccountAddressQRCode -> AccountAddressQRCode(accountAddress = section.accountAddress)
                        is Section.FullAddress -> FullAddress(
                            fullAddress = section.fullAddress,
                            boldRanges = section.boldRanges,
                            onCopy = onCopy,
                            onEnlarge = onEnlarge,
                            onShare = onShare
                        )

                        is Section.VisitDashboard -> VisitDashboard(onClick = onVisitDashboard)
                        is Section.VerifyAddressOnLedger -> VerifyAddressOnLedger(onClick = onVerifyAddressOnLedger)
                    }
                }
                Spacer(modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault))
            }

        }
    )
}

@Composable
private fun AccountAddressQRCode(
    modifier: Modifier = Modifier,
    accountAddress: AccountAddress
) {
    val qrCodeBitmap = remember(accountAddress) {
        QRCodeGenerator.forAccount(address = accountAddress)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RadixTheme.dimensions.paddingSmall),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
            text = stringResource(id = R.string.addressDetails_qrCode),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        AsyncImage(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge)
                .aspectRatio(ratio = 1f),
            model = qrCodeBitmap,
            contentDescription = null
        )
    }
}

@Composable
fun FullAddress(
    modifier: Modifier = Modifier,
    fullAddress: String,
    boldRanges: List<OpenEndRange<Int>>,
    onCopy: () -> Unit,
    onEnlarge: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault)
            .background(
                color = RadixTheme.colors.gray5,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingLarge
            ),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingLarge)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.addressDetails_fullAddress),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = buildAnnotatedString {
                append(fullAddress)

                boldRanges.forEach { range ->
                    addStyle(
                        style = SpanStyle(color = RadixTheme.colors.gray1),
                        start = range.start,
                        end = range.endExclusive
                    )
                }

            },
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray2,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCopy
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
                Text(text = stringResource(id = R.string.addressDetails_copy))
            }

            TextButton(
                onClick = onEnlarge
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_enlarge),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
                Text(text = stringResource(id = R.string.addressDetails_enlarge))
            }

            TextButton(
                onClick = onShare
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_share),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
                Text(text = stringResource(id = R.string.addressDetails_share))
            }
        }
    }
}

@Composable
fun VisitDashboard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    RadixSecondaryButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        text = stringResource(id = R.string.addressDetails_viewOnDashboard),
        trailingContent = {
            Icon(
                modifier = Modifier.size(12.dp),
                painter = painterResource(id = R.drawable.ic_external_link),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        },
        onClick = onClick
    )
}

@Composable
fun VerifyAddressOnLedger(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    RadixSecondaryButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        text = stringResource(id = R.string.addressDetails_verifyOnLedger),
        onClick = onClick
    )
}


@Composable
private fun HandleEvents(events: Flow<AddressDetailsDialogViewModel.Event>) {
    LaunchedEffect(key1 = events) {
        events.collect { event ->
            when (event) {
                is AddressDetailsDialogViewModel.Event.CloseEnlarged -> TODO()
                is AddressDetailsDialogViewModel.Event.PerformEnlarge -> TODO()
                is AddressDetailsDialogViewModel.Event.PerformCopy -> TODO()
                is AddressDetailsDialogViewModel.Event.PerformShare -> TODO()
                is AddressDetailsDialogViewModel.Event.PerformVisitDashBoard -> TODO()
                is AddressDetailsDialogViewModel.Event.ShowLedgerVerificationResult -> TODO()
            }
        }
    }
}

@UsesSampleValues
@Composable
@Preview
fun AddressDetailsDialogContentAccountAddressPreview() {
    RadixWalletPreviewTheme {
        val address = remember {
            AccountAddress.sampleMainnet()
        }
        val actionableAddress = ActionableAddress.Address(
            address = Address.Account(v1 = address),
            isVisitableInDashboard = true
        )
        AddressDetailsDialogContent(
            state = AddressDetailsDialogViewModel.State(
                actionableAddress = actionableAddress,
                title = "My Main Account",
                sections = listOf(
                    Section.AccountAddressQRCode(accountAddress = address),
                    Section.FullAddress(
                        fullAddress = address.formatted(format = AddressFormat.FULL),
                        truncatedAddress = address.formatted(format = AddressFormat.DEFAULT)
                    ),
                    Section.VisitDashboard(url = actionableAddress.dashboardUrl().orEmpty()),
                    Section.VerifyAddressOnLedger(accountAddress = address)
                )
            ),
            onCopy = {},
            onEnlarge = {},
            onShare = {},
            onVisitDashboard = {},
            onVerifyAddressOnLedger = {},
            onDismiss = { }
        )
    }
}

@UsesSampleValues
@Composable
@Preview
fun AddressDetailsDialogContentResourceAddressPreview() {
    RadixWalletPreviewTheme {
        val address = remember {
            ResourceAddress.sampleMainnet()
        }
        val actionableAddress = ActionableAddress.Address(
            address = Address.Resource(v1 = address),
            isVisitableInDashboard = true
        )
        AddressDetailsDialogContent(
            state = AddressDetailsDialogViewModel.State(
                actionableAddress = actionableAddress,
                title = "Radix (XRD)",
                sections = listOf(
                    Section.FullAddress(
                        fullAddress = address.formatted(format = AddressFormat.FULL),
                        truncatedAddress = address.formatted(format = AddressFormat.DEFAULT)
                    ),
                    Section.VisitDashboard(url = actionableAddress.dashboardUrl().orEmpty()),
                )
            ),
            onCopy = {},
            onEnlarge = {},
            onShare = {},
            onVisitDashboard = {},
            onVerifyAddressOnLedger = {},
            onDismiss = { }
        )
    }
}

@UsesSampleValues
@Composable
@Preview
fun AddressDetailsDialogContentGlobalIdPreview() {
    RadixWalletPreviewTheme {
        val address = remember {
            NonFungibleGlobalId.sample()
        }
        val actionableAddress = ActionableAddress.GlobalId(
            address = address,
            isVisitableInDashboard = true
        )
        AddressDetailsDialogContent(
            state = AddressDetailsDialogViewModel.State(
                actionableAddress = actionableAddress,
                title = "NFT Collection",
                sections = listOf(
                    Section.FullAddress(
                        fullAddress = address.formatted(format = AddressFormat.FULL),
                        truncatedAddress = address.formatted(format = AddressFormat.DEFAULT)
                    ),
                    Section.VisitDashboard(url = actionableAddress.dashboardUrl()),
                )
            ),
            onCopy = {},
            onEnlarge = {},
            onShare = {},
            onVisitDashboard = {},
            onVerifyAddressOnLedger = {},
            onDismiss = { }
        )
    }
}

@UsesSampleValues
@Composable
@Preview
fun AddressDetailsDialogContentRandomComponentAddressPreview() {
    RadixWalletPreviewTheme {
        val address = remember {
            ComponentAddress.sampleMainnet()
        }
        val actionableAddress = ActionableAddress.Address(
            address = Address.Component(address),
            isVisitableInDashboard = true
        )
        AddressDetailsDialogContent(
            state = AddressDetailsDialogViewModel.State(
                actionableAddress = actionableAddress,
                title = null,
                sections = listOf(
                    Section.FullAddress(
                        fullAddress = address.formatted(format = AddressFormat.FULL),
                        truncatedAddress = address.formatted(format = AddressFormat.DEFAULT)
                    ),
                    Section.VisitDashboard(url = actionableAddress.dashboardUrl().orEmpty()),
                )
            ),
            onCopy = {},
            onEnlarge = {},
            onShare = {},
            onVisitDashboard = {},
            onVerifyAddressOnLedger = {},
            onDismiss = { }
        )
    }
}
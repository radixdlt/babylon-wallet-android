@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DappDetails
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.LinkText
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataFieldRow
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataStringField
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.WarningButton
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet
import rdx.works.core.sargon.fields
import java.util.Locale

@Composable
fun DappDetailScreen(
    viewModel: DappDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditPersona: (IdentityAddress, RequiredPersonaFields?) -> Unit,
    onFungibleClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                DappDetailEvent.DappDeleted -> onBackClick()
                is DappDetailEvent.EditPersona -> {
                    onEditPersona(it.personaAddress, it.requiredPersonaFields)
                }

                is DappDetailEvent.OnFungibleClick -> onFungibleClick(it.resource)
                is DappDetailEvent.OnNonFungibleClick -> onNonFungibleClick(it.resource)
            }
        }
    }
    DappDetailContent(
        onBackClick = onBackClick,
        modifier = modifier.fillMaxSize(),
        state = state,
        onPersonaClick = viewModel::onPersonaClick,
        onFungibleTokenClick = viewModel::onFungibleTokenClick,
        onNftClick = viewModel::onNftClick,
        onDisconnectPersona = viewModel::onDisconnectPersona,
        personaDetailsClosed = viewModel::onPersonaDetailsClosed,
        onDeleteDapp = viewModel::onDeleteDapp,
        onEditPersona = viewModel::onEditPersona,
        onEditAccountSharing = viewModel::onEditAccountSharing,
        hidePersonaBottomSheet = viewModel::hidePersonaBottomSheet,
        onShowLockerDepositsCheckedChange = viewModel::onShowLockerDepositsCheckedChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DappDetailContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: DappDetailUiState,
    onPersonaClick: (Persona) -> Unit,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNftClick: (Resource.NonFungibleResource) -> Unit,
    onDisconnectPersona: (Persona) -> Unit,
    personaDetailsClosed: () -> Unit,
    onDeleteDapp: () -> Unit,
    onEditPersona: () -> Unit,
    onEditAccountSharing: () -> Unit,
    hidePersonaBottomSheet: () -> Unit,
    onShowLockerDepositsCheckedChange: (Boolean) -> Unit
) {
    var showDeleteDappPrompt by remember { mutableStateOf(false) }
    val bottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(bottomSheetState) {
        snapshotFlow {
            bottomSheetState.isVisible
        }.filter { !it }.distinctUntilChanged().collect {
            personaDetailsClosed()
        }
    }
    BackHandler(enabled = bottomSheetState.isVisible) {
        hidePersonaBottomSheet()
        scope.launch {
            bottomSheetState.hide()
        }
    }

    if (showDeleteDappPrompt) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    onDeleteDapp()
                }
                showDeleteDappPrompt = false
            },
            title = {
                Text(
                    text = stringResource(id = R.string.authorizedDapps_forgetDappAlert_title),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.authorizedDapps_forgetDappAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.authorizedDapps_forgetDappAlert_forget)
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = state.dAppWithResources?.dApp?.name.orEmpty().ifEmpty {
                        stringResource(id = R.string.dAppRequest_metadata_unknownName)
                    },
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.gray5)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .background(RadixTheme.colors.defaultBackground)
                .padding(padding)
        ) {
            DappDetails(
                modifier = Modifier.fillMaxSize(),
                dAppWithResources = state.dAppWithResources,
                isValidatingWebsite = state.isValidatingWebsite,
                validatedWebsite = state.validatedWebsite,
                personaList = state.authorizedPersonas,
                isShowLockerDepositsChecked = state.isShowLockerDepositsChecked,
                isReadOnly = false,
                onPersonaClick = { persona ->
                    onPersonaClick(persona)
                    scope.launch {
                        bottomSheetState.show()
                    }
                },
                onFungibleTokenClick = { fungibleResource ->
                    onFungibleTokenClick(fungibleResource)
                },
                onNonFungibleClick = { nftItem ->
                    onNftClick(nftItem)
                },
                onDeleteDapp = {
                    showDeleteDappPrompt = true
                },
                onShowLockerDepositsCheckedChange = onShowLockerDepositsCheckedChange
            )

            if (state.loading) {
                FullscreenCircularProgressContent()
            }
        }
    }

    if (state.isBottomSheetVisible) {
        DefaultModalSheetLayout(
            sheetState = bottomSheetState,
            sheetContent = {
                when (state.selectedSheetState) {
                    is SelectedSheetState.SelectedPersona -> {
                        state.selectedSheetState.persona?.let {
                            PersonaDetailsSheet(
                                persona = it,
                                sharedPersonaAccounts = state.sharedPersonaAccounts,
                                onCloseClick = {
                                    hidePersonaBottomSheet()
                                    scope.launch {
                                        bottomSheetState.hide()
                                    }
                                },
                                dappName = state.dAppWithResources?.dApp?.name.orEmpty(),
                                onDisconnectPersona = { persona ->
                                    hidePersonaBottomSheet()
                                    scope.launch {
                                        bottomSheetState.hide()
                                    }
                                    onDisconnectPersona(persona)
                                },
                                onEditPersona = onEditPersona,
                                onEditAccountSharing = onEditAccountSharing
                            )
                        }
                    }

                    else -> {}
                }
            },
            onDismissRequest = {
                hidePersonaBottomSheet()
                scope.launch {
                    bottomSheetState.hide()
                }
            }
        )
    }
}

@Composable
fun DappDefinitionAddressRow(
    dappDefinitionAddress: AccountAddress,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.authorizedDapps_dAppDetails_dAppDefinition),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )

        ActionableAddressView(
            address = Address.Account(dappDefinitionAddress),
            textStyle = RadixTheme.typography.body1HighImportance,
            textColor = RadixTheme.colors.gray1,
            iconColor = RadixTheme.colors.gray2
        )
    }
}

@Composable
fun DAppWebsiteAddressRow(
    modifier: Modifier = Modifier,
    website: String?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensions.paddingSmall)
    ) {
        Text(
            text = stringResource(id = R.string.authorizedDapps_dAppDetails_website).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )

        LinkText(
            modifier = Modifier
                .fillMaxWidth()
                .radixPlaceholder(visible = website == null),
            clickable = website != null,
            url = website.orEmpty(),
            linkIconColor = RadixTheme.colors.gray3
        )
    }
}

@Composable
private fun PersonaDetailsSheet(
    persona: PersonaUiModel,
    sharedPersonaAccounts: ImmutableList<AccountItemUiModel>,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    dappName: String,
    onDisconnectPersona: (Persona) -> Unit,
    onEditPersona: () -> Unit,
    onEditAccountSharing: () -> Unit
) {
    var personaToDisconnect by remember { mutableStateOf<Persona?>(null) }
    Box(modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
            RadixCenteredTopAppBar(
                title = persona.persona.displayName.value,
                onBackClick = onCloseClick,
                contentColor = RadixTheme.colors.gray1,
                backIconType = BackIconType.Close
            )
            HorizontalDivider(color = RadixTheme.colors.gray5)
            PersonaDetailList(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                persona = persona,
                onEditPersona = onEditPersona,
                sharedPersonaAccounts = sharedPersonaAccounts,
                dappName = dappName,
                onDisconnectPersona = {
                    personaToDisconnect = it
                },
                onEditAccountSharing = onEditAccountSharing
            )
        }
    }
    if (personaToDisconnect != null) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    personaToDisconnect?.let { persona -> onDisconnectPersona(persona) }
                }
                personaToDisconnect = null
            },
            title = {
                Text(
                    text = stringResource(id = R.string.authorizedDapps_removeAuthorizationAlert_title),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.authorizedDapps_removeAuthorizationAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.authorizedDapps_removeAuthorizationAlert_confirm)
        )
    }
}

@Composable
private fun PersonaDetailList(
    modifier: Modifier = Modifier,
    persona: PersonaUiModel,
    onEditPersona: () -> Unit,
    sharedPersonaAccounts: ImmutableList<AccountItemUiModel>,
    dappName: String,
    onDisconnectPersona: (Persona) -> Unit,
    onEditAccountSharing: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        item {
            Thumbnail.Persona(
                modifier = Modifier
                    .padding(vertical = dimensions.paddingDefault)
                    .size(104.dp),
                persona = persona.persona
            )
        }
        item {
            PersonaDataStringField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault),
                label = stringResource(id = R.string.authorizedDapps_personaDetails_personaLabelHeading),
                value = persona.persona.displayName.value
            )
            Spacer(modifier = Modifier.height(dimensions.paddingXXLarge))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = dimensions.paddingDefault)
            )
        }
        val nonEmptyPersonaFields = persona.persona.personaData.fields
        if (nonEmptyPersonaFields.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    text = stringResource(
                        R.string.authorizedDapps_personaDetails_personalDataSharingDescription,
                        dappName
                    ),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(dimensions.paddingLarge))
            }
            items(nonEmptyPersonaFields) { field ->
                PersonaDataFieldRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    field = field.value
                )
                Spacer(modifier = Modifier.height(dimensions.paddingLarge))
            }
        }
        item {
            Spacer(modifier = Modifier.height(dimensions.paddingSmall))
            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                text = stringResource(R.string.authorizedDapps_personaDetails_editPersona),
                onClick = onEditPersona
            )
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
        }
        if (sharedPersonaAccounts.isNotEmpty()) {
            item {
                GrayBackgroundWrapper(Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.authorizedDapps_personaDetails_accountSharingDescription, dappName),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2,
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
            }
            items(sharedPersonaAccounts) { account ->
                GrayBackgroundWrapper(Modifier.fillMaxWidth()) {
                    SimpleAccountCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                account.appearanceID.gradient(),
                                RadixTheme.shapes.roundedRectSmall
                            )
                            .padding(
                                horizontal = dimensions.paddingLarge,
                                vertical = dimensions.paddingDefault
                            ),
                        account = account
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
            }
            item {
                GrayBackgroundWrapper(Modifier.fillMaxWidth()) {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions.paddingLarge),
                        text = stringResource(
                            R.string.authorizedDapps_personaDetails_editAccountSharing
                        ),
                        onClick = onEditAccountSharing
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
            }
        }
        item {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            WarningButton(
                modifier = Modifier.padding(horizontal = dimensions.paddingDefault),
                text = stringResource(id = R.string.authorizedDapps_personaDetails_removeAuthorization),
                onClick = {
                    onDisconnectPersona(persona.persona)
                }
            )
        }
    }
}

@UsesSampleValues
class PreviewValuesProvider : PreviewParameterProvider<Pair<DAppWithResources, String?>> {

    val sampleDapp = DApp.sampleMainnet()

    override val values: Sequence<Pair<DAppWithResources, String?>>
        get() = sequenceOf(
            DAppWithResources(
                dApp = sampleDapp,
                fungibleResources = listOf(Resource.FungibleResource.sampleMainnet(), Resource.FungibleResource.sampleMainnet()),
                nonFungibleResources = listOf(
                    Resource.NonFungibleResource.sampleMainnet(),
                    Resource.NonFungibleResource.sampleMainnet()
                )
            ) to "https://example.com",
            DAppWithResources(
                dApp = sampleDapp,
                nonFungibleResources = listOf(
                    Resource.NonFungibleResource.sampleMainnet(),
                    Resource.NonFungibleResource.sampleMainnet()
                )
            ) to null,
            DAppWithResources(
                dApp = sampleDapp,
            ) to "https://example.com",
            DAppWithResources(
                dApp = sampleDapp.copy(metadata = sampleDapp.metadata.filter { it.key != ExplicitMetadataKey.CLAIMED_WEBSITES.key })
            ) to null,
        )
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun DappDetailContentPreview(
    @PreviewParameter(PreviewValuesProvider::class) previewValues: Pair<DAppWithResources, String?>
) {
    RadixWalletPreviewTheme {
        DappDetailContent(
            onBackClick = {},
            state = DappDetailUiState(
                loading = false,
                authorizedPersonas = persistentListOf(Persona.sampleMainnet(), Persona.sampleMainnet.ripley),
                dAppWithResources = previewValues.first,
                sharedPersonaAccounts = persistentListOf(
                    AccountItemUiModel(AccountAddress.sampleMainnet.random(), "Account1", AppearanceId(0u))
                ),
                selectedSheetState = null,
                validatedWebsite = previewValues.second
            ),
            onPersonaClick = {},
            onFungibleTokenClick = {},
            onNftClick = {},
            onDisconnectPersona = {},
            personaDetailsClosed = {},
            onDeleteDapp = {},
            onEditPersona = {},
            onEditAccountSharing = {},
            hidePersonaBottomSheet = {},
            onShowLockerDepositsCheckedChange = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PersonaDetailsSheetPreview() {
    RadixWalletTheme {
        PersonaDetailsSheet(
            persona = PersonaUiModel(Persona.sampleMainnet()),
            sharedPersonaAccounts = persistentListOf(
                AccountItemUiModel(AccountAddress.sampleMainnet.random(), "Account1", AppearanceId(0u))
            ),
            onCloseClick = {},
            dappName = "dApp",
            onDisconnectPersona = {},
            onEditPersona = {}
        ) {}
    }
}

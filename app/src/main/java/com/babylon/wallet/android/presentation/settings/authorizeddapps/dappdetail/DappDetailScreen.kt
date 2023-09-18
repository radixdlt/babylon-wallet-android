@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.metadata.ClaimedWebsitesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.account.composable.FungibleTokenBottomSheetDetails
import com.babylon.wallet.android.presentation.account.composable.NonFungibleTokenBottomSheetDetails
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataFieldRow
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataStringField
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.card.FungibleCard
import com.babylon.wallet.android.presentation.ui.composables.card.NonFungibleCard
import com.babylon.wallet.android.presentation.ui.composables.card.PersonaCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import java.util.Locale

@Composable
fun DappDetailScreen(
    viewModel: DappDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditPersona: (String, RequiredPersonaFields?) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                DappDetailEvent.LastPersonaDeleted -> onBackClick()
                DappDetailEvent.DappDeleted -> onBackClick()
                is DappDetailEvent.EditPersona -> {
                    onEditPersona(it.personaAddress, it.requiredPersonaFields)
                }
            }
        }
    }
    DappDetailContent(
        onBackClick = onBackClick,
        modifier = modifier,
        personaList = state.personas,
        dAppWithResources = state.dappWithMetadata,
        onPersonaClick = viewModel::onPersonaClick,
        onFungibleTokenClick = viewModel::onFungibleTokenClick,
        onNftClick = viewModel::onNftClick,
        selectedSheetState = state.selectedSheetState,
        selectedPersonaSharedAccounts = state.sharedPersonaAccounts,
        onDisconnectPersona = viewModel::onDisconnectPersona,
        personaDetailsClosed = viewModel::onPersonaDetailsClosed,
        onDeleteDapp = viewModel::onDeleteDapp,
        onEditPersona = viewModel::onEditPersona,
        onEditAccountSharing = viewModel::onEditAccountSharing,
        loading = state.loading
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DappDetailContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    personaList: ImmutableList<Network.Persona>,
    dAppWithResources: DAppWithMetadataAndAssociatedResources?,
    onPersonaClick: (Network.Persona) -> Unit,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNftClick: (Resource.NonFungibleResource) -> Unit,
    selectedSheetState: SelectedSheetState?,
    selectedPersonaSharedAccounts: ImmutableList<AccountItemUiModel>,
    onDisconnectPersona: (Network.Persona) -> Unit,
    personaDetailsClosed: () -> Unit,
    onDeleteDapp: () -> Unit,
    onEditPersona: () -> Unit,
    onEditAccountSharing: () -> Unit,
    loading: Boolean
) {
    var showDeleteDappPrompt by remember { mutableStateOf(false) }
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(bottomSheetState) {
        snapshotFlow {
            bottomSheetState.isVisible
        }.filter { !it }.distinctUntilChanged().collect {
            personaDetailsClosed()
        }
    }
    BackHandler(enabled = bottomSheetState.isVisible) {
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
            text = {
                Text(
                    text = stringResource(id = R.string.authorizedDapps_removeAuthorizationAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.authorizedDapps_forgetDappAlert_forget)
        )
    }

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = bottomSheetState,
        sheetContent = {
            when (selectedSheetState) {
                is SelectedSheetState.SelectedPersona -> {
                    selectedSheetState.persona?.let {
                        PersonaDetailsSheet(
                            persona = it,
                            sharedPersonaAccounts = selectedPersonaSharedAccounts,
                            onCloseClick = {
                                scope.launch {
                                    bottomSheetState.hide()
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .background(
                                    RadixTheme.colors.defaultBackground,
                                    shape = RadixTheme.shapes.roundedRectTopMedium
                                )
                                .clip(shape = RadixTheme.shapes.roundedRectTopMedium),
                            dappName = dAppWithResources?.dAppWithMetadata?.name.orEmpty(),
                            onDisconnectPersona = { persona ->
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

                is SelectedSheetState.SelectedFungibleResource -> {
                    FungibleTokenBottomSheetDetails(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        fungible = selectedSheetState.fungible,
                        onCloseClick = {
                            scope.launch {
                                bottomSheetState.hide()
                            }
                        }
                    )
                }

                is SelectedSheetState.SelectedNonFungibleResource -> {
                    NonFungibleTokenBottomSheetDetails(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        item = null,
                        onCloseClick = {
                            scope.launch {
                                bottomSheetState.hide()
                            }
                        },
                        nonFungibleResource = selectedSheetState.nonFungibleResource
                    )
                }

                else -> {}
            }
        },
        content = {
            Scaffold(
                topBar = {
                    Column {
                        RadixCenteredTopAppBar(
                            title = dAppWithResources?.dAppWithMetadata?.name.orEmpty(),
                            onBackClick = onBackClick,
                            windowInsets = WindowInsets.statusBars
                        )
                        Divider(color = RadixTheme.colors.gray5)
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    DappDetails(
                        modifier = Modifier.fillMaxSize(),
                        dAppWithResources = dAppWithResources,
                        personaList = personaList,
                        onPersonaClick = { persona ->
                            onPersonaClick(persona)
                            scope.launch {
                                bottomSheetState.show()
                            }
                        },
                        onFungibleTokenClick = { fungibleResource ->
                            onFungibleTokenClick(fungibleResource)
                            scope.launch {
                                bottomSheetState.show()
                            }
                        },
                        onNonFungibleClick = { nftItem ->
                            onNftClick(nftItem)
                            scope.launch {
                                bottomSheetState.show()
                            }
                        },
                        onDeleteDapp = {
                            showDeleteDappPrompt = true
                        }
                    )

                    if (loading) {
                        FullscreenCircularProgressContent()
                    }
                }
            }
        }
    )
}

@Composable
private fun DappDetails(
    modifier: Modifier,
    dAppWithResources: DAppWithMetadataAndAssociatedResources?,
    personaList: ImmutableList<Network.Persona>,
    onPersonaClick: (Network.Persona) -> Unit,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit,
    onDeleteDapp: () -> Unit
) {
    Column(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            dAppWithResources?.dAppWithMetadata?.let { dApp ->
                item {
                    Thumbnail.DApp(
                        modifier = Modifier
                            .padding(vertical = RadixTheme.dimensions.paddingDefault)
                            .size(104.dp),
                        dapp = dApp
                    )
                    Divider(color = RadixTheme.colors.gray5)
                }
            }
            dAppWithResources?.dAppWithMetadata?.description?.let { description ->
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensions.paddingDefault),
                        text = description,
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.Start
                    )
                    Divider(color = RadixTheme.colors.gray5)
                }
            }
            dAppWithResources?.dAppWithMetadata?.dAppAddress?.let { dappDefinitionAddress ->
                item {
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                    DappDefinitionAddressRow(
                        dappDefinitionAddress = dappDefinitionAddress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions.paddingDefault)
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
            }
            dAppWithResources?.dAppWithMetadata?.claimedWebsites?.let { websites ->
                if (websites.isNotEmpty()) {
                    item {
                        DAppWebsiteAddressRow(
                            websiteAddresses = websites,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.paddingDefault)
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                    }
                }
            }
            if (dAppWithResources?.fungibleResources?.isNotEmpty() == true) {
                item {
                    GrayBackgroundWrapper {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensions.paddingDefault),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_tokens),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
            itemsIndexed(dAppWithResources?.fungibleResources.orEmpty()) { _, fungibleToken ->
                GrayBackgroundWrapper {
                    FungibleCard(
                        modifier = Modifier.throttleClickable {
                            onFungibleTokenClick(fungibleToken)
                        },
                        fungible = fungibleToken,
                        showChevron = false
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
            }
            if (dAppWithResources?.nonFungibleResources?.isNotEmpty() == true) {
                item {
                    GrayBackgroundWrapper {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensions.paddingDefault),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_nfts),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
            items(dAppWithResources?.nonFungibleResources.orEmpty()) { nonFungibleResource ->
                GrayBackgroundWrapper {
                    NonFungibleCard(
                        modifier = Modifier.throttleClickable {
                            onNonFungibleClick(nonFungibleResource)
                        },
                        nonFungible = nonFungibleResource,
                        showChevron = false
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
            }
            item {
                GrayBackgroundWrapper {
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.authorizedDapps_dAppDetails_personasHeading),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                }
            }
            items(personaList) { persona ->
                GrayBackgroundWrapper {
                    PersonaCard(
                        modifier = Modifier.throttleClickable {
                            onPersonaClick(persona)
                        },
                        persona = persona,
                        showChevron = false
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
            }
            item {
                Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    onClick = onDeleteDapp,
                    shape = RadixTheme.shapes.roundedRectSmall,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        containerColor = RadixTheme.colors.red1
                    )
                ) {
                    Text(
                        text = stringResource(R.string.authorizedDapps_dAppDetails_forgetDapp),
                        style = RadixTheme.typography.body1Header,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun DappDefinitionAddressRow(
    dappDefinitionAddress: String,
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
            address = dappDefinitionAddress,
            textStyle = RadixTheme.typography.body1Regular,
            textColor = RadixTheme.colors.gray1
        )
    }
}

@Composable
fun DAppWebsiteAddressRow(
    websiteAddresses: ImmutableList<String>,
    modifier: Modifier = Modifier
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
        websiteAddresses.forEach { address ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = address,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.blue1
                )
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_link_out),
                    tint = RadixTheme.colors.gray3,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun PersonaDetailsSheet(
    persona: PersonaUiModel,
    sharedPersonaAccounts: ImmutableList<AccountItemUiModel>,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    dappName: String,
    onDisconnectPersona: (Network.Persona) -> Unit,
    onEditPersona: () -> Unit,
    onEditAccountSharing: () -> Unit
) {
    var personaToDisconnect by remember { mutableStateOf<Network.Persona?>(null) }
    Box(modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
            RadixCenteredTopAppBar(
                title = persona.persona.displayName,
                onBackClick = onCloseClick,
                contentColor = RadixTheme.colors.gray1
            )
            Divider(color = RadixTheme.colors.gray5)
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
            text = {
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
    onDisconnectPersona: (Network.Persona) -> Unit,
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
                value = persona.persona.displayName
            )
            Spacer(modifier = Modifier.height(dimensions.paddingXLarge))
            Divider(
                modifier = Modifier.padding(horizontal = dimensions.paddingDefault)
            )
        }
        val nonEmptyPersonaFields = persona.persona.personaData.allFields
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
                                Brush.horizontalGradient(
                                    AccountGradientList[account.appearanceID % AccountGradientList.size]
                                ),
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
            item {
                Divider(color = RadixTheme.colors.gray5)
                Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    onClick = {
                        onDisconnectPersona(persona.persona)
                    },
                    shape = RadixTheme.shapes.roundedRectSmall,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        containerColor = RadixTheme.colors.red1
                    )
                ) {
                    Text(
                        text = stringResource(R.string.authorizedDapps_personaDetails_removeAuthorization),
                        style = RadixTheme.typography.body1Header,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DappDetailContentPreview() {
    RadixWalletTheme {
        DappDetailContent(
            onBackClick = {},
            personaList = persistentListOf(SampleDataProvider().samplePersona()),
            dAppWithResources = DAppWithMetadataAndAssociatedResources(
                dAppWithMetadata = DAppWithMetadata(
                    dAppAddress = "account_tdx_abc",
                    nameItem = NameMetadataItem("Dapp"),
                    descriptionItem = DescriptionMetadataItem("Description"),
                    claimedWebsitesItem = ClaimedWebsitesMetadataItem(
                        websites = listOf(
                            "https://hammunet-dashboard.rdx-works-main.extratools.works",
                            "https://ansharnet-dashboard.rdx-works-main.extratools.works"
                        )
                    )
                ),
                resources = DAppResources(emptyList(), emptyList()),
            ),
            onPersonaClick = {},
            onFungibleTokenClick = {},
            onNftClick = {},
            selectedSheetState = null,
            selectedPersonaSharedAccounts = persistentListOf(
                AccountItemUiModel("account_tdx_efgh", "Account1", 0)
            ),
            onDisconnectPersona = {},
            personaDetailsClosed = {},
            onDeleteDapp = {},
            onEditPersona = {},
            onEditAccountSharing = {},
            loading = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PersonaDetailsSheetPreview() {
    RadixWalletTheme {
        PersonaDetailsSheet(
            persona = PersonaUiModel(SampleDataProvider().samplePersona()),
            sharedPersonaAccounts = persistentListOf(
                AccountItemUiModel("account_tdx_efgh", "Account1", 0)
            ),
            onCloseClick = {},
            dappName = "dApp",
            onDisconnectPersona = {},
            onEditPersona = {}
        ) {}
    }
}

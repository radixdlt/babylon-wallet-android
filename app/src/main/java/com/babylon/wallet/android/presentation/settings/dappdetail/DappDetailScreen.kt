@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.dappdetail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.PersonaPropertyRow
import com.babylon.wallet.android.presentation.ui.composables.PersonaRoundedAvatar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.StandardOneLineCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import java.util.Locale

@Composable
fun DappDetailScreen(
    viewModel: DappDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditPersona: (String, String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                DappDetailEvent.LastPersonaDeleted -> onBackClick()
                DappDetailEvent.DappDeleted -> onBackClick()
                is DappDetailEvent.EditPersona -> {
                    onEditPersona(it.personaAddress, it.requiredFieldsStringEncoded)
                }
            }
        }
    }
    DappDetailContent(
        onBackClick = onBackClick,
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        dappName = state.dapp?.displayName.orEmpty(),
        personaList = state.personas,
        dappMetadata = state.dappMetadata,
        onPersonaClick = viewModel::onPersonaClick,
        selectedPersona = state.selectedPersona,
        selectedPersonaSharedAccounts = state.sharedPersonaAccounts,
        onDisconnectPersona = viewModel::onDisconnectPersona,
        personaDetailsClosed = viewModel::onPersonaDetailsClosed,
        onDeleteDapp = viewModel::onDeleteDapp,
        onEditPersona = viewModel::onEditPersona,
        onEditAccountSharing = {},
        loading = state.loading
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DappDetailContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    dappName: String,
    personaList: ImmutableList<Network.Persona>,
    dappMetadata: DappMetadata?,
    onPersonaClick: (Network.Persona) -> Unit,
    selectedPersona: PersonaUiModel?,
    selectedPersonaSharedAccounts: ImmutableList<AccountItemUiModel>,
    onDisconnectPersona: (Network.Persona) -> Unit,
    personaDetailsClosed: () -> Unit,
    onDeleteDapp: () -> Unit,
    onEditPersona: () -> Unit,
    onEditAccountSharing: () -> Unit,
    loading: Boolean
) {
    var showDeleteDappPrompt by remember { mutableStateOf(false) }
    val snackState = remember { SnackbarHostState() }
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(bottomSheetState.isVisible) {
        if (!bottomSheetState.isVisible) {
            personaDetailsClosed()
        }
    }
    BackHandler(enabled = bottomSheetState.isVisible) {
        scope.launch {
            bottomSheetState.hide()
        }
    }
    Box(modifier = modifier) {
        AnimatedVisibility(modifier = Modifier.fillMaxSize(), visible = !loading, enter = fadeIn(), exit = fadeOut()) {
            DefaultModalSheetLayout(
                modifier = Modifier.fillMaxSize(),
                sheetState = bottomSheetState,
                sheetContent = {
                    Column(Modifier.fillMaxSize()) {
                        selectedPersona?.let { persona ->
                            PersonaDetailsSheet(
                                persona = persona,
                                sharedPersonaAccounts = selectedPersonaSharedAccounts,
                                onCloseClick = {
                                    scope.launch {
                                        bottomSheetState.hide()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        RadixTheme.colors.defaultBackground,
                                        shape = RadixTheme.shapes.roundedRectTopMedium
                                    )
                                    .clip(shape = RadixTheme.shapes.roundedRectTopMedium),
                                dappName = dappName,
                                onDisconnectPersona = {
                                    scope.launch {
                                        bottomSheetState.hide()
                                    }
                                    onDisconnectPersona(it)
                                },
                                onEditPersona = onEditPersona,
                                onEditAccountSharing = onEditAccountSharing
                            )
                        }
                    }
                },
                content = {
                    DappDetails(
                        modifier = Modifier.fillMaxSize(),
                        dappName = dappName,
                        onBackClick = onBackClick,
                        dappMetadata = dappMetadata,
                        personaList = personaList,
                        onPersonaClick = { persona ->
                            onPersonaClick(persona)
                            scope.launch {
                                bottomSheetState.show()
                            }
                        },
                        onDeleteDapp = {
                            showDeleteDappPrompt = true
                        }
                    )
                }
            )
        }
        if (loading) {
            FullscreenCircularProgressContent()
        }
        RadixSnackbarHost(hostState = snackState, modifier = Modifier.align(Alignment.BottomCenter))
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
                        text = stringResource(id = R.string.forget_this_dapp),
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.gray1
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.disconnect_dapp_prompt),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1
                    )
                }
            )
        }
    }
}

@Composable
private fun DappDetails(
    modifier: Modifier,
    dappName: String,
    onBackClick: () -> Unit,
    dappMetadata: DappMetadata?,
    personaList: ImmutableList<Network.Persona>,
    onPersonaClick: (Network.Persona) -> Unit,
    onDeleteDapp: () -> Unit
) {
    Column(modifier = modifier) {
        RadixCenteredTopAppBar(
            title = dappName,
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(
            contentPadding = PaddingValues(vertical = dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                PersonaRoundedAvatar(
                    url = "",
                    modifier = Modifier
                        .padding(vertical = dimensions.paddingDefault)
                        .size(104.dp)
                )
                Divider(
                    modifier = Modifier.padding(horizontal = dimensions.paddingDefault),
                    color = RadixTheme.colors.gray5
                )
            }
            dappMetadata?.getDescription()?.let { description ->
                item {
                    Divider(color = RadixTheme.colors.gray5)
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensions.paddingDefault),
                        text = description,
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.Center
                    )
                    Divider(color = RadixTheme.colors.gray5)
                }
            }
            dappMetadata?.dAppDefinitionAddress?.let { dappDefinitionAddress ->
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
            dappMetadata?.getDisplayableMetadata()?.let { metadata ->
                item {
                    metadata.forEach { mapEntry ->
                        AssetMetadataRow(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.paddingDefault),
                            mapEntry.key,
                            mapEntry.value
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                    }
                }
            }
            item {
                GrayBackgroundWrapper {
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.here_are_the_personas),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                }
            }
            items(personaList) { persona ->
                GrayBackgroundWrapper {
                    StandardOneLineCard(
                        "",
                        persona.displayName,
                        modifier = Modifier
                            .shadow(elevation = 8.dp, shape = RadixTheme.shapes.roundedRectMedium)
                            .clip(RadixTheme.shapes.roundedRectMedium)
                            .throttleClickable {
                                onPersonaClick(persona)
                            }
                            .fillMaxWidth()
                            .background(
                                RadixTheme.colors.white,
                                shape = RadixTheme.shapes.roundedRectMedium
                            )
                            .padding(
                                horizontal = dimensions.paddingLarge,
                                vertical = dimensions.paddingDefault
                            ),
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
                        text = stringResource(R.string.forget_this_dapp),
                        style = RadixTheme.typography.body1Header,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DappDefinitionAddressRow(
    dappDefinitionAddress: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.dapp_definition).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
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
                    text = stringResource(id = R.string.remove_authorization),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.disconnect_persona_prompt),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            }
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
            AsyncImage(
                model = "",
                placeholder = painterResource(id = R.drawable.img_placeholder),
                fallback = painterResource(id = R.drawable.img_placeholder),
                error = painterResource(id = R.drawable.img_placeholder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(vertical = dimensions.paddingDefault)
                    .size(104.dp)
                    .clip(RadixTheme.shapes.circle)
            )
        }
        item {
            PersonaPropertyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault),
                label = stringResource(id = R.string.persona_label),
                value = persona.persona.displayName
            )
            Spacer(modifier = Modifier.height(dimensions.paddingXLarge))
            Divider(
                modifier = Modifier.padding(horizontal = dimensions.paddingDefault)
            )
        }
        if (persona.persona.fields.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    text = stringResource(R.string.here_is_the_personal_data, dappName),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(dimensions.paddingLarge))
            }
            items(persona.persona.fields) { field ->
                PersonaPropertyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    label = stringResource(id = field.kind.toDisplayResource()),
                    value = field.value
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
                text = stringResource(R.string.edit_persona),
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
                        text = stringResource(R.string.here_are_the_account_names, dappName),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2,
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
            }
            items(sharedPersonaAccounts) { account ->
                GrayBackgroundWrapper(Modifier.fillMaxWidth()) {
                    PersonaSharedAccountCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(AccountGradientList[account.appearanceID]),
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
                        text = stringResource(R.string.edit_account_sharing),
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
                        text = stringResource(R.string.remove_authorization),
                        style = RadixTheme.typography.body1Header,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonaSharedAccountCard(
    modifier: Modifier = Modifier,
    account: AccountItemUiModel
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = account.displayName.orEmpty(),
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = RadixTheme.colors.white
        )

        ActionableAddressView(
            address = account.address,
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DappDetailContentPreview() {
    RadixWalletTheme {
        DappDetailContent(
            onBackClick = {},
            dappName = "Dapp",
            personaList = persistentListOf(SampleDataProvider().samplePersona()),
            dappMetadata = DappMetadata("account_tdx_abcd", mapOf(MetadataConstants.KEY_DESCRIPTION to "Description")),
            onPersonaClick = {},
            selectedPersona = PersonaUiModel(SampleDataProvider().samplePersona()),
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

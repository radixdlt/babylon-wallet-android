@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.personas.personadetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.DAppDetailsSheetContent
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataFieldRow
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataStringField
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.WarningButton
import com.babylon.wallet.android.presentation.ui.composables.card.DappCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun PersonaDetailScreen(
    viewModel: PersonaDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditPersona: (String) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.Close -> onBackClick()
            }
        }
    }
    var showHidePersonaPrompt by remember { mutableStateOf(false) }
    if (showHidePersonaPrompt) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    viewModel.onHidePersona()
                }
                showHidePersonaPrompt = false
            },
            text = {
                Text(
                    text = stringResource(id = R.string.authorizedDapps_personaDetails_hidePersonaConfirmation),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.common_continue)
        )
    }
    PersonaDetailContent(
        modifier = modifier,
        onBackClick = onBackClick,
        persona = state.persona,
        onEditPersona = onEditPersona,
        authorizedDapps = state.authorizedDapps,
        selectedDApp = state.selectedDApp,
        onDAppClick = viewModel::onDAppClick,
        hasAuthKey = state.hasAuthKey,
        onCreateAndUploadAuthKey = {
            context.biometricAuthenticate {
                if (it) {
                    viewModel.onCreateAndUploadAuthKey()
                }
            }
        },
        loading = state.loading,
        onHidePersona = {
            showHidePersonaPrompt = true
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PersonaDetailContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    persona: Network.Persona?,
    onEditPersona: (String) -> Unit,
    authorizedDapps: ImmutableList<DAppWithResources>,
    selectedDApp: DAppWithResources?,
    onDAppClick: (DAppWithResources) -> Unit,
    hasAuthKey: Boolean,
    onCreateAndUploadAuthKey: () -> Unit,
    loading: Boolean,
    onHidePersona: () -> Unit
) {
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = bottomSheetState,
        sheetContent = {
            selectedDApp?.let {
                DAppDetailsSheetContent(
                    modifier = Modifier.navigationBarsPadding(),
                    onBackClick = {
                        scope.launch {
                            bottomSheetState.hide()
                        }
                    },
                    dApp = it
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    RadixCenteredTopAppBar(
                        title = persona?.displayName.orEmpty(),
                        onBackClick = onBackClick,
                        windowInsets = WindowInsets.statusBars
                    )

                    HorizontalDivider(color = RadixTheme.colors.gray5)
                }
            },
            containerColor = RadixTheme.colors.defaultBackground
        ) { padding ->
            if (persona != null) {
                PersonaDetailList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding),
                    persona = persona,
                    authorizedDapps = authorizedDapps,
                    onDAppClick = {
                        onDAppClick(it)
                        scope.launch {
                            bottomSheetState.show()
                        }
                    },
                    onEditPersona = onEditPersona,
                    hasAuthKey = hasAuthKey,
                    onCreateAndUploadAuthKey = onCreateAndUploadAuthKey,
                    loading = loading,
                    onHidePersona = onHidePersona
                )
            } else {
                FullscreenCircularProgressContent()
            }
        }
    }
}

@Composable
private fun PersonaDetailList(
    modifier: Modifier = Modifier,
    persona: Network.Persona,
    authorizedDapps: ImmutableList<DAppWithResources>,
    onDAppClick: (DAppWithResources) -> Unit,
    onEditPersona: (String) -> Unit,
    hasAuthKey: Boolean,
    onCreateAndUploadAuthKey: () -> Unit,
    loading: Boolean,
    onHidePersona: () -> Unit
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
                persona = persona
            )
        }
        item {
            PersonaDataStringField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault),
                label = stringResource(id = R.string.authorizedDapps_personaDetails_personaLabelHeading),
                value = persona.displayName
            )
            HorizontalDivider(modifier = Modifier.padding(dimensions.paddingDefault), color = RadixTheme.colors.gray4)
        }
        val allFields = persona.personaData.allFields
        if (allFields.isNotEmpty()) {
            val lastItem = allFields.last()
            items(allFields) { field ->
                PersonaDataFieldRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    field = field.value
                )
                if (field != lastItem) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = dimensions.paddingDefault,
                            vertical = dimensions.paddingLarge
                        ),
                        color = RadixTheme.colors.gray4
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingXLarge),
                text = stringResource(id = R.string.authorizedDapps_personaDetails_editPersona),
                onClick = { onEditPersona(persona.address) },
                throttleClicks = true
            )
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED && !hasAuthKey) {
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingXLarge),
                    text = stringResource(id = R.string.biometrics_prompt_createSignAuthKey),
                    onClick = onCreateAndUploadAuthKey,
                    enabled = !loading,
                    throttleClicks = true
                )
                Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            }
        }
        if (authorizedDapps.isNotEmpty()) {
            item {
                GrayBackgroundWrapper {
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.authorizedDapps_personaDetails_authorizedDappsHeading),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                }
            }
            items(authorizedDapps) { dApp ->
                GrayBackgroundWrapper {
                    DappCard(
                        modifier = Modifier.throttleClickable {
                            onDAppClick(dApp)
                        },
                        dApp = dApp.dApp
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            WarningButton(
                modifier = Modifier
                    .padding(horizontal = dimensions.paddingDefault),
                text = stringResource(id = R.string.authorizedDapps_personaDetails_hideThisPersona),
                onClick = onHidePersona
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PersonaDetailContentPreview() {
    RadixWalletTheme {
        PersonaDetailContent(
            modifier = Modifier.fillMaxSize(),
            onBackClick = {},
            persona = SampleDataProvider().samplePersona(),
            onEditPersona = {},
            authorizedDapps = persistentListOf(),
            selectedDApp = null,
            onDAppClick = {},
            hasAuthKey = false,
            onCreateAndUploadAuthKey = {},
            loading = false,
            onHidePersona = {}
        )
    }
}

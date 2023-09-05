@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.personadetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.settings.dappdetail.DAppDetailsSheetContent
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataFieldRow
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataStringField
import com.babylon.wallet.android.presentation.ui.composables.PersonaRoundedAvatar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.StandardOneLineCard
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    PersonaDetailContent(
        onBackClick = onBackClick,
        modifier = modifier,
        persona = state.persona,
        onEditPersona = onEditPersona,
        authorizedDapps = state.authorizedDapps,
        selectedDApp = state.selectedDApp,
        onDAppClick = viewModel::onDAppClick,
        hasAuthKey = state.hasAuthKey,
        loading = state.loading,
        onCreateAndUploadAuthKey = viewModel::onCreateAndUploadAuthKey
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PersonaDetailContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    persona: Network.Persona?,
    onEditPersona: (String) -> Unit,
    authorizedDapps: ImmutableList<DAppWithMetadataAndAssociatedResources>,
    selectedDApp: DAppWithMetadataAndAssociatedResources?,
    onDAppClick: (DAppWithMetadataAndAssociatedResources) -> Unit,
    hasAuthKey: Boolean,
    onCreateAndUploadAuthKey: () -> Unit,
    loading: Boolean
) {
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        persona?.let { persona ->
            DefaultModalSheetLayout(
                sheetState = bottomSheetState,
                sheetContent = {
                    selectedDApp?.let {
                        DAppDetailsSheetContent(
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
                                title = persona.displayName,
                                onBackClick = onBackClick,
                                windowInsets = WindowInsets.statusBars
                            )

                            Divider(color = RadixTheme.colors.gray5)
                        }
                    },
                    containerColor = RadixTheme.colors.defaultBackground
                ) { padding ->
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
                        loading = loading
                    )
                }
            }
        }

        if (persona == null) {
            FullscreenCircularProgressContent()
        }
    }
}

@Composable
private fun PersonaDetailList(
    modifier: Modifier = Modifier,
    persona: Network.Persona,
    authorizedDapps: ImmutableList<DAppWithMetadataAndAssociatedResources>,
    onDAppClick: (DAppWithMetadataAndAssociatedResources) -> Unit,
    onEditPersona: (String) -> Unit,
    hasAuthKey: Boolean,
    onCreateAndUploadAuthKey: () -> Unit,
    loading: Boolean
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        item {
            PersonaRoundedAvatar(
                url = "",
                modifier = Modifier
                    .padding(vertical = dimensions.paddingDefault)
                    .size(104.dp)
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
            Divider(modifier = Modifier.padding(dimensions.paddingDefault), color = RadixTheme.colors.gray4)
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
                    Divider(
                        modifier = Modifier.padding(horizontal = dimensions.paddingDefault, vertical = dimensions.paddingLarge),
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
                    StandardOneLineCard(
                        image = dApp.dAppWithMetadata.iconUrl.toString(),
                        title = dApp.dAppWithMetadata.displayName(),
                        modifier = Modifier
                            .shadow(elevation = 8.dp, shape = RadixTheme.shapes.roundedRectMedium)
                            .clip(RadixTheme.shapes.roundedRectMedium)
                            .throttleClickable {
                                onDAppClick(dApp)
                            }
                            .fillMaxWidth()
                            .background(RadixTheme.colors.white, shape = RadixTheme.shapes.roundedRectMedium)
                            .padding(
                                horizontal = dimensions.paddingLarge,
                                vertical = dimensions.paddingDefault
                            ),
                        showChevron = false
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DappDetailContentPreview() {
    RadixWalletTheme {
        PersonaDetailContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            persona = SampleDataProvider().samplePersona(),
            onEditPersona = {},
            authorizedDapps = persistentListOf(),
            selectedDApp = null,
            onDAppClick = {},
            hasAuthKey = false,
            onCreateAndUploadAuthKey = {},
            loading = false
        )
    }
}

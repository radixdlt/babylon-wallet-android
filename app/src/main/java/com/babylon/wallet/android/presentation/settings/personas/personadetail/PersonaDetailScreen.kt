package com.babylon.wallet.android.presentation.settings.personas.personadetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataFieldRow
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataSectionHeader
import com.babylon.wallet.android.presentation.ui.composables.PersonaDataStringField
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.WarningButton
import com.babylon.wallet.android.presentation.ui.composables.card.DappCard
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.DApp
import rdx.works.core.sargon.fields

@Composable
fun PersonaDetailScreen(
    viewModel: PersonaDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditPersona: (IdentityAddress) -> Unit,
    onDAppClick: (DApp) -> Unit,
    onFactorSourceCardClick: (FactorSourceId) -> Unit
) {
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
            message = {
                Text(
                    text = stringResource(id = R.string.authorizedDapps_personaDetails_hidePersonaConfirmation),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.text
                )
            },
            confirmText = stringResource(id = R.string.common_continue)
        )
    }
    PersonaDetailContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onEditPersona = onEditPersona,
        onDAppClick = onDAppClick,
        onHidePersona = {
            showHidePersonaPrompt = true
        },
        onFactorSourceCardClick = onFactorSourceCardClick
    )
}

@Composable
private fun PersonaDetailContent(
    modifier: Modifier = Modifier,
    state: PersonaDetailUiState,
    onBackClick: () -> Unit,
    onEditPersona: (IdentityAddress) -> Unit,
    onDAppClick: (DApp) -> Unit,
    onHidePersona: () -> Unit,
    onFactorSourceCardClick: (FactorSourceId) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = state.persona?.displayName?.value.orEmpty(),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        bottomBar = {
            if (state.persona != null) {
                RadixBottomBar(
                    button = {
                        WarningButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = stringResource(id = R.string.authorizedDapps_personaDetails_hideThisPersona),
                            onClick = onHidePersona
                        )
                    }
                )
            }
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        if (state.persona != null) {
            PersonaDetailList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                persona = state.persona,
                authorizedDapps = state.authorizedDapps,
                securedWith = state.securedWith,
                onDAppClick = onDAppClick,
                onEditPersona = onEditPersona,
                onFactorSourceCardClick = onFactorSourceCardClick
            )
        } else {
            FullscreenCircularProgressContent()
        }
    }
}

@Composable
private fun PersonaDetailList(
    modifier: Modifier = Modifier,
    persona: Persona,
    authorizedDapps: ImmutableList<DApp>,
    securedWith: FactorSourceCard?,
    onDAppClick: (DApp) -> Unit,
    onEditPersona: (IdentityAddress) -> Unit,
    onFactorSourceCardClick: (FactorSourceId) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        item {
            Thumbnail.Persona(
                modifier = Modifier
                    .padding(
                        top = RadixTheme.dimensions.paddingDefault,
                        bottom = RadixTheme.dimensions.paddingLarge
                    )
                    .size(104.dp),
                persona = persona
            )
        }
        item {
            PersonaDataStringField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                label = stringResource(id = R.string.authorizedDapps_personaDetails_personaLabelHeading),
                labelStyle = RadixTheme.typography.body1Header,
                value = persona.displayName.value
            )
            HorizontalDivider(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge),
                color = RadixTheme.colors.divider
            )
        }
        val allFields = persona.personaData.fields
        if (allFields.isNotEmpty()) {
            items(allFields) { field ->
                PersonaDataFieldRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    field = field.value,
                    labelStyle = RadixTheme.typography.body1Header
                )
                HorizontalDivider(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge),
                    color = RadixTheme.colors.divider
                )
            }
        }

        securedWith?.let { factorSourceCard ->
            item {
                PersonaDataSectionHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(id = R.string.common_securedWith),
                    textStyle = RadixTheme.typography.body1Header
                )

                Spacer(modifier = Modifier.height(dimensions.paddingSmall))

                FactorSourceCardView(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .throttleClickable { onFactorSourceCardClick(factorSourceCard.id) },
                    item = factorSourceCard
                )

                HorizontalDivider(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge),
                    color = RadixTheme.colors.divider
                )
            }
        }

        item {
            RadixSecondaryButton(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.authorizedDapps_personaDetails_editPersona),
                onClick = { onEditPersona(persona.address) },
                throttleClicks = true
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }
        if (authorizedDapps.isNotEmpty()) {
            item {
                HorizontalDivider(
                    color = RadixTheme.colors.divider
                )
                GrayBackgroundWrapper {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                        text = stringResource(R.string.authorizedDapps_personaDetails_authorizedDappsHeading),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
                }
            }
            items(authorizedDapps) { dApp ->
                GrayBackgroundWrapper {
                    DappCard(
                        modifier = Modifier.throttleClickable {
                            onDAppClick(dApp)
                        },
                        dApp = dApp
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
            item {
                GrayBackgroundWrapper(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
                }
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PersonaDetailContentPreview() {
    RadixWalletTheme {
        PersonaDetailContent(
            modifier = Modifier.fillMaxSize(),
            state = PersonaDetailUiState(
                authorizedDapps = persistentListOf(
                    DApp.sampleMainnet(),
                    DApp.sampleMainnet.other()
                ),
                persona = Persona.sampleMainnet(),
                securedWith = FactorSource.sample().toFactorSourceCard(
                    includeLastUsedOn = true
                )
            ),
            onBackClick = {},
            onEditPersona = {},
            onDAppClick = {},
            onHidePersona = {},
            onFactorSourceCardClick = {}
        )
    }
}

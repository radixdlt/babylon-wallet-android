package com.babylon.wallet.android.presentation.settings.personas

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.settings.personas.PersonasViewModel.PersonasEvent
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.PersonaCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun PersonasScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonasViewModel,
    onBackClick: () -> Unit,
    createNewPersona: (Boolean) -> Unit,
    onPersonaClick: (String) -> Unit,
    onNavigateToMnemonicBackup: (FactorSource.FactorSourceID.FromHash) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context: Context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is PersonasEvent.CreatePersona -> createNewPersona(it.firstPersonaCreated)
            }
        }
    }
    PersonasContent(
        personas = state.personas,
        modifier = modifier,
        onBackClick = onBackClick,
        createNewPersona = viewModel::onCreatePersona,
        onPersonaClick = onPersonaClick,
        showSecurityPrompt = state.showSecurityPrompt,
        onApplySecuritySettings = {
            state.babylonFactorSource?.id?.let { babylonFactorSourceID ->
                context.biometricAuthenticate { authenticated ->
                    if (authenticated) {
                        onNavigateToMnemonicBackup(babylonFactorSourceID)
                    }
                }
            }
        }
    )
}

@Composable
fun PersonasContent(
    personas: ImmutableList<Network.Persona>,
    modifier: Modifier,
    onBackClick: () -> Unit,
    createNewPersona: () -> Unit,
    onPersonaClick: (String) -> Unit,
    showSecurityPrompt: Boolean,
    onApplySecuritySettings: (() -> Unit)?
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.personas_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            Divider(color = RadixTheme.colors.gray5)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.personas_subtitle),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingXLarge
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showSecurityPrompt) {
                    item {
                        ApplySecuritySettingsLabel(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onApplySecuritySettings,
                            text = "Please write down this Persona's seed phrase",
                            labelColor = RadixTheme.colors.backgroundAlternate.copy(alpha = 0.3f),
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }
                }
//                item {
//                InfoLink( // TODO enable it when we have a link
//                    stringResource(R.string.personas_whatIsPersona),
//                    modifier = Modifier.fillMaxWidth()
//                )
//                }
                itemsIndexed(items = personas) { _, personaItem ->
                    PersonaCard(
                        modifier = Modifier.throttleClickable {
                            onPersonaClick(personaItem.address)
                        },
                        persona = personaItem,
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                    RadixSecondaryButton(
                        text = stringResource(id = R.string.personas_createNewPersona),
                        onClick = createNewPersona
                    )
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PersonasScreenPreview() {
    RadixWalletTheme {
        PersonasContent(
            personas = listOf(
                SampleDataProvider().samplePersona(
                    personaAddress = "address1",
                    personaName = "persona1"
                ),
                SampleDataProvider().samplePersona(
                    personaAddress = "address2",
                    personaName = "persona2"
                ),
            ).toImmutableList(),
            modifier = Modifier,
            onBackClick = {},
            createNewPersona = {},
            onPersonaClick = {},
            showSecurityPrompt = true,
            onApplySecuritySettings = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PersonasScreenEmptyPreview() {
    RadixWalletTheme {
        PersonasContent(
            personas = persistentListOf(),
            modifier = Modifier,
            onBackClick = {},
            createNewPersona = {},
            onPersonaClick = {},
            showSecurityPrompt = false,
            onApplySecuritySettings = {}
        )
    }
}

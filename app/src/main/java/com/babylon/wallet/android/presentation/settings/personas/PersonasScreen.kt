package com.babylon.wallet.android.presentation.settings.personas

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
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
import com.babylon.wallet.android.presentation.settings.personas.PersonasViewModel.PersonasEvent
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.PersonaCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import rdx.works.core.sargon.factorSourceId

@Composable
fun PersonasScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonasViewModel,
    onBackClick: () -> Unit,
    createNewPersona: (Boolean) -> Unit,
    onPersonaClick: (IdentityAddress) -> Unit,
    onNavigateToMnemonicBackup: (FactorSourceId.Hash) -> Unit
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
        state = state,
        modifier = modifier.fillMaxSize(),
        onBackClick = onBackClick,
        createNewPersona = viewModel::onCreatePersona,
        onPersonaClick = onPersonaClick,
        onApplySecuritySettings = { factorSourceID ->
            (factorSourceID as? FactorSourceId.Hash)?.let { id ->
                context.biometricAuthenticate { result ->
                    if (result == BiometricAuthenticationResult.Succeeded) {
                        onNavigateToMnemonicBackup(id)
                    }
                }
            }
        }
    )
}

@Composable
fun PersonasContent(
    state: PersonasViewModel.PersonasUiState,
    modifier: Modifier,
    onBackClick: () -> Unit,
    createNewPersona: () -> Unit,
    onPersonaClick: (IdentityAddress) -> Unit,
    onApplySecuritySettings: ((FactorSourceId.Hash) -> Unit)
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
            HorizontalDivider(color = RadixTheme.colors.gray5)
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
                    vertical = RadixTheme.dimensions.paddingXXLarge
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
//                item {
//                InfoLink( // TODO enable it when we have a link
//                    stringResource(R.string.personas_whatIsPersona),
//                    modifier = Modifier.fillMaxWidth()
//                )
//                }
                itemsIndexed(items = state.personas) { _, personaItem ->
                    PersonaCard(
                        modifier = Modifier.throttleClickable {
                            onPersonaClick(personaItem.address)
                        },
                        persona = personaItem,
                        displaySecurityPrompt = state.securityPrompt(personaItem) != null,
                        onApplySecuritySettings = {
                            onApplySecuritySettings(personaItem.factorSourceId as FactorSourceId.Hash)
                        }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
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

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PersonasScreenPreview() {
    RadixWalletTheme {
        PersonasContent(
            PersonasViewModel.PersonasUiState(
                personas = Persona.sampleMainnet.all.toImmutableList()
            ),
            modifier = Modifier,
            onBackClick = {},
            createNewPersona = {},
            onPersonaClick = {},
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun PersonasScreenEmptyPreview() {
    RadixWalletTheme {
        PersonasContent(
            state = PersonasViewModel.PersonasUiState(personas = persistentListOf()),
            modifier = Modifier,
            onBackClick = {},
            createNewPersona = {},
            onPersonaClick = {}
        ) {}
    }
}

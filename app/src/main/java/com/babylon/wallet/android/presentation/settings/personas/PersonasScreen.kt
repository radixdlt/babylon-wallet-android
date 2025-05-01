package com.babylon.wallet.android.presentation.settings.personas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.usecases.securityproblems.EntityWithSecurityPrompt
import com.babylon.wallet.android.domain.usecases.securityproblems.SecurityPromptType
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.personas.PersonasViewModel.PersonasEvent
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.PersonaCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun PersonasScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonasViewModel,
    onBackClick: () -> Unit,
    createNewPersona: (Boolean) -> Unit,
    onPersonaClick: (IdentityAddress) -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
        onNavigateToSecurityCenter = onNavigateToSecurityCenter,
        onInfoClick = onInfoClick
    )
}

@Composable
fun PersonasContent(
    state: PersonasViewModel.PersonasUiState,
    modifier: Modifier,
    onBackClick: () -> Unit,
    createNewPersona: () -> Unit,
    onPersonaClick: (IdentityAddress) -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.personas_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.divider)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingMedium
                        ),
                        text = stringResource(id = R.string.personas_subtitle),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.textSecondary
                    )
                    InfoButton(
                        modifier = Modifier.padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingMedium
                        ),
                        text = stringResource(id = R.string.infoLink_title_personas),
                        onClick = {
                            onInfoClick(GlossaryItem.personas)
                        }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
                itemsIndexed(items = state.personas) { _, personaItem ->
                    PersonaCard(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                            .clip(RadixTheme.shapes.roundedRectMedium)
                            .throttleClickable {
                                onPersonaClick(personaItem.address)
                            },
                        persona = personaItem,
                        securityPrompts = state.securityPrompt(personaItem)?.toPersistentList(),
                        onNavigateToSecurityCenter = onNavigateToSecurityCenter
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(align = Alignment.CenterHorizontally)
                            .padding(bottom = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.personas_createNewPersona),
                        onClick = createNewPersona
                    )
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
            onNavigateToSecurityCenter = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PersonasScreenWithSecurityPromptsPreview() {
    RadixWalletTheme {
        val personas = Persona.sampleMainnet.all.toImmutableList()
        PersonasContent(
            PersonasViewModel.PersonasUiState(
                personas = personas,
                entitiesWithSecurityPrompts = listOf(
                    EntityWithSecurityPrompt(
                        entity = ProfileEntity.PersonaEntity(personas.first()),
                        prompts = setOf(
                            SecurityPromptType.RECOVERY_REQUIRED,
                            SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM
                        )
                    )
                )
            ),
            modifier = Modifier,
            onBackClick = {},
            createNewPersona = {},
            onPersonaClick = {},
            onNavigateToSecurityCenter = {},
            onInfoClick = {}
        )
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
            onPersonaClick = {},
            onNavigateToSecurityCenter = {},
            onInfoClick = {}
        )
    }
}

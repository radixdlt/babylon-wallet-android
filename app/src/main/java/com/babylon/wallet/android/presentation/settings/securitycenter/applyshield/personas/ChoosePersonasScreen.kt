package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.personas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.composables.ChooseEntityContent
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityEvent
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityUiState
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.card.SimplePersonaSelectionCard
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet

@Composable
fun ChoosePersonasScreen(
    modifier: Modifier = Modifier,
    viewModel: ChoosePersonasViewModel,
    onDismiss: () -> Unit,
    onSelected: (List<AddressOfAccountOrPersona>) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChoosePersonasContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onSelectAllToggleClick = viewModel::onSelectAllToggleClick,
        onSelectPersona = viewModel::onSelectItem,
        onContinueClick = viewModel::onContinueClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ChooseEntityEvent.EntitiesSelected -> onSelected(event.addresses)
            }
        }
    }
}

@Composable
private fun ChoosePersonasContent(
    modifier: Modifier = Modifier,
    state: ChooseEntityUiState<Persona>,
    onDismiss: () -> Unit,
    onSelectAllToggleClick: () -> Unit,
    onSelectPersona: (Persona) -> Unit,
    onContinueClick: () -> Unit
) {
    ChooseEntityContent(
        modifier = modifier.fillMaxSize(),
        title = "Choose Personas", // TODO crowdin
        subtitle = "Choose the Personas you want to apply this Shield to.", // TODO crowdin
        isButtonEnabled = state.isButtonEnabled,
        isSelectAllVisible = !state.isEmpty,
        selectedAll = state.selectedAll,
        onContinueClick = onContinueClick,
        onDismiss = onDismiss,
        onSelectAllToggleClick = onSelectAllToggleClick
    ) {
        items(state.items) { persona ->
            SimplePersonaSelectionCard(
                modifier = Modifier
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    )
                    .background(
                        color = RadixTheme.colors.gray5,
                        shape = RadixTheme.shapes.roundedRectMedium
                    )
                    .clip(RadixTheme.shapes.roundedRectMedium)
                    .clickable { onSelectPersona(persona.data) }
                    .padding(RadixTheme.dimensions.paddingDefault),
                persona = persona.data,
                checked = persona.selected,
                isSingleChoice = false,
                onSelectPersona = onSelectPersona
            )
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun ChooseAccountsPreview(
    @PreviewParameter(ChoosePersonasPreviewProvider::class) state: ChooseEntityUiState<Persona>
) {
    RadixWalletPreviewTheme {
        ChoosePersonasContent(
            state = state,
            onDismiss = {},
            onSelectAllToggleClick = {},
            onSelectPersona = {},
            onContinueClick = {}
        )
    }
}

@UsesSampleValues
class ChoosePersonasPreviewProvider : PreviewParameterProvider<ChooseEntityUiState<Persona>> {

    override val values: Sequence<ChooseEntityUiState<Persona>>
        get() = sequenceOf(
            ChooseEntityUiState(
                items = listOf(
                    Selectable(Persona.sampleMainnet(), true),
                    Selectable(Persona.sampleStokenet(), false)
                )
            ),
            ChooseEntityUiState(
                items = listOf(
                    Selectable(Persona.sampleMainnet(), true),
                    Selectable(Persona.sampleStokenet(), true)
                )
            ),
            ChooseEntityUiState()
        )
}

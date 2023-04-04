package com.babylon.wallet.android.presentation.settings.personas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.StandardOneLineCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun PersonasScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonasViewModel,
    onBackClick: () -> Unit,
    createNewPersona: (Boolean) -> Unit,
    onPersonaClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is PersonasViewModel.PersonasEvent.CreatePersona -> createNewPersona(it.firstPersonaCreated)
            }
        }
    }
    PersonasContent(
        personas = state.personas,
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        onBackClick = onBackClick,
        createNewPersona = viewModel::onCreatePersona,
        onPersonaClick = onPersonaClick
    )
}

@Composable
fun PersonasContent(
    personas: ImmutableList<Network.Persona>,
    modifier: Modifier,
    onBackClick: () -> Unit,
    createNewPersona: () -> Unit,
    onPersonaClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.background(RadixTheme.colors.defaultBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(id = R.string.personas),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Back
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.all_personas_info),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                InfoLink(stringResource(R.string.what_is_persona), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
            itemsIndexed(items = personas) { _, personaItem ->
                StandardOneLineCard(
                    "",
                    personaItem.displayName,
                    modifier = Modifier
                        .shadow(elevation = 8.dp, shape = RadixTheme.shapes.roundedRectMedium)
                        .clip(RadixTheme.shapes.roundedRectMedium)
                        .throttleClickable {
                            onPersonaClick(personaItem.address)
                        }
                        .fillMaxWidth()
                        .background(
                            RadixTheme.colors.white,
                            shape = RadixTheme.shapes.roundedRectMedium
                        )
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingLarge,
                            vertical = RadixTheme.dimensions.paddingDefault
                        )
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            item {
                RadixSecondaryButton(
                    text = stringResource(id = R.string.create_a_new_persona),
                    onClick = createNewPersona
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

package com.babylon.wallet.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import rdx.works.profile.data.model.pernetwork.OnNetwork

@Composable
fun PersonasScreen(
    modifier: Modifier = Modifier,
    viewModel: PersonasViewModel,
    onBackClick: () -> Unit,
    createNewPersona: () -> Unit
) {
    val state = viewModel.state

    PersonasContent(
        personas = state.personas.toImmutableList(),
        modifier = modifier,
        onBackClick = onBackClick,
        createNewPersona = createNewPersona
    )
}

@Composable
fun PersonasContent(
    personas: ImmutableList<OnNetwork.Persona>,
    modifier: Modifier,
    onBackClick: () -> Unit,
    createNewPersona: () -> Unit
) {
    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(id = R.string.empty),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Back
        )

        Divider()

        Text(
            text = stringResource(id = R.string.all_personas_info),
            style = RadixTheme.typography.body2Link,
            color = RadixTheme.colors.gray2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(PaddingValues(RadixTheme.dimensions.paddingXLarge))
        )

        Divider()

        LazyColumn(
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            itemsIndexed(items = personas) { _, personaItem ->
                Row(
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingMedium)
                        .throttleClickable { /* TODO */ },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                ) {
                    Text(
                        text = personaItem.displayName,
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(
                            id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right
                        ),
                        contentDescription = null,
                        tint = RadixTheme.colors.gray1
                    )
                }
                Divider()
            }

            item {
                RadixPrimaryButton(
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingMedium),
                    text = stringResource(id = R.string.create_a_new_persona),
                    onClick = createNewPersona
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

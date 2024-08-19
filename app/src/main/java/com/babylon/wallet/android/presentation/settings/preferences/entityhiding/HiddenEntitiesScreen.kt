package com.babylon.wallet.android.presentation.settings.preferences.entityhiding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HiddenEntitiesScreen(
    viewModel: HiddenEntitiesViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.Close -> onBackClick()
            }
        }
    }
    HiddenEntitiesContent(
        modifier = modifier,
        onBackClick = onBackClick,
        state = state,
        onShowUnhideAccountAlert = viewModel::showUnhideAccountAlert,
        onShowUnhidePersonaAlert = viewModel::showUnhidePersonaAlert,
        onUnhideSelectedEntity = viewModel::onUnhideSelectedEntity,
        onDismissUnhideAlert = viewModel::onDismissUnhideAlert
    )
}

@Composable
private fun HiddenEntitiesContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    state: State,
    onUnhideSelectedEntity: () -> Unit,
    onShowUnhideAccountAlert: (Account) -> Unit,
    onShowUnhidePersonaAlert: (Persona) -> Unit,
    onDismissUnhideAlert: () -> Unit
) {
    if (state.alertState != State.AlertState.None) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    when (state.alertState) {
                        is State.AlertState.ShowUnhideAccount,
                        is State.AlertState.ShowUnhidePersona -> {
                            onUnhideSelectedEntity()
                        }

                        else -> {}
                    }
                } else {
                    onDismissUnhideAlert()
                }
            },
            message = {
                Text(
                    text = when (state.alertState) {
                        is State.AlertState.ShowUnhideAccount -> "Make this Account visible in your wallet again?" // TODO crowdin
                        is State.AlertState.ShowUnhidePersona -> "Make this Persona visible in your wallet again?" // TODO crowdin
                        State.AlertState.None -> stringResource(id = R.string.empty)
                    },
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.common_confirm)
        )
    }
    Scaffold(modifier = modifier, topBar = {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.appSettings_entityHiding_title),
            onBackClick = onBackClick,
            windowInsets = WindowInsets.statusBarsAndBanner
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(RadixTheme.colors.gray5)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault)
        ) {
            item {
                HorizontalDivider(color = RadixTheme.colors.gray5)
                Text(
                    modifier = Modifier.padding(top = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(R.string.appSettings_entityHiding_text),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
            }
            if (state.hiddenPersonas != null) {
                item {
                    Text(
                        modifier = Modifier.padding(
                            vertical = RadixTheme.dimensions.paddingLarge
                        ),
                        text = "Personas", // TODO crowdin
                        style = RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.gray2
                    )
                }
                if (state.hiddenPersonas.isNotEmpty()) {
                    itemsIndexed(state.hiddenPersonas) { index, item ->
                        val lastItem = index == state.hiddenPersonas.lastIndex
                        PersonaCard(persona = item, onUnhide = onShowUnhidePersonaAlert)
                        if (!lastItem) {
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                        }
                    }
                } else {
                    item {
                        EmptyState()
                    }
                }
            }
            if (state.hiddenAccounts != null) {
                item {
                    Text(
                        modifier = Modifier.padding(
                            vertical = RadixTheme.dimensions.paddingLarge
                        ),
                        text = "Accounts", // TODO crowdin
                        style = RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.gray2
                    )
                }
                if (state.hiddenAccounts.isNotEmpty()) {
                    itemsIndexed(state.hiddenAccounts) { index, item ->
                        val lastItem = index == state.hiddenAccounts.lastIndex
                        AccountCard(account = item, onUnhide = onShowUnhideAccountAlert)
                        if (!lastItem) {
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                        }
                    }
                } else {
                    item {
                        EmptyState()
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(modifier: Modifier = Modifier, account: Account, onUnhide: (Account) -> Unit) {
    Row(
        modifier
            .heightIn(min = 84.dp)
            .defaultCardShadow(elevation = 2.dp)
            .background(
                account.appearanceId.gradient(),
                RadixTheme.shapes.roundedRectSmall
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                modifier = Modifier.padding(end = RadixTheme.dimensions.paddingMedium),
                text = account.displayName.value,
                style = RadixTheme.typography.body1Header,
                maxLines = 1,
                color = RadixTheme.colors.white,
                overflow = TextOverflow.Ellipsis
            )
            ActionableAddressView(
                address = Address.Account(account.address),
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
            )
        }
        RadixSecondaryButton(
            text = "Unhide", // TODO crowdin
            onClick = { onUnhide(account) }
        )
    }
}

@Composable
private fun PersonaCard(modifier: Modifier = Modifier, persona: Persona, onUnhide: (Persona) -> Unit) {
    Row(
        modifier = modifier
            .heightIn(min = 84.dp)
            .defaultCardShadow(elevation = 2.dp)
            .fillMaxWidth()
            .background(RadixTheme.colors.white, shape = RadixTheme.shapes.roundedRectMedium)
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.Persona(
            modifier = Modifier.size(44.dp),
            persona = persona
        )
        Text(
            modifier = Modifier.weight(1f),
            text = persona.displayName.value,
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        RadixSecondaryButton(
            text = "Unhide", // TODO crowdin
            onClick = { onUnhide(persona) }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.gray4, shape = RadixTheme.shapes.roundedRectMedium)
            .padding(vertical = RadixTheme.dimensions.paddingXXXLarge),
        text = stringResource(id = R.string.common_none),
        color = RadixTheme.colors.gray2,
        textAlign = TextAlign.Center,
        style = RadixTheme.typography.secondaryHeader
    )
}

@UsesSampleValues
class HiddenEntitiesPreviewParameterProvider : PreviewParameterProvider<Pair<PersistentList<Account>, PersistentList<Persona>>> {
    override val values: Sequence<Pair<PersistentList<Account>, PersistentList<Persona>>> =
        sequenceOf(
            persistentListOf(Account.sampleMainnet.alice, Account.sampleMainnet.bob) to persistentListOf(
                Persona.sampleMainnet.batman,
                Persona.sampleMainnet.ripley
            ),
            persistentListOf(Account.sampleMainnet.alice, Account.sampleMainnet.bob) to persistentListOf(),
            persistentListOf<Account>() to persistentListOf(),
            persistentListOf<Account>() to persistentListOf(Persona.sampleMainnet.batman, Persona.sampleMainnet.ripley)
        )
}

@Preview(showBackground = true)
@UsesSampleValues
@Composable
private fun HiddenEntitiesContentPreview(
    @PreviewParameter(HiddenEntitiesPreviewParameterProvider::class) parameters: Pair<PersistentList<Account>, PersistentList<Persona>>
) {
    RadixWalletTheme {
        HiddenEntitiesContent(
            onBackClick = {},
            state = State(
                hiddenAccounts = parameters.first,
                hiddenPersonas = parameters.second
            ),
            onShowUnhideAccountAlert = {},
            onShowUnhidePersonaAlert = {},
            onUnhideSelectedEntity = {},
            onDismissUnhideAlert = {}
        )
    }
}

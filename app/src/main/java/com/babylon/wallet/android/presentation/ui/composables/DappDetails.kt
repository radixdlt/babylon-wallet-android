package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DAppWebsiteAddressRow
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DappDefinitionAddressRow
import com.babylon.wallet.android.presentation.ui.composables.card.FungibleCard
import com.babylon.wallet.android.presentation.ui.composables.card.NonFungibleCard
import com.babylon.wallet.android.presentation.ui.composables.card.PersonaCard
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Persona
import kotlinx.collections.immutable.ImmutableList
import rdx.works.core.domain.resources.Resource

@Composable
fun DappDetails(
    modifier: Modifier,
    dAppWithResources: DAppWithResources?,
    isValidatingWebsite: Boolean,
    validatedWebsite: String?,
    personaList: ImmutableList<Persona>,
    onPersonaClick: ((Persona) -> Unit)?,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit,
    onDeleteDapp: () -> Unit
) {
    Column(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            dAppWithResources?.let { dAppWithResources ->
                item {
                    Thumbnail.DApp(
                        modifier = Modifier
                            .size(104.dp),
                        dapp = dAppWithResources.dApp
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                    HorizontalDivider(color = RadixTheme.colors.gray4, modifier = Modifier.padding(horizontal = dimensions.paddingXLarge))
                }
                dAppWithResources.dApp.description?.let { description ->
                    item {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.paddingXLarge, vertical = dimensions.paddingLarge),
                            text = description,
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray1,
                            textAlign = TextAlign.Start
                        )
                        HorizontalDivider(
                            color = RadixTheme.colors.gray4,
                            modifier = Modifier.padding(horizontal = dimensions.paddingXLarge)
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                    DappDefinitionAddressRow(
                        dappDefinitionAddress = dAppWithResources.dApp.dAppAddress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions.paddingXLarge)
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
                if (isValidatingWebsite || validatedWebsite != null) {
                    item {
                        DAppWebsiteAddressRow(
                            website = validatedWebsite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.paddingXLarge)
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                    }
                }
                if (dAppWithResources.fungibleResources.isNotEmpty()) {
                    item {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.paddingXLarge),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_tokens),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                    }
                    itemsIndexed(dAppWithResources.fungibleResources) { index, fungibleToken ->
                        val spacerHeight = if (dAppWithResources.fungibleResources.lastIndex == index) {
                            dimensions.paddingLarge
                        } else {
                            dimensions.paddingDefault
                        }
                        FungibleCard(
                            modifier = Modifier.padding(horizontal = dimensions.paddingDefault),
                            fungible = fungibleToken,
                            showChevron = false,
                            onClick = {
                                onFungibleTokenClick(fungibleToken)
                            }
                        )
                        Spacer(modifier = Modifier.height(spacerHeight))
                    }
                }
                if (dAppWithResources.nonFungibleResources.isNotEmpty()) {
                    item {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.paddingXLarge),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_nfts),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                    }
                    itemsIndexed(dAppWithResources.nonFungibleResources) { index, nonFungibleResource ->
                        val spacerHeight = if (dAppWithResources.nonFungibleResources.lastIndex == index) {
                            dimensions.paddingLarge
                        } else {
                            dimensions.paddingDefault
                        }
                        NonFungibleCard(
                            modifier = Modifier.padding(horizontal = dimensions.paddingDefault),
                            nonFungible = nonFungibleResource,
                            showChevron = false,
                            onClick = {
                                onNonFungibleClick(nonFungibleResource)
                            }
                        )
                        Spacer(modifier = Modifier.height(spacerHeight))
                    }
                }
                item {
                    GrayBackgroundWrapper(contentPadding = PaddingValues(horizontal = dimensions.paddingLarge)) {
                        Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.authorizedDapps_dAppDetails_personasHeading),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                    }
                }
                itemsIndexed(personaList) { index, persona ->
                    val spacerHeight = if (personaList.lastIndex == index) {
                        dimensions.paddingXXXLarge
                    } else {
                        dimensions.paddingDefault
                    }
                    GrayBackgroundWrapper {
                        PersonaCard(
                            modifier = Modifier.applyIf(
                                onPersonaClick != null,
                                Modifier.throttleClickable {
                                    onPersonaClick?.invoke(persona)
                                }
                            ),
                            showChevron = onPersonaClick != null,
                            persona = persona
                        )
                        Spacer(modifier = Modifier.height(spacerHeight))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                    WarningButton(
                        modifier = Modifier
                            .padding(horizontal = dimensions.paddingDefault),
                        text = stringResource(R.string.authorizedDapps_dAppDetails_forgetDapp),
                        onClick = onDeleteDapp
                    )
                }
            }
        }
    }
}
package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.presentation.dialogs.assets.TagsView
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DAppWebsiteAddressRow
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.DappDefinitionAddressRow
import com.babylon.wallet.android.presentation.ui.composables.card.FungibleCard
import com.babylon.wallet.android.presentation.ui.composables.card.NonFungibleCard
import com.babylon.wallet.android.presentation.ui.composables.card.PersonaCard
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Persona
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Tag

@Composable
fun DappDetails(
    modifier: Modifier,
    dAppWithResources: DAppWithResources?,
    isValidatingWebsite: Boolean,
    validatedWebsite: String?,
    personaList: ImmutableList<Persona>,
    isShowLockerDepositsChecked: Boolean,
    isReadOnly: Boolean,
    onPersonaClick: ((Persona) -> Unit)?,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit,
    onDeleteDapp: () -> Unit,
    onShowLockerDepositsCheckedChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        dAppWithResources?.let { dAppWithResources ->
            item {
                Thumbnail.DApp(
                    modifier = Modifier
                        .size(104.dp),
                    dapp = dAppWithResources.dApp
                )
                Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                HorizontalDivider(
                    color = RadixTheme.colors.divider,
                    modifier = Modifier.padding(horizontal = dimensions.paddingXLarge)
                )
            }
            dAppWithResources.dApp.description?.let { description ->
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensions.paddingXLarge,
                                vertical = dimensions.paddingLarge
                            ),
                        text = description,
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.text,
                        textAlign = TextAlign.Start
                    )
                    HorizontalDivider(
                        color = RadixTheme.colors.divider,
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
                    Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                }
            }

            val tags = dAppWithResources.dApp.tags.map { Tag.Dynamic(name = it) }
            if (tags.isNotEmpty()) {
                item {
                    TagsSectionView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions.paddingXLarge),
                        tags = tags.toPersistentList()
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                }
            }

            if (dAppWithResources.hasAnyResources) {
                item {
                    HorizontalDivider(color = RadixTheme.colors.divider)
                }
            }

            if (dAppWithResources.fungibleResources.isNotEmpty()) {
                item {
                    GrayBackgroundWrapper(
                        contentPadding = PaddingValues(
                            vertical = dimensions.paddingDefault,
                            horizontal = dimensions.paddingXLarge
                        ) + PaddingValues(top = dimensions.paddingSmall)
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_tokens),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.textSecondary,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                itemsIndexed(dAppWithResources.fungibleResources) { index, fungibleToken ->
                    GrayBackgroundWrapper(
                        contentPadding = PaddingValues(
                            horizontal = dimensions.paddingDefault
                        ) + PaddingValues(top = dimensions.paddingSmall)
                    ) {
                        FungibleCard(
                            fungible = fungibleToken,
                            showChevron = false,
                            onClick = {
                                onFungibleTokenClick(fungibleToken)
                            }
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                    }
                }
            }
            if (dAppWithResources.nonFungibleResources.isNotEmpty()) {
                item {
                    GrayBackgroundWrapper(
                        contentPadding = PaddingValues(
                            vertical = dimensions.paddingDefault,
                            horizontal = dimensions.paddingXLarge
                        )
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.authorizedDapps_dAppDetails_nfts),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.textSecondary,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                itemsIndexed(dAppWithResources.nonFungibleResources) { index, nonFungibleResource ->
                    GrayBackgroundWrapper(
                        contentPadding = PaddingValues(
                            horizontal = dimensions.paddingDefault
                        )
                    ) {
                        NonFungibleCard(
                            nonFungible = nonFungibleResource,
                            showChevron = false,
                            onClick = {
                                onNonFungibleClick(nonFungibleResource)
                            }
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                    }
                }
            }

            item {
                HorizontalDivider(
                    color = RadixTheme.colors.divider,
                )
            }

            if (personaList.isNotEmpty()) {
                item {
                    GrayBackgroundWrapper(contentPadding = PaddingValues(horizontal = dimensions.paddingLarge)) {
                        Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.authorizedDapps_dAppDetails_personasHeading),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingLarge))
                    }
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
            if (!isReadOnly) {
                item {
                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))

                    SwitchSettingsItem(
                        modifier = Modifier
                            .background(RadixTheme.colors.background)
                            .fillMaxWidth()
                            .padding(horizontal = dimensions.paddingDefault),
                        titleRes = R.string.authorizedDapps_dAppDetails_depositsTitle,
                        subtitleRes = if (isShowLockerDepositsChecked) {
                            R.string.authorizedDapps_dAppDetails_depositsVisible
                        } else {
                            R.string.authorizedDapps_dAppDetails_depositsHidden
                        },
                        icon = null,
                        subtitleTextColor = RadixTheme.colors.textSecondary,
                        checked = isShowLockerDepositsChecked,
                        onCheckedChange = onShowLockerDepositsCheckedChange
                    )

                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))

                    HorizontalDivider(
                        color = RadixTheme.colors.divider,
                        modifier = Modifier.padding(horizontal = dimensions.paddingDefault)
                    )

                    Spacer(modifier = Modifier.height(dimensions.paddingLarge))

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

@Composable
private fun TagsSectionView(
    tags: ImmutableList<Tag>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.assetDetails_tags),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(dimensions.paddingDefault))

        TagsView(
            modifier = Modifier.fillMaxWidth(),
            tags = tags
        )
    }
}

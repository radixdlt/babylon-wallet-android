package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.securityshields.EmergencyFallbackView
import com.babylon.wallet.android.presentation.common.securityshields.OrView
import com.babylon.wallet.android.presentation.common.securityshields.display
import com.babylon.wallet.android.presentation.dialogs.info.DSR
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ConfirmationRoleWithFactorSources
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PrimaryRoleWithFactorSources
import com.radixdlt.sargon.RecoveryRoleWithFactorSources
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSample
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSampleOther
import com.radixdlt.sargon.samples.sampleMainnet

@Composable
fun SecurifyEntityTypeContent(
    preview: PreviewType.UpdateSecurityStructure,
    onInfoClick: (GlossaryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RadixTheme.dimensions.paddingDefault)
    ) {
        SectionTitle(
            title = when (preview.operation) {
                PreviewType.UpdateSecurityStructure.Operation.ApplySecurityStructure,
                PreviewType.UpdateSecurityStructure.Operation.UpdateSecurityStructure,
                PreviewType.UpdateSecurityStructure.Operation.ConfirmRecovery -> stringResource(
                    id = R.string.transactionReview_updateShield_sectionTitle
                )

                PreviewType.UpdateSecurityStructure.Operation.StopRecovery -> "Stop Timed Recovery" // TODO crowdin
            },
            iconRes = DSR.ic_entity_update_shield
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                .background(
                    color = RadixTheme.colors.background,
                    shape = RadixTheme.shapes.roundedRectDefault
                )
                .padding(RadixTheme.dimensions.paddingMedium),
        ) {
            when (val entity = preview.entity) {
                is ProfileEntity.AccountEntity -> AccountCardHeader(
                    account = InvolvedAccount.Owned(entity.account)
                )

                is ProfileEntity.PersonaEntity -> PersonaCardHeader(
                    persona = entity.persona,
                    containerColor = RadixTheme.colors.card
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.backgroundSecondary,
                        shape = RadixTheme.shapes.roundedRectBottomMedium
                    )
            ) {
                when (preview.operation) {
                    PreviewType.UpdateSecurityStructure.Operation.ApplySecurityStructure,
                    PreviewType.UpdateSecurityStructure.Operation.UpdateSecurityStructure,
                    PreviewType.UpdateSecurityStructure.Operation.ConfirmRecovery -> preview.provisionalConfig?.let { config ->
                        config.metadata.displayName.value.takeIf { it.isNotEmpty() }?.let {
                            Text(
                                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                                text = stringResource(
                                    R.string.transactionReview_updateShield_applyTitle,
                                    it
                                ),
                                style = RadixTheme.typography.body1Header,
                                color = RadixTheme.colors.text
                            )

                            HorizontalDivider(color = RadixTheme.colors.divider)
                        }

                        ShieldConfigView(
                            securityStructure = config,
                            onInfoClick = onInfoClick
                        )
                    }

                    PreviewType.UpdateSecurityStructure.Operation.StopRecovery -> {
                        Row(
                            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                        ) {
                            Icon(
                                painter = painterResource(id = DSR.ic_close),
                                contentDescription = null,
                                tint = RadixTheme.colors.icon
                            )

                            Text(
                                text = "Stopping timed recovery. Your current security setup will remain unchanged.", // TODo crowdin
                                style = RadixTheme.typography.body1Header,
                                color = RadixTheme.colors.text
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShieldConfigView(
    securityStructure: SecurityStructureOfFactorSources,
    modifier: Modifier = Modifier,
    onFactorClick: (FactorSourceId) -> Unit = {},
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        PrimaryView(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
            primary = securityStructure.matrixOfFactors.primaryRole,
            onFactorClick = onFactorClick
        )

        HorizontalDivider(color = RadixTheme.colors.divider)

        ProveOwnershipView(
            modifier = Modifier.padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingSemiLarge
            ),
            authenticationSigningFactor = securityStructure.authenticationSigningFactor,
            onFactorClick = onFactorClick
        )

        HorizontalDivider(color = RadixTheme.colors.divider)

        RecoveryAndConfirmationView(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .padding(
                    top = RadixTheme.dimensions.paddingSemiLarge,
                    bottom = RadixTheme.dimensions.paddingDefault
                ),
            recovery = securityStructure.matrixOfFactors.recoveryRole,
            confirmation = securityStructure.matrixOfFactors.confirmationRole,
            confirmationDelay = securityStructure.matrixOfFactors.timeUntilDelayedConfirmationIsCallable,
            onFactorClick = onFactorClick,
            onInfoClick = onInfoClick
        )
    }
}

@Composable
private fun PrimaryView(
    primary: PrimaryRoleWithFactorSources,
    onFactorClick: (FactorSourceId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.transactionReview_updateShield_regularAccessTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(R.string.transactionReview_updateShield_regularAccessMessage),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            text = stringResource(
                R.string.transactionReview_updateShield_primaryThersholdMessage,
                primary.threshold.display()
            ).formattedSpans(
                SpanStyle(fontWeight = FontWeight.Bold)
            ),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        primary.thresholdFactors.forEachIndexed { index, factorSource ->
            FactorSourceCardView(
                modifier = Modifier.throttleClickable { onFactorClick(factorSource.id) },
                item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                castsShadow = false,
                isOutlined = true
            )

            if (index != primary.thresholdFactors.lastIndex) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            }
        }

        if (primary.overrideFactors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Text(
                text = stringResource(
                    R.string.transactionReview_updateShield_primaryOverrideMessage,
                ).formattedSpans(
                    SpanStyle(fontWeight = FontWeight.Bold)
                ),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.text
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            primary.overrideFactors.forEachIndexed { index, factorSource ->
                FactorSourceCardView(
                    modifier = Modifier.throttleClickable { onFactorClick(factorSource.id) },
                    item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                    castsShadow = false,
                    isOutlined = true
                )

                if (index != primary.overrideFactors.lastIndex) {
                    OrView()
                }
            }
        }
    }
}

@Composable
private fun ProveOwnershipView(
    modifier: Modifier,
    authenticationSigningFactor: FactorSource,
    onFactorClick: (FactorSourceId) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.transactionReview_updateShield_authSigningTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(R.string.transactionReview_updateShield_authSigningMessage),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            text = stringResource(R.string.transactionReview_updateShield_authSigningThreshold),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        FactorSourceCardView(
            modifier = Modifier.throttleClickable { onFactorClick(authenticationSigningFactor.id) },
            item = authenticationSigningFactor.toFactorSourceCard(includeLastUsedOn = false),
            castsShadow = false,
            isOutlined = true
        )
    }
}

@Composable
private fun RecoveryAndConfirmationView(
    modifier: Modifier = Modifier,
    recovery: RecoveryRoleWithFactorSources,
    confirmation: ConfirmationRoleWithFactorSources,
    confirmationDelay: TimePeriod,
    onFactorClick: (FactorSourceId) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier,
            text = stringResource(R.string.transactionReview_updateShield_startConfirmTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(R.string.transactionReview_updateShield_startConfirmMessage),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        HorizontalDivider(
            color = RadixTheme.colors.divider
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier,
            text = stringResource(R.string.transactionReview_updateShield_startRecoveryTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(
                R.string.transactionReview_updateShield_nonPrimaryOverrideMessage
            ).formattedSpans(
                SpanStyle(fontWeight = FontWeight.Bold)
            ),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        recovery.overrideFactors.forEachIndexed { index, factorSource ->
            FactorSourceCardView(
                modifier = Modifier.throttleClickable { onFactorClick(factorSource.id) },
                item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                castsShadow = false,
                isOutlined = true
            )

            if (index != recovery.overrideFactors.lastIndex) {
                OrView()
            }
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        HorizontalDivider(color = RadixTheme.colors.divider)

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier,
            text = stringResource(R.string.transactionReview_updateShield_confirmRecoveryTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(
                R.string.transactionReview_updateShield_nonPrimaryOverrideMessage
            ).formattedSpans(
                SpanStyle(fontWeight = FontWeight.Bold)
            ),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        confirmation.overrideFactors.forEachIndexed { index, factorSource ->
            FactorSourceCardView(
                modifier = Modifier.throttleClickable { onFactorClick(factorSource.id) },
                item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                castsShadow = false,
                isOutlined = true
            )

            if (index != confirmation.overrideFactors.lastIndex) {
                OrView()
            }
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        HorizontalDivider(color = RadixTheme.colors.divider)

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        EmergencyFallbackView(
            delay = confirmationDelay,
            showHeader = false,
            description = AnnotatedString(stringResource(R.string.transactionReview_updateShield_confirmationDelayMessage)),
            note = null,
            onInfoClick = onInfoClick
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun SecurifyEntityTypeForAccountPreview() {
    RadixWalletPreviewTheme {
        SecurifyEntityTypeContent(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            preview = PreviewType.UpdateSecurityStructure(
                entity = ProfileEntity.AccountEntity(Account.sampleMainnet()),
                provisionalConfig = newSecurityStructureOfFactorSourcesSample(),
                operation = PreviewType.UpdateSecurityStructure.Operation.StopRecovery
            ),
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun SecurifyEntityTypeForPersonaPreview() {
    RadixWalletPreviewTheme {
        SecurifyEntityTypeContent(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            preview = PreviewType.UpdateSecurityStructure(
                entity = ProfileEntity.PersonaEntity(Persona.sampleMainnet()),
                provisionalConfig = newSecurityStructureOfFactorSourcesSampleOther(),
                operation = PreviewType.UpdateSecurityStructure.Operation.ApplySecurityStructure
            ),
            onInfoClick = {}
        )
    }
}

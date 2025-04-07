package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.securityshields.ConfirmationDelay
import com.babylon.wallet.android.presentation.common.securityshields.OrView
import com.babylon.wallet.android.presentation.common.securityshields.display
import com.babylon.wallet.android.presentation.dialogs.info.DSR
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ConfirmationRoleWithFactorSources
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PrimaryRoleWithFactorSources
import com.radixdlt.sargon.RecoveryRoleWithFactorSources
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSample
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSampleOther
import com.radixdlt.sargon.samples.sampleMainnet

@Composable
fun SecurifyEntityTypeContent(
    modifier: Modifier = Modifier,
    preview: PreviewType.SecurifyEntity
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RadixTheme.dimensions.paddingDefault)
    ) {
        SectionTitle(
            titleRes = R.string.transactionReview_updateShield_sectionTitle,
            iconRes = DSR.ic_entity_update_shield
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                .background(
                    color = RadixTheme.colors.defaultBackground,
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
                    containerColor = RadixTheme.colors.white
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.gray5,
                        shape = RadixTheme.shapes.roundedRectBottomMedium
                    )
            ) {
                ShieldConfigTitle(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    structure = preview.provisionalConfig
                )

                HorizontalDivider(color = RadixTheme.colors.gray3)

                PrimaryView(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    primary = preview.provisionalConfig.matrixOfFactors.primaryRole
                )

                HorizontalDivider(color = RadixTheme.colors.gray3)

                ProveOwnershipView(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingSemiLarge
                    ),
                    authenticationSigningFactor = preview.provisionalConfig.authenticationSigningFactor
                )

                HorizontalDivider(color = RadixTheme.colors.gray3)

                RecoveryAndConfirmationView(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(
                            top = RadixTheme.dimensions.paddingSemiLarge,
                            bottom = RadixTheme.dimensions.paddingXXXLarge
                        ),
                    recovery = preview.provisionalConfig.matrixOfFactors.recoveryRole,
                    confirmation = preview.provisionalConfig.matrixOfFactors.confirmationRole,
                    confirmationDelay = preview.provisionalConfig.matrixOfFactors.timeUntilDelayedConfirmationIsCallable
                )
            }
        }
    }
}

@Composable
private fun ShieldConfigTitle(
    modifier: Modifier = Modifier,
    structure: SecurityStructureOfFactorSources
) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.transactionReview_updateShield_applyTitle, structure.metadata.displayName.value),
        style = RadixTheme.typography.secondaryHeader,
        color = RadixTheme.colors.gray1
    )
}

@Composable
private fun PrimaryView(
    modifier: Modifier = Modifier,
    primary: PrimaryRoleWithFactorSources
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.transactionReview_updateShield_regularAccessTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(R.string.transactionReview_updateShield_regularAccessMessage),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray2
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
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        primary.thresholdFactors.forEachIndexed { index, factorSource ->
            FactorSourceCardView(
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
                color = RadixTheme.colors.gray1
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            primary.overrideFactors.forEachIndexed { index, factorSource ->
                FactorSourceCardView(
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
    authenticationSigningFactor: FactorSource
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.transactionReview_updateShield_authSigningTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(R.string.transactionReview_updateShield_authSigningMessage),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray2
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            text = stringResource(R.string.transactionReview_updateShield_authSigningThreshold),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        FactorSourceCardView(
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
    confirmationDelay: TimePeriod
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier,
            text = stringResource(R.string.transactionReview_updateShield_startConfirmTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(R.string.transactionReview_updateShield_startConfirmMessage),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray2
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        HorizontalDivider(
            color = RadixTheme.colors.gray3
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier,
            text = stringResource(R.string.transactionReview_updateShield_startRecoveryTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(
                R.string.transactionReview_updateShield_nonPrimaryOverrideMessage
            ).formattedSpans(
                SpanStyle(fontWeight = FontWeight.Bold)
            ),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        recovery.overrideFactors.forEachIndexed { index, factorSource ->
            FactorSourceCardView(
                item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                castsShadow = false,
                isOutlined = true
            )

            if (index != recovery.overrideFactors.lastIndex) {
                OrView()
            }
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        HorizontalDivider(color = RadixTheme.colors.gray3)

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier,
            text = stringResource(R.string.transactionReview_updateShield_confirmRecoveryTitle),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = stringResource(
                R.string.transactionReview_updateShield_nonPrimaryOverrideMessage
            ).formattedSpans(
                SpanStyle(fontWeight = FontWeight.Bold)
            ),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        confirmation.overrideFactors.forEachIndexed { index, factorSource ->
            FactorSourceCardView(
                item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                castsShadow = false,
                isOutlined = true
            )

            if (index != confirmation.overrideFactors.lastIndex) {
                OrView()
            }
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        HorizontalDivider(color = RadixTheme.colors.gray3)

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        ConfirmationDelay(
            delay = confirmationDelay
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
            preview = PreviewType.SecurifyEntity(
                entity = ProfileEntity.AccountEntity(Account.sampleMainnet()),
                provisionalConfig = newSecurityStructureOfFactorSourcesSample()
            )
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
            preview = PreviewType.SecurifyEntity(
                entity = ProfileEntity.PersonaEntity(Persona.sampleMainnet()),
                provisionalConfig = newSecurityStructureOfFactorSourcesSampleOther()
            )
        )
    }
}

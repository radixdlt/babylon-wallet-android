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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.DSR
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.recovery.title
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.Threshold
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSample
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSampleOther
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

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
                    persona = entity.persona
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

                with(preview.provisionalConfig.matrixOfFactors.primaryRole) {
                    RoleView(
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                        title = stringResource(R.string.transactionReview_updateShield_regularAccessTitle),
                        message = stringResource(R.string.transactionReview_updateShield_regularAccessMessage),
                        threshold = threshold,
                        thresholdFactors = remember(thresholdFactors) { thresholdFactors.toImmutableList() },
                        overrideFactors = remember(overrideFactors) { overrideFactors.toImmutableList() }
                    )
                }

                HorizontalDivider(color = RadixTheme.colors.gray3)

                Text(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(top = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(R.string.transactionReview_updateShield_startConfirmTitle),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(R.string.transactionReview_updateShield_startConfirmMessage),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    color = RadixTheme.colors.gray3
                )

                with(preview.provisionalConfig.matrixOfFactors.recoveryRole) {
                    RoleView(
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                        title = stringResource(R.string.transactionReview_updateShield_startRecoveryTitle),
                        message = null,
                        threshold = threshold,
                        thresholdFactors = remember(thresholdFactors) { thresholdFactors.toImmutableList() },
                        overrideFactors = remember(overrideFactors) { overrideFactors.toImmutableList() }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    color = RadixTheme.colors.gray3
                )

                with(preview.provisionalConfig.matrixOfFactors.confirmationRole) {
                    RoleView(
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                        title = stringResource(R.string.transactionReview_updateShield_confirmRecoveryTitle),
                        message = null,
                        threshold = threshold,
                        thresholdFactors = remember(thresholdFactors) { thresholdFactors.toImmutableList() },
                        overrideFactors = remember(overrideFactors) { overrideFactors.toImmutableList() }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    color = RadixTheme.colors.gray3
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                EmergencyFallback(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingDefault
                    ),
                    delay = preview.provisionalConfig.matrixOfFactors.timeUntilDelayedConfirmationIsCallable
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
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
private fun RoleView(
    modifier: Modifier = Modifier,
    title: String,
    message: String?,
    threshold: Threshold,
    thresholdFactors: ImmutableList<FactorSource>,
    overrideFactors: ImmutableList<FactorSource>
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        message?.let {
            Text(
                text = it,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }

        if (thresholdFactors.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.transactionReview_updateShield_threshold,
                    threshold.display()
                ).formattedSpans(
                    SpanStyle(fontWeight = FontWeight.Bold)
                ),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            thresholdFactors.forEachIndexed { index, factorSource ->
                FactorSourceCardView(
                    item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                    castsShadow = false,
                    isOutlined = true
                )

                if (index != thresholdFactors.lastIndex) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    HorizontalDivider(color = RadixTheme.colors.gray3)

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
        }

        if (overrideFactors.isNotEmpty()) {
            if (thresholdFactors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }

            Text(
                text = stringResource(
                    R.string.transactionReview_updateShield_threshold,
                    "ANY"
                ).formattedSpans(
                    SpanStyle(fontWeight = FontWeight.Bold)
                ),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            overrideFactors.forEachIndexed { index, factorSource ->
                FactorSourceCardView(
                    item = factorSource.toFactorSourceCard(includeLastUsedOn = false),
                    castsShadow = false,
                    isOutlined = true
                )

                if (index != overrideFactors.lastIndex) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    HorizontalDivider(color = RadixTheme.colors.gray3)

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
        }
    }
}

@Composable
private fun EmergencyFallback(
    modifier: Modifier = Modifier,
    delay: TimePeriod
) {
    Column(
        modifier = modifier
            .background(
                color = RadixTheme.colors.lightRed,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Text(
            text = stringResource(R.string.transactionReview_updateShield_fallbackMessage),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.white,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
                .padding(
                    horizontal = RadixTheme.dimensions.paddingSemiLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_calendar),
                contentDescription = null
            )

            Text(
                text = delay.title(),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        }
    }
}

@Composable
private fun Threshold.display(): String = when (this) {
    is Threshold.All -> "ALL"
    is Threshold.Specific -> "${v1.toInt()}"
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

package com.babylon.wallet.android.presentation.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun NPSSurveySheet(
    modifier: Modifier = Modifier,
    reason: String?,
    onReasonChanged: (String) -> Unit,
    onScoreClick: (SurveyScore) -> Unit,
    isSubmitButtonEnabled: Boolean,
    scores: ImmutableList<Selectable<SurveyScore>>,
    isLoading: Boolean,
    onSubmitClick: () -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(RadixTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.survey_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge
                    ),
                text = stringResource(id = R.string.survey_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            RatingContainer(
                scores = scores,
                onScoreClick = onScoreClick
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.survey_lowestScoreLabel),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.textSecondary,
                )
                Text(
                    text = stringResource(id = R.string.survey_highestScoreLabel),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            HorizontalDivider(color = RadixTheme.colors.divider)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            Text(
                text = stringResource(id = R.string.survey_reason_heading),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Text(
                text = stringResource(id = R.string.common_optional),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary,
            )

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingDefault),
                onValueChanged = onReasonChanged,
                value = reason.orEmpty(),
                hint = stringResource(id = R.string.survey_reason_fieldHint),
                singleLine = true
            )
        }
        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingMedium),
            color = RadixTheme.colors.divider
        )
        RadixPrimaryButton(
            text = stringResource(id = R.string.survey_submitButton),
            onClick = onSubmitClick,
            modifier = Modifier
                .padding(RadixTheme.dimensions.paddingDefault)
                .fillMaxWidth(),
            enabled = isSubmitButtonEnabled,
            isLoading = isLoading
        )
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingContainer(
    modifier: Modifier = Modifier,
    scores: ImmutableList<Selectable<SurveyScore>>,
    onScoreClick: (SurveyScore) -> Unit
) {
    Column(
        modifier = modifier
            .padding(
                vertical = RadixTheme.dimensions.paddingXLarge,
            )
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            maxItemsInEachRow = 6
        ) {
            scores.forEach { selectableScore ->
                val backgroundColor = if (selectableScore.selected) {
                    RadixTheme.colors.icon
                } else {
                    RadixTheme.colors.background
                }
                val contentColor = if (selectableScore.selected) {
                    RadixTheme.colors.background
                } else {
                    RadixTheme.colors.text
                }

                Box(
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingXXSmall)
                        .clip(CircleShape)
                        .border(1.dp, Color.Black, CircleShape)
                        .size(45.dp)
                        .background(backgroundColor)
                        .clickable { onScoreClick(selectableScore.data) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectableScore.data.score.toString(),
                        color = contentColor,
                        style = RadixTheme.typography.body1HighImportance
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun NPSSurveySheetPreviewLight() {
    RadixWalletPreviewTheme {
        NPSSurveySheet(
            onSubmitClick = {},
            reason = "",
            onReasonChanged = {},
            onScoreClick = {},
            isLoading = false,
            isSubmitButtonEnabled = true,
            scores = persistentListOf(
                Selectable(SurveyScore(score = 0)),
                Selectable(SurveyScore(score = 1)),
                Selectable(SurveyScore(score = 2), selected = true)
            )
        )
    }
}

@Preview
@Composable
fun NPSSurveySheetPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        NPSSurveySheet(
            onSubmitClick = {},
            reason = "",
            onReasonChanged = {},
            onScoreClick = {},
            isLoading = false,
            isSubmitButtonEnabled = true,
            scores = persistentListOf(
                Selectable(SurveyScore(score = 0)),
                Selectable(SurveyScore(score = 1)),
                Selectable(SurveyScore(score = 2), selected = true)
            )
        )
    }
}

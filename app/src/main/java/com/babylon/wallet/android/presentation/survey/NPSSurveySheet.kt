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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Selectable
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
            .navigationBarsPadding()
            .fillMaxWidth()
            .background(RadixTheme.colors.defaultBackground)
            .verticalScroll(rememberScrollState()),
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
                color = RadixTheme.colors.gray1,
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
                color = RadixTheme.colors.gray1,
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
                    color = RadixTheme.colors.gray2,
                )
                Text(
                    text = stringResource(id = R.string.survey_highestScoreLabel),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            HorizontalDivider(color = RadixTheme.colors.gray4)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            Text(
                text = stringResource(id = R.string.survey_reason_heading),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Text(
                text = stringResource(id = R.string.common_optional),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2,
            )

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingDefault),
                onValueChanged = onReasonChanged,
                value = reason.orEmpty(),
                hint = stringResource(id = R.string.survey_reason_fieldHint),
                hintColor = RadixTheme.colors.gray2,
                singleLine = true
            )
        }
        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingMedium),
            color = RadixTheme.colors.gray4
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
                val backgroundColor = if (selectableScore.selected) RadixTheme.colors.gray1 else RadixTheme.colors.white
                val contentColor = if (selectableScore.selected) RadixTheme.colors.white else Color.Black

                Box(
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingXSmall)
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
fun NPSSurveySheetPreview() {
    RadixWalletTheme {
        NPSSurveySheet(
            onSubmitClick = {},
            reason = "",
            onReasonChanged = {},
            onScoreClick = {},
            isLoading = false,
            isSubmitButtonEnabled = true,
            scores = persistentListOf()
        )
    }
}

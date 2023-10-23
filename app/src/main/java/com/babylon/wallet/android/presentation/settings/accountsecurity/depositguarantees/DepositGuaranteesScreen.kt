package com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar

@Composable
fun DepositGuaranteesScreen(
    modifier: Modifier = Modifier,
    viewModel: DepositGuaranteesViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DepositGuaranteesContent(
        modifier = modifier,
        state = state,
        onDepositGuaranteeChanged = viewModel::onDepositGuaranteeChanged,
        onDepositGuaranteeIncreased = viewModel::onDepositGuaranteeIncreased,
        onDepositGuaranteeDecreased = viewModel::onDepositGuaranteeDecreased,
        onBackClick = onBackClick
    )
}

@Composable
fun DepositGuaranteesContent(
    modifier: Modifier = Modifier,
    state: DepositGuaranteesViewModel.State,
    onDepositGuaranteeChanged: (String) -> Unit,
    onDepositGuaranteeIncreased: () -> Unit,
    onDepositGuaranteeDecreased: () -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.settings_depositGuarantees_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.settings_depositGuarantees_text),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

//                InfoLink( // TODO Uncomment when showing Infolinks
//                    text = stringResource(R.string.transactionReview_guarantees_howDoGuaranteesWork),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
//                )
//                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                        .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                        .background(
                            color = Color.White,
                            shape = RadixTheme.shapes.roundedRectDefault
                        )
                        .padding(RadixTheme.dimensions.paddingDefault),
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(
                            id = R.string.transactionReview_guarantees_setGuaranteedMinimum
                        ).replace("%%", "%"),
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            modifier = Modifier.weight(0.7f),
                            onClick = onDepositGuaranteeDecreased
                        ) {
                            Icon(
                                painterResource(
                                    id = R.drawable.ic_minus
                                ),
                                tint = RadixTheme.colors.gray1,
                                contentDescription = "minus button"
                            )
                        }

                        RadixTextField(
                            modifier = Modifier.weight(1.1f),
                            onValueChanged = { value ->
                                onDepositGuaranteeChanged(value)
                            },
                            value = state.depositGuarantee.orEmpty(),
                            errorHighlight = !state.isDepositInputValid,
                            singleLine = true,
                            textStyle = RadixTheme.typography.body1Regular.copy(textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Number
                            )
                        )

                        IconButton(
                            modifier = Modifier.weight(0.7f),
                            onClick = onDepositGuaranteeIncreased
                        ) {
                            Icon(
                                painterResource(
                                    id = R.drawable.ic_plus
                                ),
                                tint = RadixTheme.colors.gray1,
                                contentDescription = "plus button"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DepositGuaranteesContentPreview() {
    RadixWalletTheme {
        DepositGuaranteesContent(
            state = DepositGuaranteesViewModel.State(
                isDepositInputValid = true,
                depositGuarantee = "100"
            ),
            onDepositGuaranteeChanged = {},
            onDepositGuaranteeIncreased = {},
            onDepositGuaranteeDecreased = {},
            onBackClick = {},
        )
    }
}

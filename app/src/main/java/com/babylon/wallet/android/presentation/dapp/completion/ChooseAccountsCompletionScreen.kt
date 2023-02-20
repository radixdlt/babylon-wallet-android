package com.babylon.wallet.android.presentation.dapp.completion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixBackground
import com.babylon.wallet.android.designsystem.theme.RadixButtonBackground
import com.babylon.wallet.android.designsystem.theme.RadixGrey2
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White

@Composable
fun ChooseAccountsCompletionScreen(
    viewModel: ChooseAccountsCompletionViewModel,
    onContinueClick: () -> Unit
) {
    val dAppName = viewModel.dAppName

    ChooseAccountsCompletionContent(
        dAppName = dAppName,
        onContinueClick = onContinueClick
    )
}

@Composable
private fun ChooseAccountsCompletionContent(
    dAppName: String,
    modifier: Modifier = Modifier,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 50.dp, vertical = RadixTheme.dimensions.paddingSmall),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Image(
            painter = painterResource(R.drawable.img_dapp_complete),
            contentDescription = "dapp_complete_image"
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(id = R.string.dapp_successful_title),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            color = RadixGrey2,
            text = stringResource(id = R.string.dapp_successful_body, dAppName),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.weight(1f))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = RadixTheme.dimensions.paddingXLarge),
            onClick = { onContinueClick() },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = RadixButtonBackground,
                disabledBackgroundColor = RadixBackground
            )
        ) {
            Text(
                color = White,
                text = stringResource(id = R.string.dapp_successful_button_title, dAppName),
                modifier = Modifier.padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingSmall
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun ChooseAccountsCompletionContentPreview() {
    ChooseAccountsCompletionContent(
        dAppName = "Radaswap",
        onContinueClick = {}
    )
}

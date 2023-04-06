package com.babylon.wallet.android.presentation.dapp.success

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetWrapper

@Composable
fun RequestResultSuccessScreen(
    viewModel: RequestResultSuccessViewModel,
    requestId: String,
    dAppName: String,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissHandler = {
        viewModel.incomingRequestHandled(requestId)
        onBackPress()
    }
    BottomSheetWrapper(onDismissRequest = dismissHandler, scrimColor = Color.Transparent) {
        RequestSuccessDialog(
            dAppName = dAppName,
            modifier = modifier
        )
    }
}

@Composable
private fun RequestSuccessDialog(
    dAppName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.defaultBackground,
                shape = RadixTheme.shapes.roundedRectTopDefault
            )
            .clip(RadixTheme.shapes.roundedRectTopDefault)
            .padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Image(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
            ),
            contentDescription = null
        )
        Text(
            text = stringResource(id = R.string.success),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Text(
            text = stringResource(id = R.string.request_complete, dAppName),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RequestResultSuccessScreenPreview() {
    RadixWalletTheme {
        RequestSuccessDialog("dApp")
    }
}

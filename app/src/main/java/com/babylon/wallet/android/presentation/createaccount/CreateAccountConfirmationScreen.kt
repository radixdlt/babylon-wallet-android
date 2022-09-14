package com.babylon.wallet.android.presentation.createaccount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.composables.AccountAddressView
import com.babylon.wallet.android.presentation.ui.theme.RadixBackground
import com.babylon.wallet.android.presentation.ui.theme.RadixButtonBackground
import com.babylon.wallet.android.presentation.ui.theme.White

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun CreateAccountConfirmationScreen(
    viewModel: CreateAccountConfirmationViewModel,
    goHomeClick: () -> Unit
) {
    val accountState = viewModel.accountUiState.collectAsStateWithLifecycle().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Image(
            painter = painterResource(R.drawable.img_account_creation),
            contentDescription = "account_creation_image"
        )
        Spacer(modifier = Modifier.height(45.dp))
        Text(
            text = stringResource(id = R.string.congratulations),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.you_created_account),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(60.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            // TODO this is different gray so will have to be unified, marking as TODO
            backgroundColor = Color(0xFFD9D9D9),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = accountState.first,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(15.dp))
                AccountAddressView(
                    address = accountState.second,
                    onCopyAccountAddressClick = {}
                )
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp),
            text = stringResource(id = R.string.account_created_info),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(Modifier.weight(1f))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { goHomeClick() },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = RadixButtonBackground,
                disabledBackgroundColor = RadixBackground
            )
        ) {
            Text(
                color = White,
                text = stringResource(id = R.string.go_to_home),
                modifier = Modifier.padding(26.dp, 8.dp, 26.dp, 8.dp)
            )
        }
    }

    // Disable back button
    BackHandler(enabled = true) { }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun CreateAccountConfirmationPreview() {
    val viewModel = CreateAccountConfirmationViewModel(
        savedStateHandle = SavedStateHandle()
    )
    CreateAccountConfirmationScreen(
        viewModel = viewModel,
        goHomeClick = {}
    )
}

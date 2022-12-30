package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixBackground
import com.babylon.wallet.android.designsystem.theme.RadixButtonBackground
import com.babylon.wallet.android.designsystem.theme.RadixGrey2
import com.babylon.wallet.android.designsystem.theme.White

@Suppress("UnstableCollections")
@Composable
fun ChooseAccountContent(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    imageUrl: String,
    continueButtonEnabled: Boolean,
    accounts: List<SelectedAccountUiState>,
    onAccountSelect: (SelectedAccountUiState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "clear"
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 50.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl,
                    placeholder = painterResource(id = R.drawable.img_placeholder),
                    error = painterResource(id = R.drawable.img_placeholder)
                ),
                contentDescription = "choose_dapp_login_image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(id = R.string.choose_dapp_accounts_title),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                color = RadixGrey2,
                text = stringResource(id = R.string.choose_dapp_accounts_body),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(40.dp))

            Column {
                accounts.forEach { dAppAccount ->
                    AccountCard(
                        accountName = dAppAccount.accountName,
                        hashValue = dAppAccount.accountAddress,
                        accountCurrency = dAppAccount.accountCurrency,
                        accountValue = dAppAccount.accountValue,
                        checked = dAppAccount.selected,
                        onCheckedChange = {
                            onAccountSelect(dAppAccount)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            TextButton(
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialTheme.colors.onBackground
                ),
                onClick = { /* TODO */ }
            ) {
                Text(
                    text = stringResource(id = R.string.create_dapp_accounts_button_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 30.dp),
                onClick = { onContinueClick() },
                enabled = continueButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = RadixButtonBackground,
                    disabledBackgroundColor = RadixBackground
                )
            ) {
                Text(
                    color = White,
                    text = stringResource(id = R.string.continue_button_title),
                    modifier = Modifier.padding(26.dp, 8.dp, 26.dp, 8.dp)
                )
            }
        }
    }
}

@Composable
fun DAppAlertDialog(
    title: String,
    body: String,
    dismissErrorDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        title = { Text(text = title, color = Color.Black) },
        text = { Text(text = body, color = Color.Black) },
        confirmButton = {
            TextButton(
                onClick = dismissErrorDialog
            ) {
                Text("Ok", color = Color.Black)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ChooseAccountContentPreview() {
    ChooseAccountContent(
        onBackClick = {},
        onContinueClick = {},
        imageUrl = "",
        continueButtonEnabled = true,
        accounts = listOf(
            SelectedAccountUiState(
                accountName = "Account name 1",
                accountAddress = "fdj209d9320",
                accountValue = "1000",
                accountCurrency = "$",
                appearanceID = 1,
                selected = true
            ),
            SelectedAccountUiState(
                accountName = "Account name 2",
                accountAddress = "342f23f2",
                accountValue = "2000",
                accountCurrency = "$",
                appearanceID = 1,
                selected = false
            )
        ),
        onAccountSelect = {},
    )
}

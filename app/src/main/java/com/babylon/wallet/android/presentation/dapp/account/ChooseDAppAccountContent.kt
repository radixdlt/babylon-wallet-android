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
import androidx.compose.material.icons.filled.ArrowBack
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
import com.babylon.wallet.android.data.dapp.DAppAccountData
import com.babylon.wallet.android.presentation.ui.theme.RadixBackground
import com.babylon.wallet.android.presentation.ui.theme.RadixButtonBackground
import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2
import com.babylon.wallet.android.presentation.ui.theme.White

@Composable
fun ChooseDAppAccountContent(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String,
    dAppAccounts: List<DAppAccountData>,
    dAppSelectedIndexes: Map<Int, Boolean>,
    onDAppAccountSelected: (selected: Boolean, index: Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "navigate back"
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 50.dp, vertical = 16.dp),
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

                dAppAccounts.forEachIndexed { index, dAppAccount ->
                    DAppAccountCard(
                        accountName = dAppAccount.accountName,
                        hashValue = dAppAccount.accountHash,
                        accountCurrency = dAppAccount.accountCurrency,
                        accountValue = dAppAccount.accountValue,
                        checked = dAppSelectedIndexes[index] == true,
                        onCheckedChange = { selected ->
                            onDAppAccountSelected(selected, index)
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
                enabled = dAppSelectedIndexes.containsValue(true),
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

@Preview(showBackground = true)
@Composable
fun ChooseDAppAccountContentPreview() {
    ChooseDAppAccountContent(
        onBackClick = {},
        onContinueClick = {},
        imageUrl = "",
        dAppAccounts = listOf(
            DAppAccountData(
                accountName = "Account name1",
                accountValue = "1000",
                accountCurrency = "$",
                accountHash = "43432423rf43g32g34"
            ),
            DAppAccountData(
                accountName = "Account name2",
                accountValue = "2000",
                accountCurrency = "$",
                accountHash = "d12j392dk02g43g43"
            ),
            DAppAccountData(
                accountName = "Account name3",
                accountValue = "3000",
                accountCurrency = "$",
                accountHash = "dj39f322dk02g43g43"
            ),
            DAppAccountData(
                accountName = "Account name4",
                accountValue = "4000",
                accountCurrency = "$",
                accountHash = "dj392dkg4302g2g42"
            )
        ),
        dAppSelectedIndexes = mapOf(
            0 to false,
            1 to false,
            2 to false,
            3 to false
        ),
        onDAppAccountSelected = { _, _ -> }
    )
}

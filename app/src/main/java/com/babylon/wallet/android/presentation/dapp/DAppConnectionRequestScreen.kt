package com.babylon.wallet.android.presentation.dapp

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
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.babylon.wallet.android.presentation.ui.theme.RadixBackground
import com.babylon.wallet.android.presentation.ui.theme.RadixButtonBackground
import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2
import com.babylon.wallet.android.presentation.ui.theme.White

@Composable
fun DAppConnectionRequestScreen(
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit,
    imageUrl: String,
    labels: List<String>,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        IconButton(onClick = onCloseClick) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "navigate back"
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 50.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.dapp_connection_request),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(40.dp))
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl,
                    placeholder = painterResource(id = R.drawable.img_placeholder),
                    error = painterResource(id = R.drawable.img_placeholder)
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(id = R.string.radaswap_wants_to_connect_wallet),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                color = RadixGrey2,
                text = stringResource(id = R.string.for_this_dapp_to_function),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(34.dp))
            labels.forEach { labelTitleResource ->
                Text(
                    text = labelTitleResource,
                    textAlign = TextAlign.Start,
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
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun DAppConnectionRequestScreenPreview() {
    DAppConnectionRequestScreen(
        onCloseClick = {},
        onContinueClick = { },
        imageUrl = "",
        labels = listOf(
            stringResource(id = R.string.dapp_condition1),
            stringResource(id = R.string.dapp_condition2)
        )
    )
}

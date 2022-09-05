package com.babylon.wallet.android.presentation.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.theme.RadixBackground
import com.babylon.wallet.android.presentation.ui.theme.RadixButtonBackground
import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2
import com.babylon.wallet.android.presentation.ui.theme.White

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreateAccountScreen(
    onBackClick: () -> Unit
) {
    var buttonEnabled by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    val maxLength = 20

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "navigate back"
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 16.dp)
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.img_account_creation),
                    contentDescription = "account_creation_image"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.create_new_account),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = stringResource(id = R.string.account_creation_text),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(30.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = text,
                        onValueChange = {
                            buttonEnabled = it.isNotEmpty()
                            text = it.take(maxLength)
                        },
                        modifier = Modifier
                            .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(4.dp))
                            .fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.account_name)) },
                        shape = RoundedCornerShape(4.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            backgroundColor = Color.LightGray,
                        )
                    )
                    Text(
                        text = stringResource(id = R.string.this_can_be_changed_any_time),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = RadixGrey2
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { },
                    enabled = buttonEnabled,
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
}
